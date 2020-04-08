
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Arrays;

import Application.PingApp;
import datalinklayer.DataLinkLayer;
import jpcap.JpcapCaptor;
import jpcap.JpcapSender;
import jpcap.NetworkInterface;
import jpcap.NetworkInterfaceAddress;
import jpcap.packet.ARPPacket;
import jpcap.packet.EthernetPacket;


public class ProtocolEntry {

    public static void main(String[] args) throws IOException {
        NetworkInterface[] devices = JpcapCaptor.getDeviceList();
        NetworkInterface device = null;
        System.out.println("there are " + devices.length + " devices");

        for (int i = 0; i < devices.length; i++) {
            boolean findDevice = false;
            for (NetworkInterfaceAddress addr : devices[i].addresses) {
                //网卡网址符合ipv4规范才是可用网卡
                if (!(addr.address instanceof Inet4Address)) {
                    continue;
                }

                findDevice = true;
                break;
            }

            if (findDevice) {
                device = devices[i];
                break;
            }

        }
        device = devices[4];
        JpcapCaptor jpcap = JpcapCaptor.openDevice(device, 2000, true, 20);
        DataLinkLayer linkLayerInstance = DataLinkLayer.getInstance();
        linkLayerInstance.initWithOpenDevice(device);

        //测试PingApp
        String ip = "192.168.1.1";
        try {
            InetAddress address = InetAddress.getByName(ip);
            PingApp pingApp = new PingApp(1, address.getAddress());
            pingApp.startPing();
        } catch (Exception e) {
            e.printStackTrace();
        }

        jpcap.loopPacket(-1, (jpcap.PacketReceiver) linkLayerInstance);

    } //main(...)
}
