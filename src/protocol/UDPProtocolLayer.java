package protocol;

import jpcap.packet.Packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

/**
 * @Author Cherry
 * @Date 2020/5/10
 * @Time 17:03
 * @Brief 进行组装 UDP 协议报头
 */

public class UDPProtocolLayer implements IProtocol {

    private static short UDP_LENGTH_WITHOUT_DATA = 8;
    public static byte PROTOCOL_UDP = 17;

    private static short UDP_SRC_PORT_OFFSET = 0;
    private static short UDP_DST_PORT_OFFSET = 2;
    private static short UDP_LENGTH_OFFSET = 4;

    @Override
    public byte[] createHeader(HashMap<String, Object> headerInfo) {
        short total_length = UDP_LENGTH_WITHOUT_DATA;
        byte[] data = null;
        if (headerInfo.get("data") != null) {
            data = (byte[]) headerInfo.get("data");
            total_length += data.length;
        }

        byte[] buf = new byte[total_length];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buf);
        if (headerInfo.get("source_port") == null) {
            return null;
        }
        char srcPort = (char) headerInfo.get("source_port");
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putChar(srcPort);

        if (headerInfo.get("dest_port") == null) {
            return null;
        }
        char destPort = (char) headerInfo.get("dest_port");
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putChar(destPort);

        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putShort(total_length);

        //UDP 报头的检验和可以直接设为 0xFFFF(在DHCP中设为0xFFFF会产生错误，这里设为0)
        char checkSum = 0x0;
        byteBuffer.putChar(checkSum);

        if (data != null) {
            byteBuffer.put(data);
        }
        return byteBuffer.array();
    }

    /**
     * 处理 UDP 报头
     *
     * @param packet
     * @return
     */
    @Override
    public HashMap<String, Object> handlePacket(Packet packet) {
        ByteBuffer buffer = ByteBuffer.wrap(packet.header);
        HashMap<String, Object> headerInfo = new HashMap<>();
        headerInfo.put("src_port", buffer.getShort(UDP_SRC_PORT_OFFSET));
        headerInfo.put("dest_port", buffer.getShort(UDP_DST_PORT_OFFSET));
        headerInfo.put("length", buffer.getShort(UDP_LENGTH_OFFSET));
        headerInfo.put("data", packet.data);
        return headerInfo;
    }
}
