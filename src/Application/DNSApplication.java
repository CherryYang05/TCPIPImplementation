package Application;

import protocol.IProtocol;
import protocol.ProtocolManager;
import protocol.UDPProtocolLayer;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Random;

/**
 * @Author Cherry
 * @Date 2020/5/29
 * @Time 16:53
 * @Brief DNS 域名解析协议
 */

public class DNSApplication extends Application {
    private byte[] resolve_server_ip = null;
    private String domainName = "";
    private byte[] dnsHeader = null;
    private byte[] dnsQuestion = null;
    private short transition_id = 0;
    private static int QUESTION_TYPE_LENGTH = 2;
    private static int QUESTION_CLASS_LENGTH = 2;
    private static short QUESTION_TYPE_A = 1;
    private static short QUESTION_CLASS = 1;
    private static char DNS_SERVER_PORT = 53;

    //该类型表示服务器返回域名的类型号
    private static short DNS_ANSWER_CANONICAL_NAME_FOR_ALIAS = 5;
    private static short DNS_ANSWER_HOST_ADDRESS = 1;

    /**
     * 初始化，一般将域名交给路由器让其查询IP
     *
     * @param destIP     要查询域名的服务器IP，一般为该局域网的路由器
     * @param domainName 域名
     */
    public DNSApplication(byte[] destIP, String domainName) {
        resolve_server_ip = destIP;
        this.domainName = domainName;
        Random random = new Random();
        transition_id = (short) random.nextInt();
        //随机选择一个发送端口，这个没有具体要求
        this.port = (short) random.nextInt();
        constructDNSPacketHeader();
        constructDNSPacketQuestion();
    }

    /**
     * 组装 DNS 首部，12B
     */
    private void constructDNSPacketHeader() {
        byte[] header = new byte[12];
        ByteBuffer buffer = ByteBuffer.wrap(header);
        //会话Id,2B
        buffer.putShort(transition_id);
        //2字节操作码
        short opCode = 0;
        /*
         * 如果是查询数据包，第16个比特位要将最低位设置为0,接下来的4个比特位表示查询类型，如果是查询ip则设置为0，
         * 第11个比特位由服务器在回复数据包中设置，用于表明信息是它拥有的还是从其他服务器查询而来，
         * 第10个比特位表示消息是否有分割，有的话设置为1，由于我们使用UDP，因此消息不会有分割。
         * 第9个比特位表示是否使用递归式查询请求，我们设置成1表示使用递归式查询,
         * 第8个比特位由服务器返回时设置，表示它是否接受递归式查询
         * 第7,6,5，3个比特位必须保留为0，
         * 最后四个比特由服务器回复数据包设置，0表示正常返回数据，1表示请求数据格式错误，2表示服务器出问题，3表示不存在给定域名等等
         * 我们发送数据包时只要将第9个比特位设置成1即可(从1开始向左数第9位)
         */
        opCode = (short) (opCode | (1 << 8));
        buffer.putShort(opCode);
        //接下来是2字节的question count,由于我们只有1个请求，因此它设置成1
        short questionCount = 1;
        buffer.putShort(questionCount);
        //剩下的默认设置成0
        short answerRRCount = 0;
        buffer.putShort(answerRRCount);
        short authorityRRCount = 0;
        buffer.putShort(authorityRRCount);
        short additionalRRCount = 0;
        buffer.putShort(additionalRRCount);
        this.dnsHeader = buffer.array();
    }

    /**
     * 构造DNS数据包中包含域名的查询数据结构
     * 首先是要查询的域名，它的结构是是：字符个数+是对应字符，
     * 例如域名字符串pan.baidu.com对应的内容为
     * [3]pan[5]baidu[3]com也就是把 '.'换成它后面跟着的字母个数
     */
    private void constructDNSPacketQuestion() {
        //解析域名结构，按照 '.' 进行分解,第一个1用于记录"pan"的长度，第二个1用0表示字符串结束
        dnsQuestion = new byte[1 + 1 + domainName.length() + QUESTION_TYPE_LENGTH + QUESTION_CLASS_LENGTH];
        String[] domain = domainName.split("\\.");
        ByteBuffer buffer = ByteBuffer.wrap(dnsQuestion);
        for (String str : domain) {
            //先填写字符个数
            buffer.put((byte) str.length());
            //填写字符
            for (int i = 0; i < str.length(); ++i) {
                buffer.put((byte) str.charAt(i));
            }
        }
        //用0表示域名表示结束
        byte end = 0;
        buffer.put(end);
        //填写查询问题的类型和级别
        buffer.putShort(QUESTION_TYPE_A);
        buffer.putShort(QUESTION_CLASS);
    }


    /**
     * 向服务器发送查询请求
     */
    public void queryDomain() {
        byte[] dnsPacketBuffer = new byte[dnsHeader.length + dnsQuestion.length];
        ByteBuffer buffer = ByteBuffer.wrap(dnsPacketBuffer);
        buffer.put(dnsHeader);
        buffer.put(dnsQuestion);

        byte[] udpHeader = createUDPHeader(dnsPacketBuffer);
        byte[] ipHeader = createIP4Header(udpHeader.length);
        byte[] dnsPacket = new byte[ipHeader.length + udpHeader.length];
        buffer.clear();
        buffer = ByteBuffer.wrap(dnsPacket);
        buffer.put(ipHeader);
        buffer.put(udpHeader);
        //将消息发给路由器
        try {
            ProtocolManager.getInstance().sendData(dnsPacket, resolve_server_ip);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 组装 UDP 包头
     *
     * @param data
     * @return
     */
    private byte[] createUDPHeader(byte[] data) {
        IProtocol udpProtocol = ProtocolManager.getInstance().getProtocol("udp");
        if (udpProtocol == null) {
            return null;
        }
        HashMap<String, Object> headerInfo = new HashMap<>();
        char udpPort = (char) this.port;
        headerInfo.put("source_port", udpPort);
        headerInfo.put("dest_port", DNS_SERVER_PORT);
        headerInfo.put("data", data);
        return udpProtocol.createHeader(headerInfo);
    }

    /**
     * 组装 IP 包头
     *
     * @param length
     * @return
     */
    private byte[] createIP4Header(int length) {
        IProtocol ipPrtocol = ProtocolManager.getInstance().getProtocol("ip");
        if (ipPrtocol == null || length <= 0) {
            return null;
        }
        HashMap<String, Object> headerInfo = new HashMap<>();
        headerInfo.put("data_length", length);
        ByteBuffer destIP = ByteBuffer.wrap(resolve_server_ip);
        headerInfo.put("destination_ip", destIP.getInt());
        byte protocol = UDPProtocolLayer.PROTOCOL_UDP;
        headerInfo.put("protocol", protocol);
        headerInfo.put("identification", transition_id);
        return ipPrtocol.createHeader(headerInfo);
    }

}
