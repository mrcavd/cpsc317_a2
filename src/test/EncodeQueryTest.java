package test;

import ca.ubc.cs.cs317.dnslookup.*;
import org.junit.jupiter.api.Test;

import java.net.UnknownHostException;

public class EncodeQueryTest {

    @Test
    void test() throws UnknownHostException {
        Query test = new Query("www.ugrad.cs.ubc.ca", RecordType.A);

        byte[] mockResponse = new byte[1024];
        mockResponse[0] = (byte) 0x5f;
        mockResponse[1] = (byte) 0x9f;
        mockResponse[2] = (byte) 0x84;
        mockResponse[3] = (byte) 0x00;
        mockResponse[4] = (byte) 0x00;
        mockResponse[5] = (byte) 0x01;
        mockResponse[6] = (byte) 0x00;
        mockResponse[7] = (byte) 0x01;
        mockResponse[8] = (byte) 0x00;
        mockResponse[9] = (byte) 0x02;
        mockResponse[10] = (byte) 0x00;
        mockResponse[11] = (byte) 0x02;

        // START OF QUESTION
        mockResponse[12] = (byte) 0x03;
        mockResponse[13] = (byte) 0x77;
        mockResponse[14] = (byte) 0x77;
        mockResponse[15] = (byte) 0x77;
        mockResponse[16] = (byte) 0x05;
        mockResponse[17] = (byte) 0x75;
        mockResponse[18] = (byte) 0x67;
        mockResponse[19] = (byte) 0x72;
        mockResponse[20] = (byte) 0x61;
        mockResponse[21] = (byte) 0x64;
        mockResponse[22] = (byte) 0x02;
        mockResponse[23] = (byte) 0x63;
        mockResponse[24] = (byte) 0x73;
        mockResponse[25] = (byte) 0x03;
        mockResponse[26] = (byte) 0x75;
        mockResponse[27] = (byte) 0x62;
        mockResponse[28] = (byte) 0x63;
        mockResponse[29] = (byte) 0x02;
        mockResponse[30] = (byte) 0x63;
        mockResponse[31] = (byte) 0x61;
        mockResponse[32] = (byte) 0x00;
        mockResponse[33] = (byte) 0x00;
        mockResponse[34] = (byte) 0x01;
        mockResponse[35] = (byte) 0x00;
        mockResponse[36] = (byte) 0x01;
    // start of answer
        mockResponse[37] = (byte) 0xc0;
        mockResponse[38] = (byte) 0x0c;
        mockResponse[39] = (byte) 0x00;
        mockResponse[40] = (byte) 0x01;
        mockResponse[41] = (byte) 0x00;
        mockResponse[42] = (byte) 0x01;
        mockResponse[43] = (byte) 0x00;
        mockResponse[44] = (byte) 0x00;
        mockResponse[45] = (byte) 0x0e;
        mockResponse[46] = (byte) 0x10;
        mockResponse[47] = (byte) 0x00;
        mockResponse[48] = (byte) 0x04;
        mockResponse[49] = (byte) 0x8e;
        mockResponse[50] = (byte) 0x67;
        mockResponse[51] = (byte) 0x06;
        mockResponse[52] = (byte) 0x2b;
    // start of auth[0]
        mockResponse[53] = (byte) 0xc0;
        mockResponse[54] = (byte) 0x10;
        mockResponse[55] = (byte) 0x00;
        mockResponse[56] = (byte) 0x02;
        mockResponse[57] = (byte) 0x00;
        mockResponse[58] = (byte) 0x01;
        mockResponse[59] = (byte) 0x00;
        mockResponse[60] = (byte) 0x00;
        mockResponse[61] = (byte) 0x0e;
        mockResponse[62] = (byte) 0x10;
        mockResponse[63] = (byte) 0x00;
        mockResponse[64] = (byte) 0x06;
        mockResponse[65] = (byte) 0x03;
        mockResponse[66] = (byte) 0x66;
        mockResponse[67] = (byte) 0x73;
        mockResponse[68] = (byte) 0x31;
        mockResponse[69] = (byte) 0xc0;
        mockResponse[70] = (byte) 0x10;
        //auth[1]
        mockResponse[71] = (byte) 0xc0;
        mockResponse[72] = (byte) 0x10;
        mockResponse[73] = (byte) 0x00;
        mockResponse[74] = (byte) 0x02;
        mockResponse[75] = (byte) 0x00;
        mockResponse[76] = (byte) 0x01;
        mockResponse[77] = (byte) 0x00;
        mockResponse[78] = (byte) 0x00;
        mockResponse[79] = (byte) 0x0e;
        mockResponse[80] = (byte) 0x10;
        mockResponse[81] = (byte) 0x00;
        mockResponse[82] = (byte) 0x06;
        mockResponse[83] = (byte) 0x03;
        mockResponse[84] = (byte) 0x6e;
        mockResponse[85] = (byte) 0x73;
        mockResponse[86] = (byte) 0x31;
        mockResponse[87] = (byte) 0xc0;
        mockResponse[88] = (byte) 0x16;
    // add[0]
        mockResponse[89] = (byte) 0xc0;
        mockResponse[90] = (byte) 0x41;
        mockResponse[91] = (byte) 0x00;
        mockResponse[92] = (byte) 0x01;
        mockResponse[93] = (byte) 0x00;
        mockResponse[94] = (byte) 0x01;
        mockResponse[95] = (byte) 0x00;
        mockResponse[96] = (byte) 0x00;
        mockResponse[97] = (byte) 0x0e;
        mockResponse[98] = (byte) 0x10;
        mockResponse[99] = (byte) 0x00;
        mockResponse[100] = (byte) 0x04;
        mockResponse[101] = (byte) 0xC6;
        mockResponse[102] = (byte) 0xA2;
        mockResponse[103] = (byte) 0x23;
        mockResponse[104] = (byte) 0x01;
        //add[2]
        mockResponse[105] = (byte) 0xc0;
        mockResponse[106] = (byte) 0x53;
        mockResponse[107] = (byte) 0x00;
        mockResponse[108] = (byte) 0x01;
        mockResponse[109] = (byte) 0x00;
        mockResponse[110] = (byte) 0x01;
        mockResponse[111] = (byte) 0x00;
        mockResponse[112] = (byte) 0x00;
        mockResponse[113] = (byte) 0x0e;
        mockResponse[114] = (byte) 0x10;
        mockResponse[115] = (byte) 0x00;
        mockResponse[116] = (byte) 0x04;
        mockResponse[117] = (byte) 0x8e;
        mockResponse[118] = (byte) 0x67;
        mockResponse[119] = (byte) 0x06;
        mockResponse[120] = (byte) 0x06;

        try {
            Response testAns = new Response(mockResponse, true);
        } catch (RCodeError rCodeError) {
            rCodeError.printStackTrace();
        } catch (TruncatedError truncatedError) {
            truncatedError.printStackTrace();
        }
    }
}
