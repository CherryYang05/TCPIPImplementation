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
        //for (byte b : broadCastIP) {
        //    System.out.println(b);
        //}
        String[] name = "pan.baidu.com".split("\\.");
        for (String str : name) {
            //先填写字符个数
            System.out.println((byte) str.length());
            //填写字符
            for (int i = 0; i < str.length(); ++i) {
                byte b = (byte) str.charAt(i);
                b = (byte) 0x85;
                System.out.println(b);
            }
        }
    }
}
