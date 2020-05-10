package protocol;

import jpcap.packet.Packet;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @Author Cherry
 * @Date 2020/3/19
 * @Time 21:23
 * @Brief 使用责任链设计模式处理 ICMP 错误数据报解读
 * 责任链模式降低了代码的耦合度
 * @Reference: https://www.runoob.com/design-pattern/chain-of-responsibility-pattern.html
 * https://www.jianshu.com/p/2a76c794c2b0
 * https://www.jianshu.com/p/e52bf38f9077
 */

public class ICMPProtocolLayer implements IProtocol {
    public static byte PROTOCL_ICMP = 1;
    private ArrayList<IProtocol> protocol_header_list = new ArrayList<>();
    private Packet packet;

    public ICMPProtocolLayer() {
        //增加icmp echo 协议包头创建对象
        protocol_header_list.add(new ICMPEchoHeader());

        protocol_header_list.add(new ICMPTimeStampHeader());

        protocol_header_list.add(new ICMPTimeExceededHeader());
    }

    //checkType针对的是IPV6

    private HashMap<String, Object> analyzeICMPMessage() {

        HashMap<String, Object> info = null;

        info = handleICMPInfoMsg(this.packet);

        return info;
    }

    private HashMap<String, Object> handleICMPInfoMsg(Packet packet) {
        for (int i = 0; i < protocol_header_list.size(); i++) {
            IProtocol handler = protocol_header_list.get(i);
            HashMap<String, Object> info = handler.handlePacket(packet);
            if (info != null) {
                return info;
            }
        }

        return null;
    }

    @Override
    public byte[] createHeader(HashMap<String, Object> headerInfo) {
        for (int i = 0; i < protocol_header_list.size(); i++) {
            byte[] buff = protocol_header_list.get(i).createHeader(headerInfo);
            if (buff != null) {
                return buff;
            }
        }

        return null;
    }

    @Override
    public HashMap<String, Object> handlePacket(Packet packet) {
        this.packet = packet;

        return analyzeICMPMessage();
    }
    //private static byte PROTOCOL_ICMP = 1;
    //private static int ICMP_DATA_OFFSET = 20 + 14;//越过20B的IP包头和14B的数据链路层包头
    //private static int PROTOCOL_FIELD_IN_IP_HEADER = 14 + 9;//越过数据链路层包头，协议类型从第9字节开始
    //
    //private List<IICMPErrorMsgHandler> error_handler_list = new ArrayList<>();
    //
    //@Override
    //public byte[] createHeader(HashMap<String, Object> headerInfo) {
    //    return new byte[0];
    //}
    //
    //@Override
    //public HashMap<String, Object> handlePacket(Packet packet) {
    //    return null;
    //}
    //
    //private enum ICMP_MSG_TYPE {
    //    ICMP_UNKNOWN_MSG,
    //    ICMP_ERROR_MSG,
    //    ICMP_INFO_MSG
    //}
    //
    //private int icmp_type = 0;
    //private int icmp_code = 0;
    //private byte[] packet_header = null;//ICMP报文首部(包括ICMP报文前8个字节以及数据链路层和IP首部)
    //private byte[] packet_data = null;
    //
    ///**
    // * 添加消息处理对象
    // */
    //public ICMPProtocolLayer() {
    //    error_handler_list.add(new ICMPUnreachableMsgHandler());
    //}

    ///**
    // * 接收 ICMP 协议的报文并且做一些分析
    // *
    // * @param packet packet
    // */
    //@Override
    //public void receivePacket(Packet packet) {
    //    if (packet == null) {
    //        return;
    //    }
    //
    //    //判断是否为IP数据报
    //    EthernetPacket ethernetPacket = (EthernetPacket) packet.datalink;
    //    if (ethernetPacket.frametype != EthernetPacket.ETHERTYPE_IP) {
    //        return;
    //    }
    //
    //    //读取IP首部，也就是接在数据链路层后面的20字节，读取协议字段是否表示ICMP，也就是偏移第10个字节的值为1
    //    if (packet.header[PROTOCOL_FIELD_IN_IP_HEADER] != PROTOCOL_ICMP) {
    //        return;
    //    }
    //
    //    //这里的数据报的首部指的是数据链路层首部，IP数据报首部以及ICMP数据报的前8个字节
    //    //跳过IP首部(包括数据链路层首部)将ICMP协议首部拷贝到新数组中
    //    packet_header = Arrays.copyOfRange(packet.header, ICMP_DATA_OFFSET, packet.header.length);
    //    packet_data = packet.data;
    //
    //    analyseICMPMsg(packet_header);
    //}

    ///**
    // * 传递错误消息的ICMP数据报type处于0到127，
    // * 传递控制信息的ICMP数据报type处于128到255
    // *
    // * @param type 错误类型
    // * @return 返回错误类型枚举
    // */
    //private ICMP_MSG_TYPE checkType(int type) {
    //    if (type > 0 && type <= 127) {
    //        return ICMP_MSG_TYPE.ICMP_ERROR_MSG;
    //    } else if (type > 127 && type <= 255) {
    //        return ICMP_MSG_TYPE.ICMP_INFO_MSG;
    //    } else {
    //        return ICMP_MSG_TYPE.ICMP_UNKNOWN_MSG;
    //    }
    //}
    //
    ///**
    // * 分析数据包头，根据不同类型进入不同责任链
    // *
    // * @param packet_header 首部
    // */
    //private void analyseICMPMsg(byte[] packet_header) {
    //    ByteBuffer byteBuffer = ByteBuffer.wrap(packet_header);
    //    icmp_type = byteBuffer.get(0);
    //    icmp_code = byteBuffer.get(1);
    //
    //    //检测当前数据包是错误信息还是控制信息,并根据不同情况分别处理
    //    if (checkType(icmp_type) == ICMP_MSG_TYPE.ICMP_ERROR_MSG) {
    //        handleICMPErrorMsg(packet_data);
    //    }
    //}
    //
    ///**
    // * ICMP错误数据报的类型很多，一个 type和 code的组合就能对应一种数据类型，我们不能把对所有数据类型的处理全部
    // * 塞入一个函数，那样会造成很多个if..else分支，使得代码复杂，膨胀，极难维护，因此我们使用责任链模式来处理
    // *
    // * @param data data
    // */
    //private void handleICMPErrorMsg(byte[] data) {
    //    for (IICMPErrorMsgHandler handler : error_handler_list) {
    //        if (handler.handlerICMPERRORMsg(icmp_type, icmp_code, data)) {
    //            break;
    //        }
    //    }
    //}


}
