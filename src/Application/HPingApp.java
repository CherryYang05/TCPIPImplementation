package Application;

import protocol.IProtocol;
import protocol.ProtocolManager;

import java.util.Calendar;
import java.util.HashMap;

/**
 * @Author Cherry
 * @Date 2020/4/13
 * @Time 21:09
 * @Brief ICMP timestamp 协议
 */

public class HPingApp extends PingApp {
    private int send_time = 0;

    /**
     * @param times  连续发送多少次数据包
     * @param destIP HPing 的对象
     */
    public HPingApp(int times, byte[] destIP) {
        super(times, destIP);
    }


    /**
     * 重写 PingApp 中的创建 ICMP 首部函数
     *
     * @return
     */
    protected byte[] createICMPHeader() {
        //得到 ICMPProtocolLayer 对象
        IProtocol icmpProtocol = ProtocolManager.getInstance().getProtocol("icmp");
        if (icmpProtocol == null) return null;
        //构造 icmp 首部
        HashMap<String, Object> headerInfo = new HashMap<>();
        headerInfo.put("header", "timestamp");
        headerInfo.put("identifier", identifier);
        headerInfo.put("sequence_num", sequence);
        sequence++;
        //获取 UTC 时间
        Calendar time = Calendar.getInstance();
        //24时制
        int hour = time.get(Calendar.HOUR_OF_DAY);
        int minute = time.get(Calendar.MINUTE);
        int second = time.get(Calendar.SECOND);

        send_time = (hour * 3600 + minute * 60 + second) * 1000;

        headerInfo.put("original_time", send_time);
        int receive_time = 0;
        int transmit_time = 0;
        headerInfo.put("receive_time", receive_time);
        headerInfo.put("transmit_time", transmit_time);
        return icmpProtocol.createHeader(headerInfo);
    }

    /**
     * 处理传送回来的包
     * 这里的时间戳不可靠，没有时钟同步，在 NTP 中会有进一步的介绍
     * @param data
     */
    public void handleData(HashMap<String, Object> data) {
        short sequence = (short) data.get("sequence");
        int receive_time = (int) data.get("receive_time");
        System.out.println("Receive time for timestamp request " + sequence +
                " for " + (send_time - receive_time) / 1000 + " s");
        int transmit_time = (int) data.get("transmit_time");
        System.out.println("Receive reply for ping request " + sequence +
                " for " + (send_time - transmit_time) / 1000 + " s\n");
    }
}
