package protocol;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import jpcap.PacketReceiver;
import jpcap.packet.EthernetPacket;
import jpcap.packet.Packet;

public class ICMPProtocolLayer implements IProtocol{
    public static byte PROTOCL_ICMP = 1;   
    private ArrayList<IProtocol> protocol_header_list = new ArrayList<IProtocol>();
    private Packet packet;
    
    public ICMPProtocolLayer() {
    	//增加icmp echo 协议包头创建对象
    	protocol_header_list.add(new ICMPEchoHeader());
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

}
