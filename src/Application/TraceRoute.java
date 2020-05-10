package Application;

import protocol.IProtocol;
import protocol.ProtocolManager;
import protocol.UDPProtocolLayer;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * @Author Cherry
 * @Date 2020/5/10
 * @Time 16:34
 * @Brief TraceRoute 应用实现
 * 主要原理是通过不断修改 IP 报头中 TTL 字段的值来让在中途停止的路由器返回一个 ICMP 包，
 * 通过解析 ICMP 包来获得路径中路由器的 IP 地址
 */

public class TraceRoute extends Application {

    private char dest_port = 33434;
    private byte[] dest_ip = null;
    private byte time_to_live = 1;

    private static byte ICMP_TIME_EXCEEDED_TYPE = 11;
    private static byte ICMP_TIME_EXCEEDED_CODE = 0;

    /**
     * 构造器，传入想要 trace 的目的 IP
     *
     * @param dest_ip
     */
    public TraceRoute(byte[] dest_ip) {
        this.dest_ip = dest_ip;
    }

    public void startTraceRoute() {
        try {
            byte[] packet = createPacket();
            ProtocolManager.getInstance().sendData(packet, dest_ip);
            ProtocolManager.getInstance().registToReceiveICMPPacket(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 需要产生 UDP 报头
     *
     * @return
     * @throws Exception
     */
    private byte[] createPacket() throws Exception {
        byte[] udpHeader = this.createUDPHeader();
        if (udpHeader == null) {
            throw new Exception("UDP Header create fail!!");
        }
        byte[] ipHeader = this.createIP4Header(udpHeader.length);

        //构造IP报头和ICMP ECHO报头，并结合在一起
        byte[] packet = new byte[udpHeader.length + ipHeader.length];
        ByteBuffer packerBuffer = ByteBuffer.wrap(packet);
        packerBuffer.put(ipHeader);
        packerBuffer.put(udpHeader);
        return packerBuffer.array();
    }

    /**
     * 构造 UDP 报头
     *
     * @return
     */
    private byte[] createUDPHeader() throws Exception {
        IProtocol udpProto = ProtocolManager.getInstance().getProtocol("udp");
        if (udpProto == null) {
            throw new Exception("UDP Header create fail!!");
        }
        HashMap<String, Object> headerinfo = new HashMap<>();
        char udpPort = (char) this.port;
        headerinfo.put("source_port", udpPort);
        headerinfo.put("dest_port", dest_port);
        //默认24个0
        byte[] data = new byte[24];
        headerinfo.put("data", data);
        return udpProto.createHeader(headerinfo);
    }

    /**
     * 构造 IP 报头
     *
     * @return
     */
    private byte[] createIP4Header(int dataLength) {
        IProtocol ip4Proto = ProtocolManager.getInstance().getProtocol("ip");
        if (ip4Proto == null || dataLength < 0) {
            return null;
        }

        //创建 IP 报头默认情况下只需要发送数据长度，下层协议号，接收方 IP 地址
        HashMap<String, Object> headerInfo = new HashMap<>();
        headerInfo.put("data_length", dataLength);
        ByteBuffer destIP = ByteBuffer.wrap(this.dest_ip);
        headerInfo.put("destination_ip", destIP.getInt());
        byte protocol = UDPProtocolLayer.PROTOCOL_UDP;
        headerInfo.put("protocol", protocol);
        headerInfo.put("identification", (short) this.port);

        //设置time_to_live递增
        headerInfo.put("time_to_live", time_to_live);
        byte[] ipHeader = ip4Proto.createHeader(headerInfo);
        return ipHeader;
    }

    public void handleData(HashMap<String, Object> data) {
        if (data.get("type") == null || data.get("code") == null) {
            return;
        }

        //如果收到的不是 icmp_time_exceeded 类型的消息，则舍弃
        if ((byte) data.get("type") != ICMP_TIME_EXCEEDED_TYPE ||
                (byte) data.get("code") != ICMP_TIME_EXCEEDED_CODE) {
            return;
        }

        //获取发送该数据包的路由器的 IP
        //这里要说明一点，因为现在很多路由器不允许外界去 ping 访问，
        //因此无法获得路径上其他路由器的 IP 信息，也就是说最后只能看到
        //当前局域网网关的那个路由器
        byte[] source_ip = (byte[]) data.get("source_ip");
        try {
            String routerIP = InetAddress.getByAddress(source_ip).toString();
            System.out.println("IP of the " + time_to_live + "th router in sending route is:" + routerIP);
            dest_port++;
            time_to_live++;//TTL + 1 后继续发送
            startTraceRoute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
