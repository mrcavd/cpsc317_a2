package ca.ubc.cs.cs317.dnslookup;

import java.io.Console;
import java.io.IOException;
import java.net.*;
import java.util.*;

public class DNSLookupService {

    private static final int DEFAULT_DNS_PORT = 53;
    private static final int MAX_INDIRECTION_LEVEL = 20;

    private static InetAddress rootServer;
    private static InetAddress initialServer;
    private static boolean verboseTracing = false;
    private static DatagramSocket socket;

    private static DNSCache cache = DNSCache.getInstance();
    private static DNSNode rootNode = null;
    private static DNSNode cnameNode = null;
    private static ArrayList<ResourceRecord> addr = new ArrayList<>();
    private static ArrayList<ResourceRecord> nameServers = new ArrayList<>();
    private static ArrayList<ResourceRecord> answers = new ArrayList<>();
    private static boolean updated = false;

    private static Random random = new Random();

    /**
     * Main function, called when program is first invoked.
     *
     * @param args list of arguments specified in the command line.
     */
    public static void main(String[] args) {

        if (args.length != 1) {
            System.err.println("Invalid call. Usage:");
            System.err.println("\tjava -jar DNSLookupService.jar rootServer");
            System.err.println("where rootServer is the IP address (in dotted form) of the root DNS server to start the search at.");
            System.exit(1);
        }

        try {
            rootServer = InetAddress.getByName(args[0]);
            System.out.println("Root DNS server is: " + rootServer.getHostAddress());
        } catch (UnknownHostException e) {
            System.err.println("Invalid root server (" + e.getMessage() + ").");
            System.exit(1);
        }

        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(5000);
        } catch (SocketException ex) {
            ex.printStackTrace();
            System.exit(1);
        }

        Scanner in = new Scanner(System.in);
        Console console = System.console();
        do {
            // Use console if one is available, or standard input if not.
            String commandLine;
            if (console != null) {
                System.out.print("DNSLOOKUP> ");
                commandLine = console.readLine();
            } else
                try {
                    commandLine = in.nextLine();
                } catch (NoSuchElementException ex) {
                    break;
                }
            // If reached end-of-file, leave
            if (commandLine == null) break;

            // Ignore leading/trailing spaces and anything beyond a comment character
            commandLine = commandLine.trim().split("#", 2)[0];

            // If no command shown, skip to next command
            if (commandLine.trim().isEmpty()) continue;

            String[] commandArgs = commandLine.split(" ");

            if (commandArgs[0].equalsIgnoreCase("quit") ||
                    commandArgs[0].equalsIgnoreCase("exit"))
                break;
            else if (commandArgs[0].equalsIgnoreCase("server")) {
                // SERVER: Change root nameserver
                if (commandArgs.length == 2) {
                    try {
                        rootServer = InetAddress.getByName(commandArgs[1]);
                        System.out.println("Root DNS server is now: " + rootServer.getHostAddress());
                    } catch (UnknownHostException e) {
                        System.out.println("Invalid root server (" + e.getMessage() + ").");
                        continue;
                    }
                } else {
                    System.out.println("Invalid call. Format:\n\tserver IP");
                    continue;
                }
            } else if (commandArgs[0].equalsIgnoreCase("trace")) {
                // TRACE: Turn trace setting on or off
                if (commandArgs.length == 2) {
                    if (commandArgs[1].equalsIgnoreCase("on"))
                        verboseTracing = true;
                    else if (commandArgs[1].equalsIgnoreCase("off"))
                        verboseTracing = false;
                    else {
                        System.err.println("Invalid call. Format:\n\ttrace on|off");
                        continue;
                    }
                    System.out.println("Verbose tracing is now: " + (verboseTracing ? "ON" : "OFF"));
                } else {
                    System.err.println("Invalid call. Format:\n\ttrace on|off");
                    continue;
                }
            } else if (commandArgs[0].equalsIgnoreCase("lookup") ||
                    commandArgs[0].equalsIgnoreCase("l")) {
                // LOOKUP: Find and print all results associated to a name.
                RecordType type;
                if (commandArgs.length == 2)
                    type = RecordType.A;
                else if (commandArgs.length == 3)
                    try {
                        type = RecordType.valueOf(commandArgs[2].toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        System.err.println("Invalid query type. Must be one of:\n\tA, AAAA, NS, MX, CNAME");
                        continue;
                    }
                else {
                    System.err.println("Invalid call. Format:\n\tlookup hostName [type]");
                    continue;
                }
                findAndPrintResults(commandArgs[1], type);
            } else if (commandArgs[0].equalsIgnoreCase("dump")) {
                // DUMP: Print all results still cached
                cache.forEachNode(DNSLookupService::printResults);
            } else {
                System.err.println("Invalid command. Valid commands are:");
                System.err.println("\tlookup fqdn [type]");
                System.err.println("\ttrace on|off");
                System.err.println("\tserver IP");
                System.err.println("\tdump");
                System.err.println("\tquit");
                continue;
            }

        } while (true);

        socket.close();
        System.out.println("Goodbye!");
    }

