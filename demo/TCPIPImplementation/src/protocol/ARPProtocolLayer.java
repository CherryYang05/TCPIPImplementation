package protocol;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.util.HashMap;

import datalinklayer.DataLinkLayer;

import jpcap.packet.ARPPacket;
import jpcap.packet.EthernetPacket;

import jpcap.packet.Packet;


public class ARPProtocolLayer implements IProtocol{
    /*
     * 数据包含数据链路层包头:dest_mac(6byte) + source_mac(6byte) + frame_type(2byte)
     * 因此读取ARP数据时需要跳过开头14字节
     */
    private static int ARP_OPCODE_START = 20;
    private static int ARP_SENDER_MAC_START = 22;
    private static int ARP_SENDER_IP_START = 28;
    private static int ARP_TARGET_IP_START = 38;
    
	
	private  boolean analyzeARPMessage(byte[] data, HashMap<String, Object> infoTable) {
		/*
		 * 解析获得的APR消息包，从中获得各项信息，此处默认返回的mac地址长度都是6
		 */
		//先读取2,3字节，获取消息操作码，确定它是ARP回复信息
		byte[] opcode = new byte[2];
		System.arraycopy(data, ARP_OPCODE_START, opcode, 0, 2);
		//转换为小端字节序
		short op = ByteBuffer.wrap(opcode).getShort();
		if (op != ARPPacket.ARP_REPLY) {
			return false;
		}
		
		//获取接受者ip，确定该数据包是回复给我们的
		byte[] ip = DataLinkLayer.getInstance().deviceIPAddress();
		for (int i = 0; i < 4; i++) {
			if (ip[i] != data[ARP_TARGET_IP_START + i]) {
				return false;
			}
		}
		
		//获取发送者IP
		byte[] senderIP = new byte[4];
		System.arraycopy(data, ARP_SENDER_IP_START, senderIP, 0, 4);
		//获取发送者mac地址
		byte[] senderMac = new byte[6];
		System.arraycopy(data, ARP_SENDER_MAC_START, senderMac, 0, 6);
		
		infoTable.put("sender_mac", senderMac);
		infoTable.put("sender_ip", senderIP);
 		return true;
	}
    
    private byte[] makeARPRequestMsg(byte[] ip) {
    	if (ip == null) {
    		return null;
    	}
    	
    	DataLinkLayer dataLinkInstance = DataLinkLayer.getInstance();
    	byte[] broadcast=new byte[]{(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255};
		int pointer = 0;
		byte[] data = new byte[28];
		//注意将字节序转换为大端 设置hardware type 字段
		ByteBuffer buffer = ByteBuffer.allocate(2);
		buffer.order(ByteOrder.BIG_ENDIAN);
		buffer.putShort(ARPPacket.HARDTYPE_ETHER);
		for (int i = 0; i < buffer.array().length; i++) {
			data[pointer] = buffer.array()[i];
			pointer++;
		}
		//注意将字节序转换为大端 设置protocol type 字段
		buffer = ByteBuffer.allocate(2);
		buffer.order(ByteOrder.BIG_ENDIAN);
		buffer.putShort(ARPPacket.PROTOTYPE_IP);
		for (int i = 0; i < buffer.array().length; i++) {
			data[pointer] = buffer.array()[i];
			pointer++;
		}
	
		data[pointer] = 6;
		pointer++;
		data[pointer] = 4;
		pointer++;
		//注意将字节序转换为大端
		buffer = ByteBuffer.allocate(2);
		buffer.order(ByteOrder.BIG_ENDIAN);
		buffer.putShort(ARPPacket.ARP_REQUEST);
		for (int i = 0; i < buffer.array().length; i++) {
			data[pointer] = buffer.array()[i];
			pointer++;
		}
		
		byte[] macAddress = dataLinkInstance.deviceMacAddress();
		for (int i = 0; i < macAddress.length; i++) {
			data[pointer] = macAddress[i];
			pointer++;
		}
		
		byte[] srcip = dataLinkInstance.deviceIPAddress();
		for (int i = 0; i < srcip.length; i++) {
			data[pointer] = srcip[i];
			pointer++;
		}
		for (int i = 0; i < broadcast.length; i++) {
			data[pointer] = broadcast[i];
			pointer++;
		}
		for (int i = 0; i < ip.length; i++) {
			data[pointer] = ip[i];
			pointer++;
		}
		
		return data;
    }

	@Override
	public byte[] createHeader(HashMap<String, Object> headerInfo) {
		byte[] ip = (byte[])headerInfo.get("sender_ip");
		if (ip == null) {
			return null;
		}
		
		byte[] header = makeARPRequestMsg(ip);
		return header;
	}

	@Override
	public HashMap<String, Object> handlePacket(Packet packet) {
		byte[] header = packet.header;
		HashMap<String, Object> infoTable = new HashMap<String, Object>();
		analyzeARPMessage(header, infoTable);
		return infoTable;
	}
}
