package protocol;

import datalinklayer.DataLinkLayer;
import jpcap.packet.ARPPacket;
import jpcap.packet.Packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

/**
 * @Author Cherry
 * @Date 2020/3/16
 * @Time 17:36
 * @Brief ARP协议
 */

public class ARPProtocolLayer implements IProtocol {

    //public static HashMap<byte[], byte[]> ipToMacTable = new HashMap<>();
    //private HashMap<Integer, ArrayList<IMacReceiver>> ipToMacReceiverTable = new HashMap<>();

    /*
     * 数据包含数据链路层包头:dest_mac(6byte) + source_mac(6byte) + frame_type(2byte)
     * 因此读取ARP数据时需要跳过开头14字节(首部14B)
     */
    private static int ARP_OPCODE_START = 20;
    private static int ARP_SENDER_MAC_START = 22;
    private static int ARP_SENDER_IP_START = 28;
    private static int ARP_TARGET_IP_START = 38;

    ///**
    // * 链路层收到数据包后，把数据包推送给它
    // *
    // * @param packet packet
    // */
    //@Override
    //public void receivePacket(Packet packet) {
    //    if (packet == null) {
    //        return;
    //    }
    //
    //    //确保接收到的数据包是 ARP 类型
    //    EthernetPacket ethernetHeader = (EthernetPacket) packet.datalink;
    //
    //    /*
    //     * 数据链路层在发送数据包时会添加一个802.3的以太网包头，格式如下
    //     * 0-7字节：[0-6]Preamble , [7]start fo frame delimiter
    //     * 8-22字节: [8-13] destination mac, [14-19]: source mac
    //     * 20-21字节: type
    //     * type == 0x0806表示数据包是arp包, 0x0800表示IP包,0x8035是RARP包
    //     */
    //    if (ethernetHeader.frametype != EthernetPacket.ETHERTYPE_ARP) {
    //        return;
    //    }
    //    byte[] header = packet.header;
    //
    //    analyseARPMsg(header);
    //}

    /**
     * 处理 ARP 协议的回复消息
     *
     * @param data data
     * @return boolean
     */
    private boolean analyseARPMsg(byte[] data, HashMap<String, Object> infoTable) {
        /*
         * 解析获得的APR消息包，从中获得各项信息，此处默认返回的mac地址长度都是6
         */
        //先读取2,3字节，获取消息操作码，确定它是ARP回复信息
        byte[] opcode = new byte[2];
        //数组拷贝函数
        System.arraycopy(data, ARP_OPCODE_START, opcode, 0, 2);
        //转化为小端模式
        short op = ByteBuffer.wrap(opcode).getShort();
        if (op == ARPPacket.ARP_REQUEST) {
            System.out.println("ARP is sending a request...\n");
        }
        if (op != ARPPacket.ARP_REPLY) {
            return false;
        }
        //获取接受者IP，确定该数据包是回复给我们的
        byte[] ip = DataLinkLayer.getInstance().deviceIPAddress();

        //System.out.println("Send from IP:");
        for (int i = 0; i < 4; i++) {
            if (ip[i] != data[ARP_TARGET_IP_START + i]) {
                return false;
            }
            //System.out.print((ip[i] & 0xff) + ".");
        }
        //System.out.println();
        //System.out.println("Reply...");

        //获取发送者 ip
        byte[] senderIP = new byte[4];
        System.arraycopy(data, ARP_SENDER_IP_START, senderIP, 0, 4);


        //获取发送者 Mac 地址
        byte[] senderMac = new byte[6];
        System.arraycopy(data, ARP_SENDER_MAC_START, senderMac, 0, 6);

        infoTable.put("sender_mac", senderMac);
        infoTable.put("sender_ip", senderIP);

        //输出相关信息
        //System.out.println("Receive ARP reply msg with sender IP: ");
        //for (byte b : senderIP) {
        //    System.out.print(Integer.toUnsignedString(b & 0xff) + ".");
        //}
        //System.out.println("\nWith sender MAC:");
        //for (byte b : senderMac) {
        //    System.out.print(Integer.toHexString(b & 0xff) + ":");
        //}
        //System.out.println('\n');

        //更新 ARP 缓存表
        //ipToMacTable.put(senderIP, senderMac);
        //
        ////通知接收者 Mac 地址
        //int ipToInteger = ByteBuffer.wrap(senderIP).getInt();
        //List<IMacReceiver> receiverList = ipToMacReceiverTable.get(ipToInteger);
        //if (receiverList != null) {
        //    for (IMacReceiver receiver : receiverList) {
        //        receiver.receiveMacAddress(senderIP, senderMac);
        //    }
        //}
        return true;
    }