    /**
     * Finds all results for a host name and type and prints them on the standard output.
     *
     * @param hostName Fully qualified domain name of the host being searched.
     * @param type     Record type for search.
     */
    private static void findAndPrintResults(String hostName, RecordType type) {

        DNSNode node = new DNSNode(hostName, type);
        initialServer = rootServer;
        try {
            printResults(node, getResults(node, 0));
        } catch (RCodeError rCodeError) {
            System.out.printf("%-30s %-5s %-8d %s\n", node.getHostName(),
                    node.getType(), -1, "0.0.0.0");
        } catch (TruncatedError truncatedError) {
            System.out.printf(truncatedError.getMessage());
        } catch (SOAError e){
            System.out.printf("%-30s %-5s %-8d %s\n", node.getHostName(),
                    node.getType(), -1, "0.0.0.0");
        } catch (RetryError retryError) {
            System.out.printf("%-30s %-5s %-8d %s\n", node.getHostName(),
                    node.getType(), -1, "0.0.0.0");
        } catch (UnknownHostException e) {
            System.out.println("Unknown Host");
        }
    }

    /**
     * Finds all the result for a specific node.
     *
     * @param node             Host and record type to be used for search.
     * @param indirectionLevel Control to limit the number of recursive calls due to CNAME redirection.
     *                         The initial call should be made with 0 (zero), while recursive calls for
     *                         regarding CNAME results should increment this value by 1. Once this value
     *                         reaches MAX_INDIRECTION_LEVEL, the function prints an error message and
     *                         returns an empty set.
     * @return A set of resource records corresponding to the specific query requested.
     */
    private static Set<ResourceRecord> getResults(DNSNode node, int indirectionLevel) throws RCodeError, TruncatedError, SOAError, RetryError, UnknownHostException {
        if (indirectionLevel > MAX_INDIRECTION_LEVEL) {
            System.err.println("Maximum number of indirection levels reached.");
            rootServer = initialServer;
            return Collections.emptySet();
        }
        else {
            // check if we can get the node from cache
            // if not proceed to query for answer
            Set<ResourceRecord> RR = cache.getCachedResults(node);
            Set<ResourceRecord> cnameRR = cache.getCachedResults(new DNSNode(node.getHostName(), RecordType.CNAME));
            Set<ResourceRecord> rootRecord = null;

            // block of code that failed to update TTL when CNAME is involved
//            if(indirectionLevel == 0) { updated = false; }
//
//            if(!cnameRR.isEmpty() && !RR.isEmpty() && !updated){
//                System.out.println(cnameRR.iterator().next().getHostName());
//                System.out.println(cnameRR.iterator().next().getTextResult());
//                System.out.println(RR.iterator().next().getInetResult());
//                retrieveResultsFromServer(node, RR.iterator().next().getInetResult());
//                updated = true;
//                return cache.getCachedResults(node);
//            }

            if(rootNode != null) {
                rootServer = initialServer;
                rootRecord = cache.getCachedResults(rootNode);
            }

            if(!RR.isEmpty() && !rootRecord.isEmpty()) {
//                System.out.println("Resetting default NS");
                initialServer = InetAddress.getByName("199.7.83.42");
                rootServer = initialServer;
            }
            if(RR.isEmpty() || rootRecord.isEmpty()) {
                // level == 0, meaning query to rootServer
                if(indirectionLevel == 0) {
//                    System.out.println("----- INDIRECTION 0 ---------");
                    rootNode = node;
                    // the following block ensures that if cache has cname of
                    // query node, it will lookup the furtherest cname pointer
                    // within the cache and send request of cname instead
                    // of the original node ( which gets to cname eventually)
                    if(!cnameRR.isEmpty()){
                        String cname = null;
//                        System.out.println("------ CACHE GRAB SUCCESS ------");
                        while(cnameRR.iterator().hasNext()) {
                            cname = cnameRR.iterator().next().getTextResult();
                            cnameRR = cache.getCachedResults(new DNSNode(cname, RecordType.CNAME));
                        }
                        cnameNode = new DNSNode(cname, rootNode.getType());
                        node = cnameNode;
                    }
                    retrieveResultsFromServer(node, rootServer);
                    indirectionLevel++;
                    getResults(node, indirectionLevel);
                }
                else {
                    DNSNode ns = node;

                    // supposedly it should go through every RR we cached
                    // and i have made a helper to extract InetAddr from RR
                    // So we can send a request to a different NS


                    // handle CNAME case
                    // if answers return a CNAME would get a code 5
                    // create node of CNAME type.A to query rootServer again
                    if(!answers.isEmpty()){
//                        System.out.println("---------In ANS----------");
                        int answerMatched = 0;
                        for(int a1 = 0; a1 <answers.size(); a1++){
                            ResourceRecord ans = answers.get(a1);
//                            System.out.println("Answer Name: " + ans.getHostName());

                            // loop through the answer lists to ensure that
                            // no CNAME is pointing to another CNAME
                            if(ans.getType().getCode() == 5){
//                                System.out.println("----- CNAME appears -----");
                                for(int a = 0; a < answers.size(); a++){
                                    if(answers.get(a).getHostName().equalsIgnoreCase(ans.getTextResult())){

                                        // if we have 2 answers and a CName is pointing to another
                                        // answer of Type A, cache the CNAME host with Type A's InetAddress
                                        if(answers.get(a).getType() == rootNode.getType()){
//                                            System.out.println("MATCH");
                                            cache.addResult(
                                                    new ResourceRecord(
                                                            ans.getHostName(),
                                                            answers.get(a).getType(),
                                                            answers.get(a).getTTL(),
                                                            answers.get(a).getInetResult()));
                                            if(a1 == answers.size()-1) { answerMatched = 1; }
                                        }
                                        // else make sure the CName pointer points to the ultimate
                                        // CNAME of rootNode
                                        else{
                                            ans = answers.get(a);
                                            cnameNode = new DNSNode(ans.getTextResult(), RecordType.A);
//                                            System.out.println("CNAME node: " + cnameNode.getHostName());
                                        }
                                    }
                                }
                                if (rootNode.getType() == RecordType.A ){
                                    cnameNode = new DNSNode(ans.getTextResult(), RecordType.A);
                                    ns = cnameNode;
                                } else if (rootNode.getType() == RecordType.AAAA){
                                    cnameNode = new DNSNode(ans.getTextResult(), RecordType.AAAA);
                                    ns = cnameNode;
                                }

                                rootServer = initialServer;

                            }

                            // check if the answers are type A == IP address
                            // if yes it will either be the end of a NS IP or the answer to our root node
                            if((ans.getType().getCode() == 1 || ans.getType().getCode() == 28) && answerMatched != 1 && a1 <= answers.size()-1){
//                                System.out.println("--------- RECOGNIZE ANSWER ----------");
                                // Case: root Node is Type A in answer
                                if (cnameNode != null){
                                    if(ans.getHostName().equalsIgnoreCase(cnameNode.getHostName())){
                                        cache.addResult(
                                                new ResourceRecord(
                                                        rootNode.getHostName(),
                                                        ans.getType(),
                                                        ans.getTTL(),
                                                        ans.getInetResult()));
                                        if(a1 == answers.size()-1) {
                                            answerMatched = 1;
                                            rootServer = initialServer;
//                                            System.out.println("-----answered------" + rootServer.getHostAddress());
                                        }
                                    }
                                    else { rootServer = ans.getInetResult(); }
                                    ns = cnameNode;
                                }


                                // Case: end of NS server, now ser parameter in order
                                // to query answer for actual lookup url
                                else {
//                                    System.out.println("NS ip reached, " + ans.getInetResult());
                                    ns =rootNode;
                                    rootServer = ans.getInetResult();
                                }

                            }
                        }
                        if(answerMatched != 1) {
                            retrieveResultsFromServer(ns, rootServer);
                        } else {
                            cnameNode = null;
//                            System.out.println(rootServer.getHostAddress() +", " + initialServer.getHostAddress() );
                            rootServer = initialServer;

                            return cache.getCachedResults(node);
                        }
//                        System.out.println("---------Out ANS----------");

                    }
                    // Most common case
                    // Additional contains information and we read the first entry
                    // send request to the said InetAddress for further information
                    // on our node
                    else if(!addr.isEmpty()){
//                        System.out.println("---------In Additional----------");
                        ResourceRecord add = addr.get(0);
                        for (int i = 0; i < addr.size(); i++) {
                            if (addr.get(i).getType().getCode() == 1) {
                                add = addr.get(i);
                                break;
                            }
                        }
                        // so we never search on an IPv6 name server
                        if (add.getType().getCode() == 28){
                            return Collections.emptySet();
                        }

                        InetAddress a = add.getInetResult();
                        retrieveResultsFromServer(node, a);
//                        System.out.println("---------Out Additional----------");

                    }
                    else if(!nameServers.isEmpty()){
//                        System.out.println("---------In NS----------");
                        ResourceRecord name = nameServers.iterator().next();
                        if(name.getType().getCode() == 6){ throw new SOAError(); }
                        ns = new DNSNode(name.getTextResult(), RecordType.A);
                        retrieveResultsFromServer(ns, rootServer);
//                        System.out.println("---------Out NS----------");

                    }

                    getResults(ns, ++indirectionLevel);
                }
            }
        }
        return cache.getCachedResults(node);
    }


