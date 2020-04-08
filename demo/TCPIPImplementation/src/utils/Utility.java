package utils;

public class Utility {
    public static long checksum(byte[] buf, int length) {
        int i = 0;
        long sum = 0;
        while (length > 0) {
            sum += (buf[i++]&0xff) << 8;
            if ((--length)==0) break;
            sum += (buf[i++]&0xff);
            --length;
        }

        return (~((sum & 0xFFFF)+(sum >> 16)))&0xFFFF;
    }
}
