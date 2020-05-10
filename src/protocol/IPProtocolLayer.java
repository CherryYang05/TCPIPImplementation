package protocol;

import datalinklayer.DataLinkLayer;
import jpcap.packet.Packet;
import utils.Utility;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

/**
 * @Author Cherry
 * @Date 2020/4/8
 * @Time 13:26
 * @Brief
 */

public class IPProtocolLayer implements IProtocol {
    private static int ETHERNET_FRAME_HEADER_LENGTH = 14;
    private static byte IP_VERSION = 4;
    private static int CHECKSUM_OFFSET = 10;
    private static int HEADER_LENGTH_OFFSET = 0 + ETHERNET_FRAME_HEADER_LENGTH;
    private static int TOTAL_LENGTH_OFFSET = 2 + ETHERNET_FRAME_HEADER_LENGTH;
    private static int SOURCE_IP_OFFSET = 12 + ETHERNET_FRAME_HEADER_LENGTH;
    private static int DEST_IP_OFFSET = 16 + ETHERNET_FRAME_HEADER_LENGTH;
    private static int PROTOCOL_INDICATOR_OFFSET = 9 + ETHERNET_FRAME_HEADER_LENGTH;

    @Override
    public byte[] createHeader(HashMap<String, Object> headerInfo) {
        byte version = (byte) (IP_VERSION & 0x0F);
        byte internetHeaderLength = 5;
        if (headerInfo.get("internet_header_length") != null) {
            internetHeaderLength = (byte) headerInfo.get("internet_header_length");
        }
        byte[] buffer = new byte[internetHeaderLength * 4];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        byteBuffer.put((byte) (version << 4 | internetHeaderLength));
        byte b = byteBuffer.get(0);

        byte dscp = 0;
        if (headerInfo.get("dscp") != null) {
            dscp = (byte) headerInfo.get("dscp");
        }
        byte ecn = 0;
        if (headerInfo.get("ecn") != null) {
            ecn = (byte) headerInfo.get("ecn");
        }
        byteBuffer.put((byte) (dscp << 2 | ecn));

        if (headerInfo.get("data_length") == null) {
            return null;
        }
        /*
         * 总长度等于IP数据包包头长度加上末尾option长度加上后续数据长度
         */
        int optionLength = 0;
        byte[] options = null;

        if (headerInfo.get("options") != null) {
            options = (byte[]) headerInfo.get("options");
            optionLength += options.length;
        }
        short totalLength = (short) ((int) headerInfo.get("data_length") + optionLength + internetHeaderLength * 4);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putShort(totalLength);

        short identification = 0;
        if (headerInfo.get("identification") != null) {
            identification = (short) headerInfo.get("identification");
        }
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putShort(identification);

        short flagAndOffset = 0;
        if (headerInfo.get("flag") != null) {
            flagAndOffset = (short) (((short) headerInfo.get("flag")) << 13);
        }
        if (headerInfo.get("fragment_offset") != null) {
            flagAndOffset |= ((short) headerInfo.get("fragment_offset"));
        }
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putShort(flagAndOffset);

        byte timeToLive = 64;
        if (headerInfo.get("time_to_live") != null) {
            timeToLive = (byte) headerInfo.get("time_to_live");
        }
        byteBuffer.put(timeToLive);

        byte protocol = 0;
        if (headerInfo.get("protocol") == null) {
            return null;
        }
        protocol = (byte) headerInfo.get("protocol");
        byteBuffer.put(protocol);

        short checkSum = 0;
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putShort(checkSum);

        //设置source ip
        byte[] ipArr = DataLinkLayer.getInstance().deviceIPAddress();
        ByteBuffer ip = ByteBuffer.wrap(ipArr);
        int srcIP = ip.getInt();
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putInt(srcIP);

        int destIP = 0;
        if (headerInfo.get("destination_ip") == null) {
            return null;
        }
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        destIP = (int) headerInfo.get("destination_ip");
        byteBuffer.putInt(destIP);


        if (headerInfo.get("options") != null) {
            byteBuffer.put(options);
        }

        checkSum = (short) Utility.checksum(byteBuffer.array(), byteBuffer.array().length);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putShort(CHECKSUM_OFFSET, checkSum);

        return byteBuffer.array();
    }

    @Override
    public HashMap<String, Object> handlePacket(Packet packet) {
        /*
         * 解析收到数据包的IP包头，暂时不做校验和检测，默认网络发送的数据包不会出错,
         * 暂时忽略对option段的处理
         */

        byte[] ip_data = new byte[packet.header.length + packet.data.length];
        ByteBuffer buffer = ByteBuffer.wrap(ip_data);
        buffer.put(packet.header);
        buffer.put(packet.data);

        HashMap<String, Object> headerInfo = new HashMap<>();

        //获取发送者IP
        byte[] src_ip = new byte[4];
        buffer.position(SOURCE_IP_OFFSET);
        buffer.get(src_ip, 0, 4);
        headerInfo.put("source_ip", src_ip);

        /*
        //打印每个包的发送者的IP信息
        System.out.print("发送者IP:");
        for (int i = 0; i < src_ip.length; i++) {
            System.out.print((src_ip[i] & 0xff) + ".");
        }
        System.out.println();
        */


        //获取接受者IP
        byte[] dest_ip = new byte[4];
        buffer.position(DEST_IP_OFFSET);
        buffer.get(dest_ip, 0, 4);
        headerInfo.put("dest_ip", dest_ip);

        /*
        //打印每个包的接受者IP信息
        System.out.print("接收者IP:");
        for (int i = 0; i < dest_ip.length; i++) {
            System.out.print((dest_ip[i] & 0xff) + ".");
        }
        System.out.println();
        */


        //确保接受者是我们自己
        byte[] ip = DataLinkLayer.getInstance().deviceIPAddress();
        for (int i = 0; i < ip.length; i++) {
            if (ip[i] != dest_ip[i]) {
                return null;
            }
        }


        //获得下一层协议编号
        buffer.position(0);
        byte protocol = buffer.get(PROTOCOL_INDICATOR_OFFSET);
        headerInfo.put("protocol", protocol);
        int k = 0;
        if (protocol == 1) {
            k = 2;
            //ICMP协议的协议号
            System.out.println("Receive protocol 1(ICMP)\n");
        }

        byte headerLength = buffer.get(HEADER_LENGTH_OFFSET);
        headerLength &= 0x0F;
        //*4得到包头字节长度
        headerLength *= 4;
        short totalLength = buffer.getShort(TOTAL_LENGTH_OFFSET);
        int dataLength = totalLength - headerLength;
        ;
        byte[] data = new byte[dataLength];
        buffer.position(headerLength + ETHERNET_FRAME_HEADER_LENGTH);
        buffer.get(data, 0, dataLength);
        headerInfo.put("header", data);
        return headerInfo;
    }
}
