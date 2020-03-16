package TCPIPImplementation;

import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;
import jpcap.NetworkInterfaceAddress;
import jpcap.packet.Packet;

import java.io.IOException;
import java.net.Inet4Address;

/**
 * @Author Cherry
 * @Date 2020/3/14
 * @Time 21:28
 * @Brief 使用 JPCAP 获取网卡
 */

public class ProtocolEntry {
    public void receivePacket(Packet packet) {
        System.out.println(packet);
        System.out.println("Receive a packet");
    }

    public static void showNetWorkCard(NetworkInterface[] devices) throws IOException {

        JpcapCaptor captor = null;
        for (int i = 0; i < devices.length; i++) {
            //显示网卡名字
            System.out.println(i + ": " + devices[i].name + "(" + devices[i].description + ")");
            System.out.println(" datalink: " + devices[i].datalink_name + "(" + devices[i].datalink_description + ")");
            System.out.print(" Windows Address: ");
            for (byte b : devices[i].mac_address) {
                System.out.print(Integer.toHexString(b & 0xff) + ":");
            }
            System.out.println();

            for (NetworkInterfaceAddress net : devices[i].addresses) {
                System.out.println(" address: " + net.address + " " + net.subnet + "　" + net.broadcast);
            }

            captor = JpcapCaptor.openDevice(devices[i], 65536, false, 20);
            if (captor != null) {
                System.out.println("Open captor on device " + i);
                System.out.println();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        //获取网卡列表
        NetworkInterface[] devices = JpcapCaptor.getDeviceList();
        NetworkInterface device = null;

        //显示所有网卡
        //showNetWorkCard(devices);

        System.out.println("There are " + devices.length + " devices.");

        for (int i = 0; i < devices.length; i++) {
            boolean isFindDevice = false;

            for (NetworkInterfaceAddress net : devices[i].addresses) {
                //当网卡地址符合Ipv4规范才是可用网卡
                if (net.address instanceof Inet4Address) {
                    isFindDevice = true;
                    break;
                }
            }

            if (isFindDevice) {
                device = devices[i];
                break;
            }
        }

        //我的电脑是 5 号网卡为硬件网卡
        device = devices[5];

        System.out.println("Open device: " + device.name);

        JpcapCaptor jpcap = JpcapCaptor.openDevice(device, 2000, true, 20);

        jpcap.loopPacket(-1, (jpcap.PacketReceiver) new DataLinkLayer());
    }
}
