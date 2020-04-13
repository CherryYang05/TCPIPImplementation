package protocol;

import jpcap.packet.Packet;
import utils.Utility;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Random;

/**
 * @Author Cherry
 * @Date 2020/4/13
 * @Time 21:13
 * @Brief 用来组装 HPing 的首部
 */

public class ICMPTimeStampHeader implements IProtocol {
    private static int ICMP_TIMESTAMP_HEADER_LENGTH = 20;
    private static byte ICMP_TIMESTAMP_REQUEST_TYPE = 13;
    private static byte ICMP_TIMESTAMP_REPLY_TYPE = 14;

    private static short ICMP_ECHO_IDENTIFIER_OFFSET = 4;
    private static short ICMP_ECHO_SEQUENCE_NUM_OFFSET = 6;
    private static short ICMP_ORIGINAL_TIMESTAMP_OFFSET = 8;
    private static short ICMP_RECEIVE_TIMESTAMP_OFFSET = 12;
    private static short ICMP_TRANSMIT_TIMESTAMP_OFFSET = 16;

    @Override
    public byte[] createHeader(HashMap<String, Object> headerInfo) {
        String headerName = (String) headerInfo.get("header");
        if (!headerName.equals("timestamp")) {
            return null;
        }
        int bufferLen = ICMP_TIMESTAMP_HEADER_LENGTH;
        byte[] buffer = new byte[bufferLen];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        byte type = ICMP_TIMESTAMP_REQUEST_TYPE;
        byteBuffer.put(type);
        byte code = 0;
        byteBuffer.put(code);
        short checkSum = 0;
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putShort(checkSum);
        short identifier = 0;
        if (headerInfo.get("identifier") == null) {
            Random random = new Random();
            identifier = (short) random.nextInt();
            headerInfo.put("identifier", identifier);
        }
        identifier = (short) headerInfo.get("identifier");
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putShort(identifier);

        short sequenceNumber = 0;
        if (headerInfo.get("sequenceNumber") != null) {
            sequenceNumber = (short) headerInfo.get("sequenceNumber");
        }
        headerInfo.put("sequenceNumber", sequenceNumber);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putShort(sequenceNumber);

        if (headerInfo.get("original_time") != null) {
            int original_time = (int) headerInfo.get("original_time");
            byteBuffer.putInt(original_time);
        }

        if (headerInfo.get("receive_time") != null) {
            int receive_time = (int) headerInfo.get("receive_time");
            byteBuffer.putInt(receive_time);
        }

        if (headerInfo.get("transmit_time") != null) {
            int transmit_time = (int) headerInfo.get("transmit_time");
            byteBuffer.putInt(transmit_time);
        }
        checkSum = (short) Utility.checksum(byteBuffer.array(), byteBuffer.array().length);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putShort(2, checkSum);
        System.out.println("ICMP timestamp header, checksum: " + String.format("0x%08x", checkSum));

        return byteBuffer.array();
    }


    /**
     * 读取三个时间戳
     *
     * @param packet
     * @return
     */
    @Override
    public HashMap<String, Object> handlePacket(Packet packet) {
        ByteBuffer buffer = ByteBuffer.wrap(packet.header);
        if (buffer.get(0) != ICMP_TIMESTAMP_REPLY_TYPE) {
            return null;
        }
        HashMap<String, Object> header = new HashMap<>();
        header.put("identifier", buffer.getShort(ICMP_ECHO_IDENTIFIER_OFFSET));
        header.put("sequence", buffer.getShort(ICMP_ECHO_SEQUENCE_NUM_OFFSET));
        header.put("original_time", buffer.getInt(ICMP_ORIGINAL_TIMESTAMP_OFFSET));
        header.put("receive_time", buffer.getInt(ICMP_RECEIVE_TIMESTAMP_OFFSET));
        header.put("transmit_time", buffer.getInt(ICMP_TRANSMIT_TIMESTAMP_OFFSET));
        return header;
    }
}