    /**
     * Retrieves DNS results from a specified DNS server. Queries are sent in iterative mode,
     * and the query is repeated with a new server if the provided one is non-authoritative.
     * Results are stored in the cache.
     *
     * @param node   Host name and record type to be used for the query.
     * @param server Address of the server to be used for the query.
     */
    private static void retrieveResultsFromServer(DNSNode node, InetAddress server) throws RCodeError, TruncatedError, RetryError {


        Query Q = new Query(node.getHostName(), node.getType());
        byte[] request = Q.Query();
        if (verboseTracing){
            Q.printQuery();
            System.out.println(server.getHostAddress());
        }


        DatagramPacket query = new DatagramPacket(request, request.length, server, DEFAULT_DNS_PORT);
        try {
            socket.send(query);

        }catch(IOException e){
            System.out.println("socket.send error: " + e.getMessage());
        }catch(Exception e){
            System.out.println("Error: " + e.getMessage());
        }

        byte[] buf = new byte[1024];
        DatagramPacket response = new DatagramPacket(buf, buf.length);

        try {
//            System.out.println("RECEIVING");
            try {
                socket.receive(response);
            } catch (SocketTimeoutException timeout) {
                if(verboseTracing){
                    Q.printQuery();
                    System.out.println(server.getHostAddress());
                }
                socket.receive(response);
            }

            Response r = new Response(buf, verboseTracing);

            answers = r.getAnswer();
//            if(!answers.isEmpty()){
//                System.out.println("-------------Answer from Response: " + answers.get(0).getHostName());
//            }
            nameServers = r.getNameServers();
            addr = r.getAdditional();

            if (verboseTracing){
                System.out.println("  Answers (" + answers.size() + ")");
            }
            if (!answers.isEmpty()) {
                for (ResourceRecord record : answers) {
                    verbosePrintResourceRecord(record, record.getType().getCode());
                }
            }
            if(verboseTracing){
                System.out.println("  Nameservers (" + nameServers.size() + ")");
            }
            if (!nameServers.isEmpty()) {
                for (ResourceRecord record : nameServers) {
                    verbosePrintResourceRecord(record, record.getType().getCode());
                }
            }
            if(verboseTracing){
                System.out.println("  Additional Information (" + addr.size() + ")");
            }
            if (!addr.isEmpty()) {
                for (ResourceRecord record : addr) {
                    verbosePrintResourceRecord(record, record.getType().getCode());
                }
            }
        } catch (SocketTimeoutException e){
            throw new RetryError();
        } catch (RCodeError e) {
            throw e;
        } catch (TruncatedError e) {
            throw e;
        } catch (IOException e){
            System.out.println("socket.receive error: " + e.getMessage());
        } catch(Exception e){
            System.out.println("socket.Error: " + e.getMessage());
        }
    }

    private static void verbosePrintResourceRecord(ResourceRecord record, int rtype) {
        if (verboseTracing)
            System.out.format("       %-30s %-10d %-4s %s\n", record.getHostName(),
                    record.getTTL(),
                    record.getType() == RecordType.OTHER ? rtype : record.getType(),
                    record.getTextResult());
    }

    /**
     * Prints the result of a DNS query.
     *
     * @param node    Host name and record type used for the query.
     * @param results Set of results to be printed for the node.
     */
    private static void printResults(DNSNode node, Set<ResourceRecord> results) {
        if (results.isEmpty())
            System.out.printf("%-30s %-5s %-8d %s\n", node.getHostName(),
                    node.getType(), -1, "0.0.0.0");
        for (ResourceRecord record : results) {
            System.out.printf("%-30s %-5s %-8d %s\n", node.getHostName(),
                    node.getType(), record.getTTL(), record.getTextResult());
        }
    }
}
