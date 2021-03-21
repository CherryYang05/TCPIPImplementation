import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Properties;

/**
 * @Author Cherry
 * @Date 2020/5/15
 * @Time 21:26
 * @Brief
 */

public class TestDemo {
    private static byte[] MAGIC_COOKIE = new byte[]{0x63, (byte) 0x82, 0x53, 0x63};
    static byte[] broadCastIP = new byte[]{(byte) 255, (byte) 255, (byte) 255, (byte) 255};

    @Test
    public void testBuffer() {
        //for (byte b : broadCastIP) {
        //    System.out.println(b);
        //}
        //String[] name = "pan.baidu.com".split("\\.");
        //for (String str : name) {
        //    //先填写字符个数
        //    System.out.println((byte) str.length());
        //    //填写字符
        //    for (int i = 0; i < str.length(); ++i) {
        //        byte b = (byte) str.charAt(i);
        //        b = (byte) 0x85;
        //        System.out.println(b);
        //    }
        //}
        byte[] b = new byte[12];
        ByteBuffer buffer = ByteBuffer.wrap(b);
        buffer.put((byte) 10);
        buffer.put((byte) 11);
        buffer.put((byte) 12);
        buffer.put((byte) 13);
        buffer.put((byte) 14);
        buffer.put((byte) 15);
        buffer.put((byte) 16);
        buffer.put((byte) 17);
        buffer.put((byte) 18);
        buffer.put((byte) 19);
        buffer.put((byte) 20);
        buffer.position(2);
        //for (int i = 0; i < 11; i++) {
        //    System.out.println(buffer.get());
        //}
        //System.out.println(buffer.capacity());
        //System.out.println(buffer);
        byte[] msg = new byte[10];
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.get(msg);
        for (int i = 0; i < 10; i++) {
            System.out.println(msg[i]);
        }
    }

    @Test
    public void testProperties() throws IOException {
        Properties properties = new Properties();
        InputStream in = new FileInputStream("./default.properties");
        properties.load(in);
        String ip = properties.getProperty("ip");
        System.out.println(ip);
    }

    @Test
    public void testIP() throws UnknownHostException {
        InetAddress[] inetAddress = null;
        inetAddress = InetAddress.getAllByName("cherryYang");
        for (InetAddress ip : inetAddress) {
            System.out.println(ip.getHostAddress());
        }
    }
}
