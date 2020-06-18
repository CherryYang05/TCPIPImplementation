package Application;

import protocol.IProtocol;
import protocol.ProtocolManager;
import protocol.UDPProtocolLayer;

import java.net.InetAddress;
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
     * @param data data
     * @return byte[]
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
     * @param length UDP 长度
     * @return byte[]
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

    /**
     * 解析服务器回发的数据包，首先读取头2字节判断 transition_id 是否与我们发送时使用的一致
     *
     * @param headerInfo data
     */
    @Override
    public void handleData(HashMap<String, Object> headerInfo) {
        System.out.println("\n==================== DNS START ====================");
        byte[] data = (byte[]) headerInfo.get("data");
        if (data == null) {
            System.out.println("Empty data...");
            return;
        }
        ByteBuffer buffer = ByteBuffer.wrap(data);
        short transitionID = buffer.getShort();
        if (transitionID != transition_id) {
            System.out.println("TransitionID is different!!!");
            return;
        }
        //读取2字节flag各个比特位的含义
        short flag = buffer.getShort();
        readFlags(flag);
        //下面两个字节表示请求数量(Questions)
        short questionCount = buffer.getShort();
        System.out.println("Client send " + questionCount + " requests.");
        //两字节表示服务器回复信息的数量
        short answerCount = buffer.getShort();
        System.out.println("Server return " + answerCount + " answers.");
        //两字节表示数据拥有属性信息的数量
        short authorityCount = buffer.getShort();
        System.out.println("Server return " + authorityCount + " authority resources.");
        //两字节表示附加信息的数量
        short additionalInfoCount = buffer.getShort();
        System.out.println("Server return " + additionalInfoCount + " additional info.");

        //处理回复包中的Question部分，这部分湖人查询包中的内容一模一样
        readQuestions(questionCount, buffer);
        //处理服务器返回的信息
        readAnswers(answerCount, buffer);
    }

    /**
     * 分析 flag 字段各个比特位的含义
     *
     * @param flag
     */
    private void readFlags(short flag) {
        //最高字节为1表示该数据包为回复数据包
        if ((flag & (1 << 15)) != 0) {
            System.out.println("This is packet returned from server...");
        }
        //如果第9个比特位为1表示客户端请求递归式查询
        if ((flag & (1 << 8)) != 0) {
            System.out.println("Client requests recursive query!(客户端请求递归查询)");
        }
        //第8个比特位为1表示服务器接受递归式查询请求
        if ((flag & (1 << 7)) != 0) {
            System.out.println("Server accept recursive query request!(服务器接受递归查询)");
        }
        //第6个比特位表示服务器是否拥有解析信息
        if ((flag & (1 << 5)) != 0) {
            System.out.println("Sever own the domain info!(拥有解析信息)");
        } else {
            System.out.println("Server query domain info from other servers!(无解析信息)");
        }
    }

    /**
     * 处理 Question 部分
     *
     * @param questionCount question 部分的数量
     * @param data          buffer
     */
    private void readQuestions(int questionCount, ByteBuffer data) {
        System.out.println("\n=============== Queries ===============");
        for (int i = 0; i < questionCount; i++) {
            readStringContent(data);
            System.out.println();
            //查询问题的类型
            short type = data.getShort();
            if (type == QUESTION_TYPE_A) {
                System.out.println("Request IP for given domain name");
            }
            //查询问题的级别
            short clasz = data.getShort();
            System.out.println("The class of the request is " + clasz);
        }
    }

    /**
     * 处理 Answer 部分
     * 回复信息的格式如下：
     * 第一个字段是 name，它的格式如同请求数据中的域名字符串
     * 第二个字段是类型，2字节
     * 第三字段是级别，2字节
     * 第4个字段是 Time to live, 4字节，表示该信息可以缓存多久
     * 第5个字段是数据内容长度，2字节
     * 第6个字段是内如数组，长度如同第5个字段所示
     *
     * @param answerCount 服务器返回的 answer 部分的数量
     * @param data        buffer
     */
    private void readAnswers(int answerCount, ByteBuffer data) {
        System.out.println("\n=============== Answers ===============");
        /*
         * 在读取name字段时，要注意它是否使用了压缩方式，如果是那么该字段的第一个字节就一定大于等于192，
         * 也就是它会把第一个字节的最高2比特设置成11，接下来的1字节表示数据在dns数据段中的偏移，
         * 即从DNS报文段开头开始偏移。
         * 因为规定字符串长度不能超过63，即6位，因此若发现字符串的长度超过(或等于)192，就是采用了压缩
         */
        for (int i = 0; i < answerCount; i++) {
            System.out.println(i + 1 + ": Name content in answer filed is:");
            if (isNameCompression(data.get())) {
                int offset = (int) data.get();
                byte[] array = data.array();
                ByteBuffer dup_buffer = ByteBuffer.wrap(array);
                //从指定偏移处读取
                dup_buffer.position(offset);
                readStringContent(dup_buffer);
                System.out.println();

            } else {
                readStringContent(data);
                System.out.println();
            }
            //类型
            short type = data.getShort();
            System.out.println("Answer type is : " + type);
            if (type == DNS_ANSWER_CANONICAL_NAME_FOR_ALIAS) {
                System.out.println("This answer contains server string name..." +
                        "(该答复中包含了服务器的字符串名称)");
            }
            //级别
            short clasz = data.getShort();
            System.out.println("Answer class: " + clasz);
            //接下来4个字节是TTL存活时间
            int ttl = data.getInt();
            System.out.println("This content can cache (该域名生存时间为):" + ttl + " seconds(秒)...");
            //接下来2字节表示数据长度，长度为4表示IP，其他长度为服务器字符串名称
            short length = data.getShort();
            if (type == DNS_ANSWER_CANONICAL_NAME_FOR_ALIAS) {
                readStringContent(data);
                System.out.println();
            } else if (type == DNS_ANSWER_HOST_ADDRESS) {
                //打印服务器的IP
                byte[] ip = new byte[4];
                for (int j = 0; j < 4; ++j) {
                    ip[j] = data.get();
                }
                try {
                    InetAddress inetAddress = InetAddress.getByAddress(ip);
                    System.out.println("IP for domain name is(域名解析得到的IP为): " +
                            inetAddress.getHostAddress());
                    System.out.println("==================== DNS END ====================");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println();
        }
    }

    /**
     * 解析域名字符串
     *
     * @param buffer buffer
     */
    private void readStringContent(ByteBuffer buffer) {
        byte charCount = buffer.get();
        //如果字符第一个数正确或者使用压缩方式，输出字符串内容
        while (charCount != 0 || isNameCompression(charCount)) {
            if (isNameCompression(charCount)) {
                int offset = buffer.get();
                byte[] array = buffer.array();
                ByteBuffer dup_buffer = ByteBuffer.wrap(array);
                dup_buffer.position(offset);
                readStringContent(dup_buffer);
                break;
            }
            //输出字符
            for (int i = 0; i < charCount; ++i) {
                System.out.print((char) buffer.get());
            }
            charCount = buffer.get();
            if (charCount != 0) {
                System.out.print(".");
            }
        }
    }

    /**
     * 判断字符串是否使用压缩
     * 若 7.8位 为 1，则采用压缩，因为允许的字符串长度最长为 64，即 6位
     *
     * @param b 字符串本串
     * @return
     */
    private boolean isNameCompression(byte b) {
        return (b & (1 << 7)) != 0 && (b & (1 << 6)) != 0;
    }
}
