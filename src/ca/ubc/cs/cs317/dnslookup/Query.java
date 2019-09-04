package ca.ubc.cs.cs317.dnslookup;

import java.util.ArrayList;
import java.util.Random;

public class Query {
    //assume query is never longer than 512 bytes, when sending must send exact number of bytes in query
    private byte[] id; // must be between 0 and 65535 0x00FFFF  - use random to generate ID - this is first 16 bits
    private byte[] secondRow = new byte[2];
    private byte[] qdCount = new byte[2];
    private byte[] anCount = new byte[2];
    private byte[] nsCount = new byte[2];
    private byte[] arCount = new byte[2];
    private Question question;
    private byte[] request;
    private int concatenatedID;
    private String url;
    private RecordType rType;

    public Query(String url, RecordType ipType) {
        this.id = new byte[2];
        this.url = url;
        this.rType = ipType;
        Random randNumGen = new Random();
        //this is exclusive bound
        Integer idAsInt = randNumGen.nextInt(65536);

        this.id[1] = (byte) (idAsInt & 0xFF);           //last 8 (bits 8-15)
        this.id[0] = (byte) ((idAsInt >> 8) & 0xFF);    //first 8 bits

        this.concatenatedID = ((this.id[0] & 0x00FF ) << 8) | (this.id[1] & 0x00ff);

        // first bit should be 0 (sending query) or 1 (response) , remaining Opcode, AA, TC, RD, RA, Z, and RCODE should be 0
        this.secondRow[0] = (byte) 0x00;
        this.secondRow[1] = (byte) 0x00;

        this.qdCount[0] = (byte) 0x00;
        this.qdCount[1] = (byte) 0x01;  // set to 1 to specify number of entries in the question section
        this.anCount[0] = (byte) 0x00;
        this.anCount[1] = (byte) 0x00;
        this.nsCount[0] = (byte) 0x00;
        this.nsCount[1] = (byte) 0x00;
        this.arCount[0] = (byte) 0x00;
        this.arCount[1] = (byte) 0x00;

        this.question = new Question(url, ipType);

        this.combineBytes();

    }

    public void printQuery(){
        System.out.printf("\n\nQuery ID     %d\t ", this.concatenatedID);
        System.out.print(this.url + "  " + this.rType + " --> ");
    }

    public byte[] Query(){
        return request;
    }

    private byte[] combineBytes(){
        ArrayList<Byte> allBytes = new ArrayList<>();
        for (byte b: this.id){
            allBytes.add(b);
        }

        for (byte b: this.secondRow){
            allBytes.add(b);
        }

        for (byte b: this.qdCount){
            allBytes.add(b);
        }

        for (byte b: this.anCount){
            allBytes.add(b);
        }

        for (byte b: this.nsCount){
            allBytes.add(b);
        }

        for (byte b: this.arCount){
            allBytes.add(b);
        }

        for (byte b: this.question.getqName()){
            allBytes.add(b);
        }

        for (byte b: this.question.getqType()){
            allBytes.add(b);
        }


        for (byte b: this.question.getqClass()){
            allBytes.add(b);
        }

        request = new byte[allBytes.size()];
        for (int j = 0; j < allBytes.size(); j++) {
            request[j] = allBytes.get(j);
        }

        return request;

    }


}
