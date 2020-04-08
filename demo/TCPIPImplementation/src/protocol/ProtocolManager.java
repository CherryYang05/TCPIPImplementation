package protocol;

import java.util.Arrays;
import java.util.HashMap;

import Application.ApplicationManager;
import Application.IApplication;
import datalinklayer.DataLinkLayer;
import jpcap.PacketReceiver;
import jpcap.packet.EthernetPacket;
import jpcap.packet.IPPacket;
import jpcap.packet.Packet;

public class ProtocolManager implements PacketReceiver{
	private static ProtocolManager instance = null;
	private static ARPProtocolLayer arpLayer = null;
	private static DataLinkLayer dataLinkInstance = null;
	private static HashMap<String , byte[] > ipToMacTable = null;
	private static HashMap<String, byte[]> dataWaitToSend = null;
	
	private static byte[] broadcast=new byte[]{(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255};
	private ProtocolManager() {}
	public static ProtocolManager getInstance() {
		if (instance == null) {
			instance = new ProtocolManager();
			dataLinkInstance = DataLinkLayer.getInstance();
			ipToMacTable = new HashMap<String, byte[]>();
			dataWaitToSend = new HashMap<String, byte[]>();
			dataLinkInstance.registerPacketReceiver(instance);
			arpLayer = new ARPProtocolLayer();
		}
		
		return instance;
	}
	
    public IProtocol getProtocol(String name) {
    	switch (name.toLowerCase()) {
    	case "icmp":
    		return new ICMPProtocolLayer();
    	case "ip":
    		return new IPProtocolLayer();
    	}
    	
    	return null;
    }
    
    public void sendData(byte[] data, byte[] ip) throws Exception {
    	/*
    	 * 发送数据前先检查给定ip的mac地址是否存在，如果没有则先让ARP协议获取mac地址
    	 */
    	byte[] mac = ipToMacTable.get(Arrays.toString(ip));
    	if (mac == null) {
    		HashMap<String, Object> headerInfo = new HashMap<String, Object>();
    		headerInfo.put("sender_ip", ip);
    		byte[] arpRequest = arpLayer.createHeader(headerInfo);
    		if (arpRequest == null) {
    			throw new Exception("Get mac adress header fail");
    		}
    		
    		dataLinkInstance.sendData(arpRequest, broadcast, EthernetPacket.ETHERTYPE_ARP);
    		//将要发送的数据存起，等待mac地址返回后再发送
    		dataWaitToSend.put(Arrays.toString(ip), data);
    	} else {
    		//如果mac地址已经存在则直接发送数据
    		dataLinkInstance.sendData(data, mac, IPPacket.IPPROTO_IP);
    	}
    }
    
	@Override
	public void receivePacket(Packet packet) {
		if (packet == null) {
			return;
		}
		
		//确保收到数据包是arp类型
		EthernetPacket etherHeader = (EthernetPacket)packet.datalink;
		/*
		 * 数据链路层在发送数据包时会添加一个802.3的以太网包头，格式如下
		 * 0-7字节：[0-6]Preamble , [7]start fo frame delimiter
		 * 8-22字节: [8-13] destination mac, [14-19]: source mac 
		 * 20-21字节: type
		 * type == 0x0806表示数据包是arp包, 0x0800表示IP包,0x8035是RARP包
		 */
		if (etherHeader.frametype == EthernetPacket.ETHERTYPE_ARP) {
			//调用ARP协议解析数据包
			ARPProtocolLayer arpLayer = new ARPProtocolLayer();
			HashMap<String, Object> info = arpLayer.handlePacket(packet);
			byte[] senderIP = (byte[])info.get("sender_ip");
			byte[] senderMac = (byte[])info.get("sender_mac");
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
		IProtocol ipProtocol = new IPProtocolLayer();
		HashMap<String, Object> info = ipProtocol.handlePacket(packet);
		if (info == null) {
			return ;
		}
		
		byte protocol = 0;
		if (info.get("protocol") != null) {
			protocol = (byte)info.get("protocol");
			//设置下一层协议的头部
			packet.header = (byte[])info.get("header");
			System.out.println("receive packet with protocol: " + protocol);
		}
		if (protocol != 0) {
			switch(protocol) {
				case IPPacket.IPPROTO_ICMP:
					handleICMPPacket(packet);
					break;
				default:
					return;
			}
					
		}
	}
	
	private void handleICMPPacket(Packet packet) {
		IProtocol icmpProtocol = new ICMPProtocolLayer();
		HashMap<String, Object> headerInfo = icmpProtocol.handlePacket(packet);
		short identifier = (short)headerInfo.get("identifier");
		IApplication app = ApplicationManager.getInstance().getApplicationByPort(identifier);
		if (app != null && app.isClosed() != true) {
			app.handleData(headerInfo);
		}
	}
		
	
	private void sendWaitingData(byte[] destIP) {
		byte[] data = dataWaitToSend.get(Arrays.toString(destIP));
		byte[] mac = ipToMacTable.get(Arrays.toString(destIP));
		if (data != null && mac != null) {
			dataLinkInstance.sendData(data, mac, EthernetPacket.ETHERTYPE_IP);
		}
	}
}
