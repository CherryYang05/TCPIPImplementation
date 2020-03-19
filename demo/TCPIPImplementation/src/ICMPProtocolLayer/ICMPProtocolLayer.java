package ICMPProtocolLayer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import jpcap.PacketReceiver;
import jpcap.packet.EthernetPacket;
import jpcap.packet.Packet;

public class ICMPProtocolLayer implements PacketReceiver{
    private static byte PROTOCL_ICMP = 1;
    public static int ICMP_DATA_OFFSET = 20 + 14; //越过20字节IP包头
    private static int PROTOCOL_FIELD_IN_IP_HEADER = 14 + 9;
    
    private ArrayList<IICMPErrorMsgHandler> error_handler_list = new ArrayList<IICMPErrorMsgHandler>();
    private enum ICMP_MSG_TYPE {
    	ICMP_UNKNOW_MSG,
    	ICMP_ERROR_MSG,
    	ICMP_INFO_MSG
    };
    
    private int icmp_type  = 0;
    private int icmp_code = 0;
    private byte[] packet_header = null;
    private byte[] packet_data = null;
    
    public ICMPProtocolLayer() {
    	//添加错误消息处理对象
    	error_handler_list.add(new ICMPUnReachableMsgHandler());
    }
    
	@Override
	public void receivePacket(Packet packet) {
		if (packet == null) {
			return;
		}
		
		//确保收到数据包是IP类型
		EthernetPacket etherHeader = (EthernetPacket)packet.datalink;
		if (etherHeader.frametype != EthernetPacket.ETHERTYPE_IP) {
			return;
		}
	
		//读取IP包头，也就是接在数据链路层后面的20字节，读取协议字段是否表示ICMP，也就是偏移第10个字节的值为1
		if (packet.header[PROTOCOL_FIELD_IN_IP_HEADER] != PROTOCL_ICMP) {
			return;
		}
		
		packet_header = Arrays.copyOfRange(packet.header, ICMP_DATA_OFFSET, packet.header.length);
		packet_data = packet.data;
		
		analyzeICMPMessage(packet_header);
	}
	
	private ICMP_MSG_TYPE  checkType(int type) {
		/*
		 * 传递错误消息的ICMP数据报type处于0到127，
		 * 传递控制信息的ICMP数据报type处于128到255
		 */
		if (type >= 0 && type <= 127) {
			return ICMP_MSG_TYPE.ICMP_ERROR_MSG;
		}
		
		if (type >= 128 && type <= 255) {
			return ICMP_MSG_TYPE.ICMP_INFO_MSG;
		}
		
		return ICMP_MSG_TYPE.ICMP_UNKNOW_MSG;
	}
	
	private void analyzeICMPMessage(byte[] data) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(data);
		icmp_type = byteBuffer.get(0);
		icmp_code = byteBuffer.get(1);
	
		//检测当前数据包是错误信息还是控制信息,并根据不同情况分别处理
		if (checkType(icmp_type) == ICMP_MSG_TYPE.ICMP_ERROR_MSG) {
			handleICMPErrorMsg(packet_data);
		}
	}
	
	private void handleICMPErrorMsg(byte[] data) {
		/*
		 * ICMP错误数据报的类型很多，一个type和code的组合就能对应一种数据类型，我们不能把对所有数据类型的处理全部
		 * 塞入一个函数，那样会造成很多个if..else分支，使得代码复杂，膨胀，极难维护，因此我们使用责任链模式来处理
		 */
		for (int i = 0; i < error_handler_list.size(); i++) {
			if (error_handler_list.get(i).handleICMPErrorMsg(icmp_type, icmp_code, data) == true) {
				break;
			}
		}
	}

}
