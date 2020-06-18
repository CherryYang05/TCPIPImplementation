package protocol;

import jpcap.packet.Packet;

import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * @Author Cherry
 * @Date 2020/5/10
 * @Time 18:44
 * @Brief
 */

public class ICMPTimeExceededHeader implements IProtocol {

    private static byte ICMP_TIME_EXCEEDED_TYPE = 11;
    private static byte ICMP_TIME_EXCEEDED_CODE = 0;
    //这里包括1字节的type,1字节的code,2字节的校验和以及4字节无用部分(Unused)，一共8字节
    private static int ICMP_TIME_EXCEEDED_DATA_OFFSET = 8;

    @Override
    public byte[] createHeader(HashMap<String, Object> headerInfo) {
        return null;
    }

    /**
     * 解析路由器回发的数据包
     * @param packet 数据包
     * @return
     */
    @Override
    public HashMap<String, Object> handlePacket(Packet packet) {
        ByteBuffer buffer = ByteBuffer.wrap(packet.header);
        if (buffer.get(0) != ICMP_TIME_EXCEEDED_TYPE && buffer.get(1) != ICMP_TIME_EXCEEDED_CODE) {
            return null;
        }
        HashMap<String, Object> headerInfo = new HashMap<>();
        headerInfo.put("type", ICMP_TIME_EXCEEDED_TYPE);
        headerInfo.put("code", ICMP_TIME_EXCEEDED_CODE);
        byte[] data = new byte[packet.header.length - ICMP_TIME_EXCEEDED_DATA_OFFSET];
        buffer.position(ICMP_TIME_EXCEEDED_DATA_OFFSET);
        buffer.get(data, 0, data.length);
        headerInfo.put("data", data);
        return headerInfo;
    }
}
