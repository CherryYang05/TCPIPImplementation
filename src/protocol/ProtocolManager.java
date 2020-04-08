package protocol;

import Application.ApplicationManager;
import Application.IApplication;
import datalinklayer.DataLinkLayer;
import jpcap.PacketReceiver;
import jpcap.packet.EthernetPacket;
import jpcap.packet.IPPacket;
import jpcap.packet.Packet;

import java.util.Arrays;
import java.util.HashMap;

/**
 * @Author Cherry
 * @Date 2020/4/8
 * @Time 12:47
 * @Brief 所有协议对象都接受 ProtocolManager的统一管理，
 * 当应用对象需要调用某个协议对象创建包头时，需要经过
 * ProtocolManager获取相应对象，同时它是唯一一个从网卡
 * 接收数据的对象，当网卡把数据包传递给它后，它通过解析
 * 网络包的包头，决定把数据包转交给对应的网络协议对象解析
 */

public class ProtocolManager implements PacketReceiver {
    private static ProtocolManager instance = null;
    private static ARPProtocolLayer arpLayer = null;
    private static DataLinkLayer dataLinkLayer = null;
    private static HashMap<String, byte[]> ipToMacTable = null;
    private static HashMap<String, byte[]> dataWaitToSend = null;

    private static byte[] broadcast = new byte[]{(byte) 255, (byte) 255, (byte) 255,
            (byte) 255, (byte) 255, (byte) 255};

    private ProtocolManager() {
    }

    public static ProtocolManager getInstance() {
        if (instance == null) {
            instance = new ProtocolManager();
            dataLinkLayer = DataLinkLayer.getInstance();
            ipToMacTable = new HashMap<>();
            dataWaitToSend = new HashMap<>();
            dataLinkLayer.registerPacketReceiver(instance);
            arpLayer = new ARPProtocolLayer();
        }
        return instance;
    }

    /**
     * 获取协议类型
     *
     * @param name name
     * @return IProtocol
     */
    public IProtocol getProtocol(String name) {
        switch (name.toLowerCase()) {
            case "icmp":
                return (IProtocol) new ICMPProtocolLayer();
            case "ip":
                return (IProtocol) new IPProtocolLayer();
        }
        return null;
    }

    /**
     * @param data
     * @param ip
     * @throws Exception
     */
    public void sendData(byte[] data, byte[] ip) throws Exception {
        // 发送数据前先检查给定ip的mac地址是否存在，如果没有则先让ARP协议获取mac地址
        byte[] mac = ipToMacTable.get(Arrays.toString(ip));
        if (mac == null) {
            HashMap<String, Object> headerInfo = new HashMap<>();
            headerInfo.put("sender_ip", ip);
            byte[] arpRequest = arpLayer.createHeader(headerInfo);
            if (arpRequest == null) {
                throw new Exception("Get mac address header fail");
            }

            dataLinkLayer.sendData(arpRequest, broadcast, EthernetPacket.ETHERTYPE_ARP);
            //将要发送的数据存起来，等待mac地址返回后再发送(实际上应该用队列，假设此时只有一个数据包等待)
            dataWaitToSend.put(Arrays.toString(ip), data);
        } else {
            //如果mac地址已经存在则直接发送数据
            dataLinkLayer.sendData(data, mac, IPPacket.IPPROTO_IP);
        }
    }

    /**
     * @param packet
     */
    @Override
    public void receivePacket(Packet packet) {
        if (packet == null) {
            return;
        }

        //确保收到的数据包书ARP类型
        EthernetPacket etherHeader = (EthernetPacket) packet.datalink;
        /*
         * 数据链路层在发送数据包时会添加一个802.3的以太网包头，格式如下
         * 0-7字节：[0-6]Preamble , [7]start fo frame delimiter
         * 8-22字节: [8-13] destination mac, [14-19]: source mac
         * 20-21字节: type
         * type == 0x0806表示数据包是arp包, 0x0800表示IP包,0x8035是RARP包
         */
        if (etherHeader.frametype == EthernetPacket.ETHERTYPE_ARP) {
            //调用 ARP 协议解析数据包
            ARPProtocolLayer arpLayer = new ARPProtocolLayer();
            HashMap<String, Object> info = arpLayer.handlePacket(packet);
            byte[] senderIP = (byte[]) info.get("sender_ip");
            byte[] senderMac = (byte[]) info.get("sender_mac");
            ipToMacTable.put(Arrays.toString(senderIP), senderMac);
            //一旦有mac地址更新后，查看缓存表是否有等待发送的数据
            sendWaitingData(senderIP);
        }

        //处理IP包头
        if (etherHeader.frametype == EthernetPacket.ETHERTYPE_IP) {
            handleIPPacket(packet);
        }
    }

    private void handleIPPacket(Packet packet) {
        IProtocol ipProtocol = (IProtocol) new IPProtocolLayer();
        HashMap<String, Object> info = ipProtocol.handlePacket(packet);
        if (info == null) {
            return;
        }
        byte protocol = 0;
        if (info.get("protocol") != null) {
            protocol = (byte) info.get("protocol");
            //设置下一层协议的头部
            packet.header = (byte[]) info.get("header");
            System.out.println("Receive packet with protocol: " + protocol);
        }
        if (protocol != 0) {
            switch (protocol) {
                case IPPacket.IPPROTO_ICMP:
                    handleICMPPacket(packet);
                    break;
                default:
                    return;
            }
        }
    }

    /**
     * @param packet
     */
    private void handleICMPPacket(Packet packet) {
        IProtocol icmpProtocol = (IProtocol) new ICMPProtocolLayer();
        HashMap<String, Object> headerInfo = icmpProtocol.handlePacket(packet);
        short identifier = (short) headerInfo.get("identifier");
        IApplication app = ApplicationManager.getInstance().getApplicationByPort(identifier);
        if (app != null && !app.isClosed()) {
            app.handleData(headerInfo);
        }
    }

    /**
     * 发送正在等待的数据，通过 ip获得 mac地址，调用链路层对象，将数据发送出去
     *
     * @param destIP
     */
    private void sendWaitingData(byte[] destIP) {
        byte[] data = dataWaitToSend.get(Arrays.toString(destIP));
        byte[] mac = ipToMacTable.get(Arrays.toString(destIP));
        if (data != null && mac != null) {
            dataLinkLayer.sendData(data, mac, EthernetPacket.ETHERTYPE_IP);
        }
    }
}
