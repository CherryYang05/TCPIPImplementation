/**
 * @Author Cherry
 * @Date 2020/3/14
 * @Time 22:57
 * @Brief
 */
package TCPIPImplementation;

import jpcap.packet.*;

public class DataLinkLayer implements jpcap.PacketReceiver {
    public String[] protocoll = {"HOPOPT", "ICMP", "IGMP", "GGP", "IPV4", "ST", "TCP", "CBT", "EGP", "IGP",
            "BBN", "NV2", "PUP", "ARGUS", "EMCON", "XNET", "CHAOS", "UDP", "mux"};

    @Override
    public void receivePacket(Packet packet) {
        boolean show_tcp = false;
        boolean show_icmp = true;
        boolean show_udp = false;

        IPPacket tpt = (IPPacket) packet;
        if (packet != null) {
            String protocol = protocoll[tpt.protocol];

            if (protocol.equals(("TCP")) && show_tcp) {
                System.out.println("\n==== This is TCP packet ====");
                TCPPacket tp = (TCPPacket) packet;
                System.out.println("This is destination port of tcp: " + tp.dst_port);

                if (tp.ack) {
                    System.out.println("\n" + "This is an acknowledgement.");
                } else {
                    System.out.println("This is not an acknowledgment packet.");
                }

                if (tp.rst) {
                    System.out.println("Reset connection...");
                }
                System.out.println("Protocol version is: " + tp.version);
                System.out.println("This is destination ip: " + tp.dst_ip);
                System.out.println("This is source ip: " + tp.src_ip);

                if (tp.fin) {
                    System.out.println("Sender does not have more data to transfer");
                }
                if (tp.syn) {
                    System.out.println("\n Request for connection...");
                }
            } else if (protocol.equals("ICMP") && show_icmp) {
                ICMPPacket ipc = (ICMPPacket) packet;
                System.out.println("\n ====This ICMP Packet =====");
                System.out.println("This is alive time: " + ipc.alive_time);
                System.out.println("Number of advertised address: " + (int) ipc.addr_num);
                System.out.println("MTU of the packet is: " + (int) ipc.mtu);
                System.out.println("Subnet mask: " + ipc.subnetmask);
                System.out.println("Source ip: " + ipc.src_ip);
                System.out.println("Destination ip: " + ipc.dst_ip);
                System.out.println("Check sum: " + ipc.checksum);
                System.out.println("ICMP type: " + ipc.type);
                System.out.println();
            } else if (protocol.equals("UDP") && show_udp) {
                UDPPacket pac = (UDPPacket) packet;
                System.out.println("\n==== This is UDP packet ====");
                System.out.println("This is source port: " + pac.src_port);
                System.out.println("This is destination port: " + pac.dst_port);
                System.out.println();
            }
        } else {
            System.out.println("Dft bi is not set. Packet will be fragmented... \n");
        }
    }
}
