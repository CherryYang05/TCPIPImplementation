/**
 * @Author Cherry
 * @Date 2020/5/15
 * @Time 21:26
 * @Brief
 */

public class Test {
    private static byte[] MAGIC_COOKIE = new byte[]{0x63, (byte) 0x82, 0x53, 0x63};
    static byte[] broadCastIP = new byte[]{(byte) 255, (byte) 255, (byte) 255, (byte) 255};

    public static void main(String[] args) {
        for (byte b : broadCastIP) {
            System.out.println(b);
        }
    }
}