    ///**
    // * getMacByIP 是它提供给上层协议的接口，
    // * 当上层协议需要获得指定 ip 设备的 mac 地址时就调用该接口，
    // * 它先从缓存表中看指定 ip 对应的 mac 地址是否存在，
    // * 如果不存在就调用 sendARPRequestMsg 发送 ARP 请求包
    // *
    // * @param ip
    // * @param receiver
    // */
    //public void getMacByIp(byte[] ip, IMacReceiver receiver) {
    //    if (receiver == null) {
    //        return;
    //    }
    //    //查看给的ip的mac是否已经缓存
    //    int ipToInt = ByteBuffer.wrap(ip).getInt();
    //    if (ipToMacTable.get(ipToInt) != null) {
    //        receiver.receiveMacAddress(ip, ipToMacTable.get(ipToInt));
    //    }
    //    if (ipToMacReceiverTable.get(ipToInt) == null) {
    //        ipToMacReceiverTable.put(ipToInt, new ArrayList<IMacReceiver>());
    //        //发送 ARP 请求包
    //        makeARPRequestMsg(ip);
    //    }
    //    ArrayList<IMacReceiver> receiverList = ipToMacReceiverTable.get(ipToInt);
    //    if (!receiverList.contains(receiver)) {
    //        receiverList.add(receiver);
    //    }
    //    return;
    //}

    /**
     * 发送 ARP 请求包,
     * 把接收者的 mac 地址设置成[0xff, 0xff, 0xff, 0xff, 0xff, 0xff],
     * 这是一个广播硬件地址，于是所有设备都可以读取这个消息，
     * 如果接收设备的 IP 与数据包里对应的 target ip相同，
     * 那么它就应该构造同一个表，把自己的硬件地址存储在表中，
     * 返回给消息的发起者
     *
     * @param ip ip
     * @return
     */
    private byte[] makeARPRequestMsg(byte[] ip) {
        if (ip == null) {
            return null;
        }

        DataLinkLayer dataLinkInstance = DataLinkLayer.getInstance();
        byte[] broadcast = new byte[]{(byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255};
        int pointer = 0;
        byte[] data = new byte[28];
        data[pointer] = 0;
        pointer++;
        data[pointer] = 1;
        pointer++;

        //将字节序转换为大端
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort(ARPPacket.PROTOTYPE_IP);
        for (int i = 0; i < buffer.array().length; i++) {
            data[pointer] = buffer.array()[i];
            pointer++;
        }

        data[pointer++] = 6;
        data[pointer++] = 4;

        //将字节序转换为大端
        buffer = ByteBuffer.allocate(2);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort(ARPPacket.ARP_REQUEST);
        for (int i = 0; i < buffer.array().length; i++) {
            data[pointer++] = buffer.array()[i];
        }

        byte[] macAddress = dataLinkInstance.deviceMacAddress();
        for (byte address : macAddress) {
            data[pointer++] = address;
        }

        byte[] srcip = dataLinkInstance.deviceIPAddress();
        for (byte b : srcip) {
            data[pointer++] = b;
        }
        for (byte value : broadcast) {
            data[pointer++] = value;
        }
        for (byte b : ip) {
            data[pointer++] = b;
        }

        //dataLinkInstance.sendData(data, broadcast, EthernetPacket.ETHERTYPE_ARP);
        return data;
    }

    @Override
    public byte[] createHeader(HashMap<String, Object> headerInfo) {
        byte[] ip = (byte[]) headerInfo.get("sender_ip");
        if (ip == null) {
            return null;
        }

        byte[] header = makeARPRequestMsg(ip);
        return header;
    }

    @Override
    public HashMap<String, Object> handlePacket(Packet packet) {
        byte[] header = packet.header;
        HashMap<String, Object> infoTable = new HashMap<>();
        analyseARPMsg(header, infoTable);
        return infoTable;
    }
}
