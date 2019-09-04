package ca.ubc.cs.cs317.dnslookup;

import java.util.ArrayList;

public class Question {
    private ArrayList<Byte> qName;
    private byte[] qType;
    private byte[] qClass;


    public Question(String url, RecordType ipType){
        this.qName = new ArrayList<>();
        this.qType = new byte[2];
        this.qClass = new byte[2];

        String[] urlParts = url.split("\\.");

        for (int i=0; i < urlParts.length; i++) {

            int pieceLength = urlParts[i].length();
            byte pieceLengthAsByte = (byte) pieceLength;
            this.qName.add(pieceLengthAsByte);

            char[] chars = urlParts[i].toCharArray();
            for (char c : chars){
                int asciiInt = (int) c;
                byte asciiAsByte = (byte) asciiInt;
                this.qName.add(asciiAsByte);
            }
        }

        byte zeroByte = 0x00;
        this.qName.add(zeroByte);
        this.qType[0] =0x00;
        if (ipType.toString().equals("A")){
            this.qType[1] =(byte) 0x01;
        } else if (ipType.toString().equals("AAAA")){
            this.qType[1] =(byte) 0x1C; //AAAA
        } else if (ipType.toString().equals("NS")){
            this.qType[1] =(byte) 0x02; //NS
        } else if (ipType.toString().equals("CNAME")) {
            this.qType[1] =(byte) 0x05; //CNAME
        } else if (ipType.toString().equals("MX")){
            this.qType[1] =(byte) 0x0F; //MX
        }

        this.qClass[0] = (byte) 0x00;
        this.qClass[1] = (byte) 0x01; //using internet : which has a value of 1

    }

    public ArrayList<Byte> getqName() {
        return qName;
    }

    public byte[] getqType() {
        return qType;
    }

    public byte[] getqClass() {
        return qClass;
    }
}
