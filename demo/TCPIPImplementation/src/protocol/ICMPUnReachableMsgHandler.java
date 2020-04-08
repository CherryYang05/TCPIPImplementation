package protocol;

import java.nio.ByteBuffer;

import jpcap.packet.IPPacket;

public class ICMPUnReachableMsgHandler implements IICMPErrorMsgHandler {
	private static int ICMP_UNREACHABLE_TYPE = 3;
	private static int IP_HEADER_LENGTH = 20;
	
	enum ICMP_ERROR_MSG_CODE {
	  ICMP_NETWORK_UNREACHABLE,
	  ICMP_HOST_UNREACHABLE,
	  ICMP_PROTOCAL_UNREACHABLE,
	  ICMP_PORT_UNREACHABLE,
	  ICMP_FRAGMETATION_NEEDED_AND_DF_SET
	  //后面需要在继续添加其他类型
	} ;
	@Override
	public boolean handleICMPErrorMsg(int type, int code, byte[] data) {
		if (type != ICMPUnReachableMsgHandler.ICMP_UNREACHABLE_TYPE) {
			return false;	
		}
	
		ByteBuffer buffer = ByteBuffer.wrap(data);
		
		switch (ICMP_ERROR_MSG_CODE.values()[code]) {
		case ICMP_PORT_UNREACHABLE:
			//错误数据格式:IP包头和8字节内容
			//获取协议类型
			byte protocol = buffer.get(9);
			if (protocol == IPPacket.IPPROTO_UDP) {
				handleUDPError(buffer);
			}
			break;
	    default:
	    	return false;
		}
		
		return true;
	}
	
	private void handleUDPError(ByteBuffer buffer) {
		System.out.println("protocol of error packet is UDP");
		System.out.println("Source IP Address is :");
		int source_ip_offset = 12;
		for (int i = 0; i < 4; i++) {
			int v = buffer.get(source_ip_offset + i) & 0xff;
			System.out.print(v + ".");
		}
		System.out.println("\nDest IP Address is :");
		int dest_ip_offset = 16;
		for (int i = 0; i < 4; i++) {
			int v = buffer.get(dest_ip_offset + i) & 0xff;
			System.out.print(v + ".");
		}
		
		/*
		 * 打印UDP数据包头前8个字节信息，其格式为：
		 * source_port(2 byte),
		 * dest_port (2byte)
		 * length (2byte)
		 * check_sum(2byte)
		 */
		int source_port = (int) (buffer.getShort(IP_HEADER_LENGTH) & 0xFFFF);
		System.out.println("\nSource Port: " + source_port);
		int source_port_len = 2;
		int dest_port = (int) (buffer.getShort(IP_HEADER_LENGTH + source_port_len) & 0xFFFF);
		System.out.println("dest port: " + dest_port);
	}

}
