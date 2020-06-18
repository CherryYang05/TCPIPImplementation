import Application.TFTPClient;
import datalinklayer.DataLinkLayer;
import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;
import jpcap.NetworkInterfaceAddress;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;

/**
 * @Author Cherry
 * @Date 2020/3/14
 * @Time 21:28
 * @Brief 使用 JPCAP 获取网卡
 */

public class ProtocolEntry {

    //public void receivePacket(Packet packet) {
    //    System.out.println(packet);
    //    System.out.println("Receive a packet");
    //}

    private static void showNetWorkCard(NetworkInterface[] devices) throws IOException {

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

    public static void main(String[] args) throws Exception {
        //获取网卡列表
        NetworkInterface[] devices = JpcapCaptor.getDeviceList();
        NetworkInterface device = null;

        //显示所有网卡
        showNetWorkCard(devices);

        System.out.println("There are " + devices.length + " devices.");

        for (int i = devices.length - 1; i >= 0; i--) {
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

        //我的电脑是 4 号网卡为硬件网卡
        //device = devices[5];

        System.out.println("Open device: " + device.name + "\n");

        JpcapCaptor jpcap = JpcapCaptor.openDevice(device, 2000, true, 20);

        DataLinkLayer linkLayerInstance = DataLinkLayer.getInstance();
        linkLayerInstance.initWithOpenDevice(device);

        //测试PING APP 和 HPing App
        String ip = "192.168.1.1";
        //traceroute 百度
        //String ip = "112.80.248.75";
        try {
            //InetAddress address = InetAddress.getByName(ip);

            //测试Ping和HPing
            //PingApp pingApp = new PingApp(1, address.getAddress());
            //PingApp pingApp = new HPingApp(1, address.getAddress());
            //pingApp.startPing();

            //测试TraceRoute
            //TraceRoute traceRoute = new TraceRoute(address.getAddress());
            //traceRoute.startTraceRoute();

            //测试DHCP
            //DHCPApplication dhcpApp = new DHCPApplication();
            //dhcpApp.dhcpDiscovery();

            //测试DNS
            //DNSApplication dnsApplication = new DNSApplication(address.getAddress(), "pan.baidu.com");
            //dnsApplication.queryDomain();

            //测试TFTP
            InetAddress address = InetAddress.getByName("192.168.1.150");
            TFTPClient tftpClient = new TFTPClient(address.getAddress());
            tftpClient.getFile("file.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }

        jpcap.loopPacket(-1, (jpcap.PacketReceiver) linkLayerInstance);
    }
}
