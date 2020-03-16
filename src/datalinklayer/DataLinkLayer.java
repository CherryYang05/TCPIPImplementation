package datalinklayer;

import jpcap.JpcapCaptor;
import jpcap.JpcapSender;
import jpcap.NetworkInterface;
import jpcap.NetworkInterfaceAddress;
import jpcap.packet.EthernetPacket;
import jpcap.packet.Packet;
import protocol.ARPProtocolLayer;
import utils.IMacReceiver;
import utils.PacketProvider;

import java.net.Inet4Address;
import java.net.InetAddress;

/**
 * @Author Cherry
 * @Date 2020/3/14
 * @Time 22:57
 * @Brief 模拟数据链路层
 * ARPProtocolLayer 要求所有通过它获取 mac地址的对象
 * 都必须实现 IMacReceiver接口，有可能很多个上层协议对象
 * 都需要获得同一个 ip 对应设备的 mac 地址，它会把这些对象
 * 存储在一个队里中，一旦给定ip设备返回包含它 mac 地址的 ARP
 * 消息后，ARPProtocolLayer从消息中解读出 mac 地址，
 * 它就会把该地址推送给所有需要的接收者
 */

public class DataLinkLayer extends PacketProvider implements jpcap.PacketReceiver, IMacReceiver {

    //单例
    private static DataLinkLayer instance = null;
    private NetworkInterface device = null;
    private Inet4Address ipAddress = null;
    private byte[] macAddress = null;
    JpcapSender jpcapSender = null;

    /**
     * 私有化构造器，为了实现单例模式
     */
    private DataLinkLayer() {
    }

    /**
     * 创建 DataLinkLayer 实例
     *
     * @return 返回 DataLinkLayer 实例
     */
    public static DataLinkLayer getInstance() {
        if (instance == null) {
            instance = new DataLinkLayer();
        }
        return instance;
    }

    /**
     * 获得网卡对象
     *
     * @param device 网卡
     */
    public void initWithOpenDevice(NetworkInterface device) {
        this.device = device;
        this.ipAddress = this.getDeviceIpAddress();
        this.macAddress = new byte[6];
        this.getDeviceMacAddress();

        JpcapCaptor jpcapCaptor = null;

        try {
            jpcapCaptor = JpcapCaptor.openDevice(device, 2000, false, 3000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (jpcapSender == null && jpcapCaptor != null) {
            this.jpcapSender = jpcapCaptor.getJpcapSenderInstance();
        } else {
            System.out.println("Device open failed...");
        }

        //测试 ARP 协议
        this.testARPProtocol();
    }

    /**
     * 获取网卡 IP
     *
     * @return
     */
    private Inet4Address getDeviceIpAddress() {
        for (NetworkInterfaceAddress addr : device.addresses) {
            //网卡网址符合ipv4规范才是可用网卡
            if (!(addr.address instanceof Inet4Address)) {
                continue;
            }
            return (Inet4Address) addr.address;
        }
        return null;
    }

    /**
     * 获取网卡的 Mac 地址
     */
    private void getDeviceMacAddress() {
        int count = 0;
        for (byte b : this.device.mac_address) {
            this.macAddress[count++] = (byte) (b & 0xff);
        }
    }

    public byte[] deviceIPAddress() {
        return this.ipAddress.getAddress();
    }

    public byte[] deviceMacAddress() {
        return this.macAddress;
    }

    /**
     * 将收到的数据包推送给上层协议
     *
     * @param packet
     */
    @Override
    public void receivePacket(Packet packet) {
        this.pushPacketToReceivers(packet);
    }

    /**
     * 给上层协议要发送的数据添加数据链路层包头，然后使用网卡发送出去
     *
     * @param data          数据
     * @param dstMacAddress 目的 Mac 地址
     * @param frameType     类型，是发送还是回复
     */
    public void sendData(byte[] data, byte[] dstMacAddress, short frameType) {
        if (data == null) {
            return;
        }
        Packet packet = new Packet();
        packet.data = data;

        /*
         * 数据链路层会给发送数据添加包头：
         * 0-5字节：接收者的mac地址
         * 6-11字节：发送者mac地址
         * 12-13字节：数据包发送类型，0x0806表示ARP包，0x0800表示ip包，
         */
        EthernetPacket ether = new EthernetPacket();
        ether.frametype = EthernetPacket.ETHERTYPE_ARP;
        ether.src_mac = this.device.mac_address;
        ether.dst_mac = dstMacAddress;
        packet.datalink = ether;
        jpcapSender.sendPacket(packet);
    }


    /**
     * 对 IMacReceiver 接口中方法的实现
     *
     * @param ip  ip
     * @param mac mac
     */
    @Override
    public void receiveMacAddress(byte[] ip, byte[] mac) {
        System.out.println("Receive ARP reply msg with sender ip: ");
        for (byte b : ip) {
            System.out.print(Integer.toUnsignedString(b & 0xff) + ".");
        }
        System.out.println("\nWith sender Mac:");
        for (byte b : mac) {
            System.out.print(Integer.toHexString(b & 0xff) + ":");
        }
        System.out.println();
    }


    private void testARPProtocol() {
        ARPProtocolLayer arpProtocolLayer = new ARPProtocolLayer();
        registerPacketReceiver(arpProtocolLayer);
        byte[] ip;
        try {
            ip = InetAddress.getByName("192.168.1.1").getAddress();
            arpProtocolLayer.getMacByIp(ip, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}