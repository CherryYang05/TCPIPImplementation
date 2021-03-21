package utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @Author Cherry
 * @Date 2020/4/8
 * @Time 13:42
 * @Brief 计算检验和
 */

public class Utility {

    /**
     * IP数据包校验和
     *
     * @param buf
     * @param length
     * @return
     */
    public static long checksum(byte[] buf, int length) {
        int i = 0;
        long sum = 0;
        while (length > 0) {
            sum += (buf[i++] & 0xff) << 8;
            if ((--length) == 0) break;
            sum += (buf[i++] & 0xff);
            --length;
        }

        return (~((sum & 0xFFFF) + (sum >> 16))) & 0xFFFF;
    }

    /**
     * 获得本机 IP
     *
     * @return
     * @throws IOException
     */
    public static String getMasterIP() throws IOException {
        Properties properties = new Properties();
        InputStream in = new FileInputStream("./default.properties");
        properties.load(in);
        return properties.getProperty("ip");
    }

    /**
     * 获得路由器 IP
     *
     * @return
     * @throws IOException
     */
    public static String getRouterIP() throws IOException {
        Properties properties = new Properties();
        InputStream in = new FileInputStream("./default.properties");
        properties.load(in);
        return properties.getProperty("routerip");
    }
}

