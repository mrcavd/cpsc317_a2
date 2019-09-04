package ca.ubc.cs.cs317.dnslookup;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Response {
    byte[] listOfBytes;
    ArrayList<ResourceRecord> answers = new ArrayList<>();
    ArrayList<ResourceRecord> nameServers = new ArrayList<>();
    ArrayList<ResourceRecord> additional = new ArrayList<>();
    DNSCache cache = DNSCache.getInstance();


    public Response(byte[] listOfBytes, boolean tracingFlag) throws UnknownHostException, RCodeError, TruncatedError { //throws Exception {
        this.listOfBytes = listOfBytes;
        int id =  ((listOfBytes[0] & 0x00FF ) << 8) | (listOfBytes[1] & 0x00ff);
        if (tracingFlag){
            System.out.printf("Response ID: %d ", id);
        }

        //AA flag
        if ((listOfBytes[2] & 0x04) == 0x04) {
            if(tracingFlag){
                System.out.println("Authoritative = true");
            }
        }
        else {
            if(tracingFlag){
                System.out.println("Authoritative = false"); }
            }

        //QR FLAG
        if ((listOfBytes[2] & 0x80) == 0x80) {
        }

        if ((listOfBytes[2] & 0x01) == 0x01) {
            throw new TruncatedError("Received message was truncated");
        }

        //RCODE
        switch (listOfBytes[3] & 0x0F) {
            case 0x00:
                //"No error"
                break;
            case 0x01:
//                "Format error"
                throw new RCodeError();
            case 0x02:
//               "Server failure"
                throw new RCodeError();
            case 0x03:
//                "Name error"
                throw new RCodeError();
            case 0x04:
//               "Not implemented"
                throw new RCodeError();
            case 0x05:
//               "Refused"
                throw new RCodeError();
            default:
                break;
        }

        int qdCount = ((listOfBytes[4] & 0x00FF ) << 8) | (listOfBytes[5] & 0x00ff);
        int anCount = ((listOfBytes[6] & 0x00FF ) << 8) | (listOfBytes[7] & 0x00ff);
        int nsCount = ((listOfBytes[8] & 0x00FF ) << 8) | (listOfBytes[9] & 0x00ff);
        int arCount = ((listOfBytes[10] & 0x00FF ) << 8) | (listOfBytes[11] & 0x00ff);

        // end of header now need to parse question

        int nameLabelLength =listOfBytes[12];
        int byteIndex = 13;
        int lastByteIndex = byteIndex + nameLabelLength;

        String parsedURL = "";
        while (nameLabelLength > 0 ){
            for (; byteIndex < lastByteIndex; byteIndex++){
                char currentChar = (char) this.listOfBytes[byteIndex];
                parsedURL = parsedURL+currentChar;
            }
            nameLabelLength = this.listOfBytes[byteIndex];
            if (nameLabelLength != 0 ){
                parsedURL = parsedURL+".";
            }
            byteIndex = byteIndex + 1;
            lastByteIndex = lastByteIndex + nameLabelLength +1;
        }

        // parsed question name

        int qType = ((listOfBytes[byteIndex] & 0x00FF ) << 8) | (listOfBytes[byteIndex+1] & 0x00ff);
        int qClass = ((listOfBytes[byteIndex+2] & 0x00FF ) << 8) | (listOfBytes[byteIndex+3] & 0x00ff);

        // start Response Parsing
        int curIndexAnswer = byteIndex + 4;
        int anCountIterator = anCount;

        // parse the answers

        while (anCountIterator > 0) {

            String urlName = decodeName(curIndexAnswer, listOfBytes[curIndexAnswer]);

            if ((this.listOfBytes[curIndexAnswer] >> 6  & 0x03) != 0x03) {
//                // update index of where we are
                while (listOfBytes[curIndexAnswer] != 0) {
                    curIndexAnswer++;
                }
                curIndexAnswer = curIndexAnswer++; //once more to get the next byte to parse
            } else {
                curIndexAnswer = curIndexAnswer + 2;
            }


            int ansType = ((listOfBytes[curIndexAnswer] & 0x00FF ) << 8) | (listOfBytes[curIndexAnswer+1] & 0x00ff);
            int ansClass = ((listOfBytes[curIndexAnswer + 2] & 0x00FF ) << 8) | (listOfBytes[curIndexAnswer+3] & 0x00ff);
            long ttl = ((listOfBytes[curIndexAnswer+4] & 0x00FF ) << 24) | ((listOfBytes[curIndexAnswer+5] & 0x00ff) << 16) | ((listOfBytes[curIndexAnswer+6] & 0x00ff) << 8) | ((listOfBytes[curIndexAnswer + 7] & 0x00ff));
            int rdLength = ((listOfBytes[curIndexAnswer + 8] & 0x00FF ) << 8) | (listOfBytes[curIndexAnswer+9] & 0x00ff);

            curIndexAnswer = curIndexAnswer + 9; //update index to end of length
            String parsedIP = "";
            ResourceRecord RR = null;

            // get IPv4 in true otherwise get IPv6 address
            if (ansType == 1){
                for (int i =1; i <= rdLength; i++) {
                    if (i != 1){
                        parsedIP = parsedIP + ".";
                    }
                    Integer curIPpart = this.listOfBytes[curIndexAnswer+i] & 0xFF;
                    parsedIP = parsedIP + curIPpart;
                }
                RR = new ResourceRecord(urlName,  RecordType.getByCode(ansType), ttl, InetAddress.getByName(parsedIP));
            } else if (ansType == 28){
                for (int i =1; i <= rdLength; i++) {
                    if (i != 1){
                        parsedIP = parsedIP + ":";
                    }
                    Integer curIPpart = ((listOfBytes[curIndexAnswer+i] & 0x00FF ) << 8) | (listOfBytes[curIndexAnswer + i + 1] & 0x00ff);
                    parsedIP = parsedIP + Integer.toHexString(curIPpart);
                    i++;
                }
                RR = new ResourceRecord(urlName,  RecordType.getByCode(ansType), ttl, InetAddress.getByName(parsedIP));
            } else if (ansType == 5) {
                String cName = decodeName(curIndexAnswer + 1, listOfBytes[curIndexAnswer + 1]);
                RR = new ResourceRecord(urlName,  RecordType.getByCode(ansType), ttl, cName);
            }


            cache.addResult(RR);
            answers.add(RR);
            curIndexAnswer = curIndexAnswer + rdLength + 1;

            anCountIterator--;
        }

        int authorityCount = nsCount;
        //Nameservers

        while (authorityCount > 0) {
            String urlName = decodeName(curIndexAnswer, listOfBytes[curIndexAnswer]);

            if ((this.listOfBytes[curIndexAnswer] >> 6  & 0x03) != 0x03) {
//               update index of where we are
                while (listOfBytes[curIndexAnswer] != 0) {
                    curIndexAnswer++;
                }
                curIndexAnswer++;

            } else {
                curIndexAnswer = curIndexAnswer + 2;
            }


            int ansType = ((listOfBytes[curIndexAnswer] & 0x00FF ) << 8) | (listOfBytes[curIndexAnswer+1] & 0x00ff);
            int ansClass = ((listOfBytes[curIndexAnswer + 2] & 0x00FF ) << 8) | (listOfBytes[curIndexAnswer+3] & 0x00ff);
            long ttl = ((listOfBytes[curIndexAnswer+4] & 0x00FF ) << 24) | ((listOfBytes[curIndexAnswer+5] & 0x00ff) << 16) | ((listOfBytes[curIndexAnswer+6] & 0x00ff) << 8) | ((listOfBytes[curIndexAnswer + 7] & 0x00ff));
            int rdLength = ((listOfBytes[curIndexAnswer + 8] & 0x00FF ) << 8) | (listOfBytes[curIndexAnswer+9] & 0x00ff);

            curIndexAnswer = curIndexAnswer + 10; //update index to start of rData

            // check if it's a pointer
            String nsName = decodeName(curIndexAnswer, listOfBytes[curIndexAnswer]);
            //System.out.println("NS Name: " + nsName + "\n");

            // move pointer to next start of next section
            curIndexAnswer = curIndexAnswer + rdLength;

            ResourceRecord RR = new ResourceRecord(urlName,  RecordType.getByCode(ansType), ttl, nsName);
            cache.addResult(RR);
            nameServers.add(RR);
            authorityCount--;
        }



        int addRRCount = arCount;
        //tAdditional information
        while (addRRCount > 0) {
            String urlName = decodeName(curIndexAnswer, listOfBytes[curIndexAnswer]);

            if ((this.listOfBytes[curIndexAnswer] >> 6 & 0x03) != 0x03) {
//              update index of where we are
                while (listOfBytes[curIndexAnswer] != 0) {
                    curIndexAnswer++;
                }
                curIndexAnswer = curIndexAnswer++; //once more to get the next byte to parse
            } else {
                curIndexAnswer = curIndexAnswer + 2;
            }

            int ansType = ((listOfBytes[curIndexAnswer] & 0x00FF) << 8) | (listOfBytes[curIndexAnswer + 1] & 0x00ff);
            int ansClass = ((listOfBytes[curIndexAnswer + 2] & 0x00FF) << 8) | (listOfBytes[curIndexAnswer + 3] & 0x00ff);
            long ttl = ((listOfBytes[curIndexAnswer + 4] & 0x00FF) << 24) | ((listOfBytes[curIndexAnswer + 5] & 0x00ff) << 16) | ((listOfBytes[curIndexAnswer + 6] & 0x00ff) << 8) | ((listOfBytes[curIndexAnswer + 7] & 0x00ff));
            int rdLength = ((listOfBytes[curIndexAnswer + 8] & 0x00FF) << 8) | (listOfBytes[curIndexAnswer + 9] & 0x00ff);

            curIndexAnswer = curIndexAnswer + 9; //update index to end of length
            String parsedIP = "";

            // 1 is IPv4 case else IpV6
            if (ansType == 1) {
                for (int i = 1; i <= rdLength; i++) {
                    if (i != 1) {
                        parsedIP = parsedIP + ".";
                    }
                    Integer curIPpart = this.listOfBytes[curIndexAnswer + i] & 0xFF;
                    parsedIP = parsedIP + curIPpart;
                }
            } else {
                for (int i = 1; i <= rdLength; i++) {
                    if (i != 1) {
                        parsedIP = parsedIP + ":";
                    }
                    Integer curIPpart = ((listOfBytes[curIndexAnswer + i] & 0x00FF) << 8) | (listOfBytes[curIndexAnswer + i + 1] & 0x00ff);
                    parsedIP = parsedIP + Integer.toHexString(curIPpart);
                    i++;
                }
            }

            ResourceRecord RR = new ResourceRecord(urlName, RecordType.getByCode(ansType), ttl, InetAddress.getByName(parsedIP));
            cache.addResult(RR);

            curIndexAnswer = curIndexAnswer + rdLength + 1;
            additional.add(RR);
            addRRCount--;
        }
    }

    public ArrayList<ResourceRecord> getAnswer(){
        return answers;
    }

    public ArrayList<ResourceRecord> getNameServers(){
        return nameServers;
    }

    public ArrayList<ResourceRecord> getAdditional(){
        return additional;
    }

    private String decodeName(int byteIndex, int length) {
        // true case is when it's a pointer to a pointer
        if (((listOfBytes[byteIndex] >> 6)& 0x03) == 0x03){
            byte firstByte = listOfBytes[byteIndex];
            byte secondByte = listOfBytes[byteIndex+1];
            int last6bitsOfFirstByte = (((firstByte & 0x3F) & 0xFF) << 8);
            int secondBits = (secondByte & 0xFF);
            int offset = last6bitsOfFirstByte + secondBits; // + 1 as we need the bits starting fr
            return decodeName(offset, listOfBytes[offset]);
        } else {
            byteIndex = byteIndex + 1;
            int lastByteIndex = byteIndex + length;
            String parsedURL = "";
            while (length > 0) {
                for (; byteIndex < lastByteIndex; byteIndex++) {
                    char currentChar = (char) this.listOfBytes[byteIndex];
                    parsedURL = parsedURL + currentChar;
                }
                length = this.listOfBytes[byteIndex];
                if (length != 0 && (((listOfBytes[byteIndex] >> 6)& 0x03) != 0x03)) {
                    parsedURL = parsedURL + ".";
                } else if ((this.listOfBytes[byteIndex] >> 6  & 0x03) == 0x03) {
                    parsedURL = parsedURL + "." + decodeName(byteIndex, listOfBytes[byteIndex]);
                }
                byteIndex = byteIndex + 1;
                lastByteIndex = lastByteIndex + length + 1;
            }
            return parsedURL;
        }
    }

}
