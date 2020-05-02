package Application;

import java.nio.ByteBuffer;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;

import javax.xml.crypto.Data;

import protocol.IProtocol;
import protocol.ProtocolManager;

public class HPingApp extends PingApp{
    private int send_time = 0;
    
	public HPingApp(int times, byte[] destIP) {
		super(times, destIP);
	}
	
	protected byte[] createICMPHeader() {
		IProtocol icmpProto = ProtocolManager.getInstance().getProtocol("icmp");
		if (icmpProto == null) {
			return null;
		}
		//构造icmp echo 包头
		HashMap<String, Object> headerInfo = new HashMap<String, Object>();
		headerInfo.put("header", "timestamp");
		headerInfo.put("identifier", identifier);
		headerInfo.put("sequence_number", sequence);
		sequence++;
		//获取UTC时间
		Calendar rightNow = Calendar.getInstance();
		int hour = rightNow.get(Calendar.HOUR_OF_DAY);
		int minutes = rightNow.get(Calendar.MINUTE);
		int secs = rightNow.get(Calendar.SECOND);
		
		send_time = (hour * 3600 + minutes * 60 + secs) * 1000;
	   
        headerInfo.put("original_time", send_time);
        int receive_time = 0, transmit_time = 0;
        headerInfo.put("receive_time", receive_time);
        headerInfo.put("transmit_time", transmit_time);
        
		byte[] icmpEchoHeader = icmpProto.createHeader(headerInfo);
		
		return icmpEchoHeader;
	}
	
	public void handleData(HashMap<String, Object> data) {
		short sequence = (short)data.get("sequence");
		int receive_time = (int)data.get("receive_time");
		System.out.println("receive time  for timestamp request " + sequence + "for  " + (send_time - receive_time) / 1000 + "secs");
		
		int transmit_time = (int)data.get("transmit_time");
		System.out.println("receive reply for ping request " + sequence + "for  " + (send_time - transmit_time) / 1000 + "secs");
	}
}
