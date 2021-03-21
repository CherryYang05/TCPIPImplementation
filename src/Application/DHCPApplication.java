package Application;

import datalinklayer.DataLinkLayer;
import protocol.IProtocol;
import protocol.ProtocolManager;
import protocol.UDPProtocolLayer;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Random;

/**
 * @Author Cherry
 * @Date 2020/5/15
 * @Time 19:12
 * @Brief DHCP 协议的实现
 */

public class DHCPApplication extends Application {
    //DHCP报文的前几个固定字段
    private static byte HARDWARE_TYPE = 1;
    private static byte HARDWARE_ADDR_LENGTH = 6;
    private static byte DHCP_HOPS = 0;
    private static byte MESSAGE_TYPE_REQUEST = 1;

    //优先级
    private short secs_elapsed = 0;
    //是否全局广播
    private short bootp_flags = 0;

    //客户端IP
    private byte[] client_ip_address = new byte[4];
    //分配给你的IP(未分配之前你的IP为0.0.0.0)
    private byte[] your_ip_address = new byte[4];
    //下一个请求DHCP服务的IP
    //网关IP
    private byte[] next_server_ip_address = new byte[4];
    private byte[] relay_agent_ip_address = new byte[4];
    //当前请求的ID号
    private int transaction_id = 0;

    //魔术字,固定为0x63,0x82,0x53,0x63
    private static byte[] MAGIC_COOKIE = new byte[]{0x63, (byte) 0x82, 0x53, 0x63};

    private static byte[] dhcp_first_part;
    private static byte[] dhcp_options_part;

    //option字段(magic cookie)前的部分
    private static int DHCP_FIRST_PART_LENGTH = 236;

    //DHCP MESSAGE TPYE字段
    private static byte OPTION_MSG_TYPE_LENGTH = 3;
    private static byte OPTION_MSG_TYPE = 53;
    private static byte OPTION_MSG_DATA_LENGTH = 1;
    private static byte OPTION_MSG_TYPE_DISCOVERY = 1;

    //Parameter Request List 字段
    private static byte OPTION_PARAMETER_REQUEST_LIST = 55;
    private static byte OPTION_PARAMETER_REQUEST_LENGTH = 16;
    private static byte OPTION_PARAMETER_REQUEST_DATA_LENGTH = 14;

    //设置请求的数据类型
    private static byte OPTIONS_PARAMETER_SUBNET_MASK = 1;
    private static byte OPTIONS_PARAMETER_CLASSLESS_STATIC_ROUTER = 121;
    private static byte OPTIONS_PARAMETER_STATIC_ROUTER = 33;
    //请求路由器地址
    private static byte OPTIONS_PARAMETER_ROUTER = 3;
    //请求域名服务器
    private static byte OPTIONS_PARAMETER_DOMAIN_NAME_SERVER = 6;
    //请求子网域名
    private static byte OPTIONS_PARAMETER_DOMAIN_NAME = 15;

    private static byte OPTIONS_PARAMETER_PERFORM_ROUTER_DISCOVER = 31;
    private static byte OPTIONS_PARAMETER_VENDOR_SPECIFIC_INFORMATION = 43;
    private static byte OPTIONS_PARAMETER_NETBIOS_SERVER = 44;
    private static byte OPTIONS_PARAMETER_NETBIOS_TYPE = 46;
    private static byte OPTIONS_PARAMETER_NETBIOS_SCOPE = 47;


    //设置Requested IP Address字段
    private static byte OPTION_REQUESTED_IP_TYPE = 50;
    private static byte OPTION_REQUESTED_IP_LENGTH = 4;

    //下面的请求参数具体作用暂时忽略
    private static byte OPTIONS_PARAMETER_DOMAIN_SEARCH = 119;
    private static byte OPTIONS_PARAMETER_PROXY = (byte) 0xfc;
    private static byte OPTIONS_PARAMETER_CLASSLESS = (byte) 0xf9;
    private static byte OPTIONS_PARAMETER_LDPA = 95;
    private static byte OPTIONS_PARAMETER_IP_NAME_SERVER = 44;
    private static byte OPTIONS_PARAMETER_IP_NODE_TYPE = 46;

    private static byte OPTION_MAXIMUM_DHCP_MESSAGE_SIZE_TYPE = 57;
    private static byte OPTION_MAXIMUN_DHCP_MESSAGE_SIZE_DATA_LENGTH = 2;
    private static short OPTION_MAXIMUN_DHCP_MESSAGE_SIZE_CONTENT = 1500;
    private static byte OPTION_MAXIMUN_DHCP_MESSAGE_SIZE_LENGTH = 4;

    private static byte OPTION_CLIENT_IDENTIFIER = 61;
    private static byte OPTION_CLIENT_IDENTIFIER_DATA_LENGTH = 7;
    private static byte OPTION_CLIENT_IDENTIFIER_HARDWARE_TYPE = 0x01;
    private static byte OPTION_CLIENT_IDENTIFIER_LENGTH = 9;

    private static byte OPTION_IP_LEASE_TIME = 51;
    private static byte OPTION_IP_LEASE_TIME_DATA_LENGTH = 4;
    //租借时间是90天
    private static int OPTION_IP_LEASE_TIME_CONTENT = 7776000;
    private static byte OPTION_IP_LEASE_TIME_LENGTH = 6;

    private static byte OPTION_HOST_NAME = 12;
    private static byte[] OPTION_HOST_NAME_CONTENT = "cherryYang".getBytes();
    private static byte OPTION_HOST_NAME_DATA_LENGTH = (byte) OPTION_HOST_NAME_CONTENT.length;
    private static int OPTION_HOST_NAME_LENGTH = 2 + OPTION_HOST_NAME_CONTENT.length;

    private static byte OPTION_VENDOR_TYPE = 60;
    private static byte OPTION_VENDOR_LENGTH = 8;
    private static byte[] OPTION_VENDOR_IDENTIFIER = new byte[]{(byte) 0x4d, (byte) 0x53, (byte) 0x46,
            (byte) 0x54, (byte) 0x20, (byte) 0x35, (byte) 0x2e, (byte) 0x30};

    private static byte OPTION_END = (byte) 0xff;

    //发送和应答端口号，固定值
    private static char srcPort = 68;
    private static char dstPort = 67;

    private static byte DHCP_MSG_REPLY = 2;
    private static short DHCP_MSG_TYPE_OFFSET = 0;
    private static short DHCP_YOUR_IP_ADDRESS_OFFSET = 16;
    private static short DHCP_NEXT_IP_ADDRESS_OFFSET = 20;
    private static short DHCP_OPTIONS_OFFSET = 240;

    private static final byte DHCP_MSG_TYPE = 53;
    private static final byte DHCP_SERVER_IDENTIFER = 54;
    private static final byte DHCP_IP_ADDRESS_LEASE_TIME = 51;
    private static final byte DHCP_RENEWAL_TIME = 58;
    private static final byte DHCP_REBINDING_TIME = 59;
    private static final byte DHCP_SUBNET_MASK = 1;
    private static final byte DHCP_BROADCAST_ADDRESS = 28;
    private static final byte DHCP_ROUTER = 3;
    private static final byte DHCP_DOMAIN_NAME_SERVER = 6;
    private static final byte DHCP_DOMAIN_NAME = 15;
    private static final byte DHCP_VENDOR_CLASS = 60;


    private static byte DHCP_MSG_OFFER = 2;

    //记录来自服务器提供的IP
    private InetAddress server_supply_ip;
    private static byte OPTION_MSG_REQUEST_TYPE = 3;
    private static byte OPTION_MSG_REQUEST_LENGTH = 1;
    private static byte OPTION_REQUESTED_IP_TYPE_LENGTH = 6;

    //DHCP服务器确认类型号
    private static byte DHCP_MSG_ACK = 5;

    private final static int DHCP_STATE_DISCOVER = 0;
    private final static int DHCP_STATE_REQUESTING = 1;
    private final static int DHCP_STATE_ACK = 5;

    //该DHCP协议中维护了一个状态机
    private static int dhcp_current_state = DHCP_STATE_DISCOVER;

    public DHCPApplication() {
        Random random = new Random();
        transaction_id = random.nextInt();
        this.port = srcPort;
        constructDHCPFirstPart();
        constructDHCPOptionsPart();
    }

    /**
     * 组装 DHCP 前半部分的数据
     */
    private void constructDHCPFirstPart() {
        dhcp_first_part = new byte[DHCP_FIRST_PART_LENGTH];
        ByteBuffer buffer = ByteBuffer.wrap(dhcp_first_part);

        //设置数据包类型
        buffer.put(MESSAGE_TYPE_REQUEST);
        //设置网络类型
        buffer.put(HARDWARE_TYPE);
        //设置硬件地址长度
        buffer.put(HARDWARE_ADDR_LENGTH);
        //设置数据包跳转次数
        buffer.put(DHCP_HOPS);

        //设置会话id
        buffer.putInt(transaction_id);
        //设置等待时间
        buffer.putShort(secs_elapsed);
        //设置标志位
        buffer.putShort(bootp_flags);
        //设置设备ip
        buffer.put(client_ip_address);
        //设置租借ip
        buffer.put(your_ip_address);
        //设置下一个服务器ip
        buffer.put(next_server_ip_address);
        //设置网关ip
        buffer.put(relay_agent_ip_address);
        //设置硬件地址
        buffer.put(DataLinkLayer.getInstance().deviceMacAddress());

        //填充接下来的10个字节
        byte[] padding = new byte[10];
        buffer.put(padding);
        //设置64字节的服务器名称
        byte[] host_name = new byte[64];
        buffer.put(host_name);
        //设置128位的byte字段
        byte[] file = new byte[128];
        buffer.put(file);
    }

    /**
     * 组装 DHCP 后半部分的数据
     */
    private void constructDHCPOptionsPart() {
        //Option: (53) DHCP Message Type (Discover)
        byte[] option_msg_type = new byte[OPTION_MSG_TYPE_LENGTH];
        ByteBuffer buffer = ByteBuffer.wrap(option_msg_type);
        buffer.put(OPTION_MSG_TYPE);
        buffer.put(OPTION_MSG_DATA_LENGTH);
        buffer.put(OPTION_MSG_TYPE_DISCOVERY);

        //Option: (57) Maximum DHCP Message Size
        byte[] maximun_dhcp_msg_size = new byte[OPTION_MAXIMUN_DHCP_MESSAGE_SIZE_LENGTH];
        buffer.clear();
        buffer = ByteBuffer.wrap(maximun_dhcp_msg_size);
        buffer.put(OPTION_MAXIMUM_DHCP_MESSAGE_SIZE_TYPE);
        buffer.put(OPTION_MAXIMUN_DHCP_MESSAGE_SIZE_DATA_LENGTH);
        buffer.putShort(OPTION_MAXIMUN_DHCP_MESSAGE_SIZE_CONTENT);

        //Option: (61) Client identifier
        byte[] client_identifier = new byte[OPTION_CLIENT_IDENTIFIER_LENGTH];
        buffer.clear();
        buffer = ByteBuffer.wrap(client_identifier);
        buffer.put(OPTION_CLIENT_IDENTIFIER);
        buffer.put(OPTION_CLIENT_IDENTIFIER_DATA_LENGTH);
        buffer.put(OPTION_CLIENT_IDENTIFIER_HARDWARE_TYPE);
        //buffer.put(new byte[]{(byte)0x34, (byte)0x41, (byte)0x5d, (byte)0xed, (byte)0x28, (byte)0x0d});
        buffer.put(DataLinkLayer.getInstance().deviceMacAddress());

        System.out.println("MAC:");
        for (byte b : DataLinkLayer.getInstance().deviceMacAddress()) {
            System.out.print(b + ":");
        }
        System.out.println();
        //Option: (50) Requested IP Address (192.168.1.100)
        byte[] requestedIP = new byte[OPTION_REQUESTED_IP_LENGTH + 2];
        buffer.clear();
        buffer = ByteBuffer.wrap(requestedIP);
        buffer.put(OPTION_REQUESTED_IP_TYPE);
        buffer.put(OPTION_REQUESTED_IP_LENGTH);
        buffer.put(DataLinkLayer.getInstance().deviceIPAddress());
        System.out.println("IP:");
        for (byte b : DataLinkLayer.getInstance().deviceIPAddress()) {
            System.out.print(b + ".");
        }

        //Option: (51) ip address lease time
        byte[] ip_lease_time = new byte[OPTION_IP_LEASE_TIME_LENGTH];
        buffer.clear();
        buffer = ByteBuffer.wrap(ip_lease_time);
        buffer.put(OPTION_IP_LEASE_TIME);
        buffer.put(OPTION_IP_LEASE_TIME_DATA_LENGTH);
        buffer.putInt(OPTION_IP_LEASE_TIME_CONTENT);

        //Option: (12) Host Name
        byte[] host_name = new byte[OPTION_HOST_NAME_LENGTH];
        buffer.clear();
        buffer = ByteBuffer.wrap(host_name);
        buffer.put(OPTION_HOST_NAME);
        buffer.put(OPTION_HOST_NAME_DATA_LENGTH);
        buffer.put(OPTION_HOST_NAME_CONTENT);

        //Option: (60) Vendor class identifier
        byte[] vendor = new byte[OPTION_VENDOR_LENGTH + 2];
        buffer.clear();
        buffer = ByteBuffer.wrap(vendor);
        buffer.put(OPTION_VENDOR_TYPE);
        buffer.put(OPTION_VENDOR_LENGTH);
        buffer.put(OPTION_VENDOR_IDENTIFIER);

        //Option: (55) Parameter Request List
        byte[] parameter_request_list = new byte[OPTION_PARAMETER_REQUEST_LENGTH];
        buffer.clear();
        buffer = ByteBuffer.wrap(parameter_request_list);
        buffer.put(OPTION_PARAMETER_REQUEST_LIST);
        buffer.put(OPTION_PARAMETER_REQUEST_DATA_LENGTH);
        byte[] option_buffer = new byte[]{
                OPTIONS_PARAMETER_SUBNET_MASK,                  //1
                OPTIONS_PARAMETER_ROUTER,                       //3
                OPTIONS_PARAMETER_DOMAIN_NAME_SERVER,           //6
                OPTIONS_PARAMETER_DOMAIN_NAME,                  //15
                OPTIONS_PARAMETER_PERFORM_ROUTER_DISCOVER,      //31
                OPTIONS_PARAMETER_STATIC_ROUTER,                //33
                OPTIONS_PARAMETER_VENDOR_SPECIFIC_INFORMATION,  //43
                OPTIONS_PARAMETER_NETBIOS_SERVER,               //44
                OPTIONS_PARAMETER_NETBIOS_TYPE,                 //46
                OPTIONS_PARAMETER_NETBIOS_SCOPE,                //47
                OPTIONS_PARAMETER_DOMAIN_SEARCH,                //119
                OPTIONS_PARAMETER_CLASSLESS_STATIC_ROUTER,      //121
                OPTIONS_PARAMETER_CLASSLESS,                    //249
                OPTIONS_PARAMETER_PROXY,                        //252
                //OPTIONS_PARAMETER_LDPA,                         //95
                //OPTIONS_PARAMETER_IP_NAME_SERVER,               //44
                //OPTIONS_PARAMETER_IP_NODE_TYPE                  //46
        };
        buffer.put(option_buffer);

        //Option end
        byte[] end = new byte[1];
        end[0] = OPTION_END;
        byte[] padding = new byte[3];
        dhcp_options_part = new byte[option_msg_type.length +
                client_identifier.length +
                requestedIP.length + host_name.length +
                vendor.length + parameter_request_list.length +
                end.length + padding.length];
        buffer.clear();
        buffer = ByteBuffer.wrap(dhcp_options_part);
        buffer.put(option_msg_type);                    //53
        buffer.put(client_identifier);                  //61
        buffer.put(requestedIP);                        //50
        buffer.put(host_name);                          //12
        buffer.put(vendor);                             //60
        buffer.put(parameter_request_list);             //55
        //buffer.put(maximun_dhcp_msg_size);
        //buffer.put(ip_lease_time);
        buffer.put(end);                                //255
        buffer.put(padding);
    }

    /**
     * 组装 UDP 报文
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
        headerInfo.put("source_port", srcPort);
        headerInfo.put("dest_port", dstPort);
        headerInfo.put("data", data);
        return udpProtocol.createHeader(headerInfo);
    }

    private byte[] createIP4Header(int dataLength) {
        IProtocol ipPrtocol = ProtocolManager.getInstance().getProtocol("ip");
        if (ipPrtocol == null) {
            return null;
        }
        //创建IP包头默认情况下只需要发送数据长度,下层协议号，接收方ip地址
        HashMap<String, Object> headerInfo = new HashMap<>();
        headerInfo.put("data_length", dataLength);
        byte[] sourceIP = new byte[]{0, 0, 0, 0};
        //向全体广播
        byte[] broadCastIP = new byte[]{(byte) 255, (byte) 255, (byte) 255, (byte) 255};
        ByteBuffer srcIP = ByteBuffer.wrap(sourceIP);
        headerInfo.put("source_ip", srcIP.getInt());
        ByteBuffer dstIP = ByteBuffer.wrap(broadCastIP);
        headerInfo.put("destination_ip", dstIP.getInt());
        byte protocol = UDPProtocolLayer.PROTOCOL_UDP;
        headerInfo.put("protocol", protocol);
        headerInfo.put("identification", (short) srcPort);
        return ipPrtocol.createHeader(headerInfo);
    }

    /**
     * 组装好所有数据包，发送查询
     */
    public void dhcpDiscovery() {
        byte[] dhcpDiscoveryBuffer = new byte[dhcp_first_part.length + MAGIC_COOKIE.length
                + dhcp_options_part.length];
        ByteBuffer buffer = ByteBuffer.wrap(dhcpDiscoveryBuffer);
        buffer.put(dhcp_first_part);
        buffer.put(MAGIC_COOKIE);
        buffer.put(dhcp_options_part);

        byte[] udpHeader = createUDPHeader(dhcpDiscoveryBuffer);
        if (udpHeader == null) {
            return;
        }
        byte[] ipHeader = createIP4Header(udpHeader.length);
        byte[] dhcpPacket = new byte[udpHeader.length + ipHeader.length];
        buffer.clear();
        buffer = ByteBuffer.wrap(dhcpPacket);
        buffer.put(ipHeader);
        buffer.put(udpHeader);
        //将消息向全体广播
        ProtocolManager.getInstance().broadCast(dhcpPacket);
    }

    /**
     * 处理接收到的数据包
     *
     * @param headerInfo data
     */
    @Override
    public void handleData(HashMap<String, Object> headerInfo) {
        System.out.println("\n=============== DHCP ===============");
        System.out.println("====================================");
        byte[] data = (byte[]) headerInfo.get("data");
        boolean readSuccess = readFirstPart(data);
        if (readSuccess) {
            readOptions(data);
        }
    }

    /**
     * 处理数据包前半部分
     *
     * @param data data
     * @return
     */
    private boolean readFirstPart(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        byte reply = buffer.get(DHCP_MSG_TYPE_OFFSET);
        if (reply != DHCP_MSG_OFFER) {
            return false;
        }

        //获取分配的IP
        byte[] your_addr = new byte[4];
        buffer.position(DHCP_YOUR_IP_ADDRESS_OFFSET);
        buffer.get(your_addr, 0, your_addr.length);
        System.out.println("Available IP Offer by DHCP Server is:");
        try {
            //记录下服务器提供可用的IP
            server_supply_ip = InetAddress.getByAddress(your_addr);
            System.out.println(server_supply_ip.getHostAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }

        //获取DHCP服务器主机IP
        buffer.position(DHCP_NEXT_IP_ADDRESS_OFFSET);
        byte[] next_server_ip = new byte[4];
        buffer.get(next_server_ip, 0, next_server_ip.length);
        System.out.println("Next DHCP Server is:");
        try {
            InetAddress ser_ip = InetAddress.getByAddress(next_server_ip);
            System.out.println(ser_ip.getHostAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 处理数据包 option 部分
     * position 将直接定位到 Option 字段 (240B) 处
     *
     * @param data data
     */
    private void readOptions(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.position(DHCP_OPTIONS_OFFSET);
        while (true) {
            byte type = buffer.get();
            if (type == OPTION_END) {
                break;
            }
            switch (type) {
                case DHCP_MSG_TYPE:                 //53
                    //跳过第二个长度字段
                    buffer.get();
                    byte msg_type = buffer.get();
                    if (msg_type == DHCP_MSG_OFFER) {
                        //接收到DHCP_OFFER后，将状态转变为requesting
                        dhcp_current_state = DHCP_STATE_REQUESTING;
                        System.out.println("============ DHCP OFFER ============");
                        System.out.println("\nReceive DHCP Offer Message from Server...");
                    } else if (msg_type == DHCP_MSG_ACK) {
                        dhcp_current_state = DHCP_STATE_ACK;
                        System.out.println("============= DHCP ACK =============");
                        System.out.println("\nReceive DHCP ACK Message from Server...");
                    }
                    break;
                case DHCP_SERVER_IDENTIFER:         //54
                    //buffer.get();
                    printOptionArray("DHCP Server Identifier:", buffer);
                    break;
                case DHCP_IP_ADDRESS_LEASE_TIME:    //51
                    //越过长度字段
                    buffer.get();
                    int lease_time_secs = buffer.getInt();
                    System.out.println("The ip will lease to us for " + lease_time_secs + " seconds");
                    break;
                case DHCP_RENEWAL_TIME:             //58
                    //越过长度字段
                    buffer.get();
                    int renew_time = buffer.getInt();
                    System.out.println("We Need to Renew IP after " + renew_time + " seconds");
                    break;
                case DHCP_REBINDING_TIME:           //59
                    //越过长度字段
                    buffer.get();
                    int rebinding_time = buffer.getInt();
                    System.out.println("we need to rebinding new ip after  " + rebinding_time + " seconds");
                    break;
                case DHCP_SUBNET_MASK:              //1
                    printOptionArray("Subnet mask is : ", buffer);
                    break;
                case DHCP_BROADCAST_ADDRESS:        //28
                    printOptionArray("Broadcasting Address is : ", buffer);
                    break;
                case DHCP_ROUTER:                   //3
                    printOptionArray("Router IP Address is : ", buffer);
                    break;
                case DHCP_DOMAIN_NAME_SERVER:       //6
                    printOptionArray("Domain name server is : ", buffer);
                    break;
                case DHCP_DOMAIN_NAME:              //15
                    int len = buffer.get();
                    for (int i = 0; i < len; i++) {
                        System.out.print((char) buffer.get() + " ");
                    }
                    break;
            }
        }
        //进行状态机转化
        if (dhcp_current_state == DHCP_STATE_REQUESTING) {
            trigger_action_by_state();
        } else if (dhcp_current_state == DHCP_STATE_ACK) {
            System.out.println("=============== DHCP END ===============\n");
        }
    }

    private void printOptionArray(String content, ByteBuffer buffer) {
        System.out.println(content);
        int len = buffer.get();
        if (len == 4) {
            byte[] buf = new byte[4];
            for (int i = 0; i < len; i++) {
                buf[i] = buffer.get();
            }

            try {
                InetAddress addr = InetAddress.getByAddress(buf);
                System.out.println(addr.getHostAddress());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (len == 8) {
            byte[] buf = new byte[4];
            for (int i = 0; i < 4; ++i) {
                buf[i] = buffer.get();
            }
            try {
                InetAddress addr = InetAddress.getByAddress(buf);
                System.out.println(addr.getHostAddress());
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (int i = 0; i < 4; ++i) {
                buf[i] = buffer.get();
            }
            try {
                InetAddress addr = InetAddress.getByAddress(buf);
                System.out.println(addr.getHostAddress());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            for (int i = 0; i < len; i++) {
                System.out.print(buffer.get() + ".");
            }
        }
        System.out.println();
    }

    /**
     * 状态机转化
     */
    private void trigger_action_by_state() {
        switch (dhcp_current_state) {
            case DHCP_STATE_REQUESTING:
                dhcpRequest();
                break;
            default:
                break;
        }
    }

    /**
     * 发送 DHCP Request
     */
    private void dhcpRequest() {
        if (server_supply_ip == null) {
            return;
        }
        byte[] options = constructDHCPRequestOptions();
        byte[] dhcpDiscoverBuffer = new byte[dhcp_first_part.length + MAGIC_COOKIE.length + options.length];
        ByteBuffer buffer = ByteBuffer.wrap(dhcpDiscoverBuffer);
        buffer.put(dhcp_first_part);
        buffer.put(MAGIC_COOKIE);
        buffer.put(dhcp_options_part);
        byte[] udpHeader = createUDPHeader(dhcpDiscoverBuffer);
        byte[] ipHeader = createIP4Header(udpHeader.length);
        byte[] dhcpPacket = new byte[udpHeader.length + ipHeader.length];
        buffer = ByteBuffer.wrap(dhcpPacket);
        buffer.put(ipHeader);
        buffer.put(udpHeader);
        //将消息向全体广播
        ProtocolManager.getInstance().broadCast(dhcpPacket);
    }

    /**
     * 组装 DHCP Request Options字段
     *
     * @return
     */
    private byte[] constructDHCPRequestOptions() {
        byte[] option_msg_type = new byte[OPTION_MSG_TYPE_LENGTH];
        ByteBuffer buffer = ByteBuffer.wrap(option_msg_type);
        //Option: (53) DHCP Message Type (Discover)
        buffer.put(DHCP_MSG_TYPE);
        buffer.put(OPTION_MSG_REQUEST_LENGTH);
        buffer.put(OPTION_MSG_REQUEST_TYPE);

        //Option: (61) Client identifier
        byte[] client_identifier = new byte[OPTION_CLIENT_IDENTIFIER_LENGTH];
        buffer = ByteBuffer.wrap(client_identifier);
        buffer.put(OPTION_CLIENT_IDENTIFIER);
        buffer.put(OPTION_CLIENT_IDENTIFIER_DATA_LENGTH);
        buffer.put(OPTION_CLIENT_IDENTIFIER_HARDWARE_TYPE);
        buffer.put(DataLinkLayer.getInstance().deviceMacAddress());

        //option 57 Maximum DHCP Message Size
        byte[] maximun_dhcp_msg_size = new byte[OPTION_MAXIMUN_DHCP_MESSAGE_SIZE_LENGTH];
        buffer = ByteBuffer.wrap(maximun_dhcp_msg_size);
        buffer.put(OPTION_MAXIMUM_DHCP_MESSAGE_SIZE_TYPE);
        buffer.put(OPTION_MAXIMUN_DHCP_MESSAGE_SIZE_DATA_LENGTH);
        buffer.putShort(OPTION_MAXIMUN_DHCP_MESSAGE_SIZE_CONTENT);

        //Option: (50) Requested IP Address (192.168.1.101)
        byte[] requested_ip_addr = new byte[OPTION_REQUESTED_IP_TYPE_LENGTH +
                server_supply_ip.getAddress().length];
        buffer = ByteBuffer.wrap(requested_ip_addr);
        buffer.put(OPTION_REQUESTED_IP_TYPE);
        buffer.put(OPTION_REQUESTED_IP_LENGTH);
        buffer.put(server_supply_ip.getAddress());


        //option 51 ip address lease time
        byte[] ip_lease_time = new byte[OPTION_IP_LEASE_TIME_LENGTH];
        buffer = ByteBuffer.wrap(ip_lease_time);
        buffer.put(OPTION_IP_LEASE_TIME);
        buffer.put(OPTION_IP_LEASE_TIME_DATA_LENGTH);
        buffer.putInt(OPTION_IP_LEASE_TIME_CONTENT);

        //Option: (12) Host Name
        byte[] host_name = new byte[OPTION_HOST_NAME_LENGTH];
        buffer = ByteBuffer.wrap(host_name);
        buffer.put(OPTION_HOST_NAME);
        buffer.put(OPTION_HOST_NAME_DATA_LENGTH);
        buffer.put(OPTION_HOST_NAME_CONTENT);

        //Option: (60) Vendor class identifier
        byte[] vendor = new byte[OPTION_VENDOR_LENGTH + 2];
        buffer.clear();
        buffer = ByteBuffer.wrap(vendor);
        buffer.put(OPTION_VENDOR_TYPE);
        buffer.put(OPTION_VENDOR_LENGTH);
        buffer.put(OPTION_VENDOR_IDENTIFIER);

        //Option: (55) Parameter Request List
        byte[] parameter_request_list = new byte[OPTION_PARAMETER_REQUEST_LENGTH];
        buffer.clear();
        buffer = ByteBuffer.wrap(parameter_request_list);
        buffer.put(OPTION_PARAMETER_REQUEST_LIST);
        buffer.put(OPTION_PARAMETER_REQUEST_DATA_LENGTH);
        byte[] option_buffer = new byte[]{
                OPTIONS_PARAMETER_SUBNET_MASK,                  //1
                OPTIONS_PARAMETER_ROUTER,                       //3
                OPTIONS_PARAMETER_DOMAIN_NAME_SERVER,           //6
                OPTIONS_PARAMETER_DOMAIN_NAME,                  //15
                OPTIONS_PARAMETER_PERFORM_ROUTER_DISCOVER,      //31
                OPTIONS_PARAMETER_STATIC_ROUTER,                //33
                OPTIONS_PARAMETER_VENDOR_SPECIFIC_INFORMATION,  //43
                OPTIONS_PARAMETER_NETBIOS_SERVER,               //44
                OPTIONS_PARAMETER_NETBIOS_TYPE,                 //46
                OPTIONS_PARAMETER_NETBIOS_SCOPE,                //47
                OPTIONS_PARAMETER_DOMAIN_SEARCH,                //119
                OPTIONS_PARAMETER_CLASSLESS_STATIC_ROUTER,      //121
                OPTIONS_PARAMETER_CLASSLESS,                    //249
                OPTIONS_PARAMETER_PROXY,                        //252
                //OPTIONS_PARAMETER_LDPA,                         //95
                //OPTIONS_PARAMETER_IP_NAME_SERVER,               //44
                //OPTIONS_PARAMETER_IP_NODE_TYPE                  //46
        };
        buffer.put(option_buffer);

        //Option: (255) End
        byte[] end = new byte[1];
        end[0] = OPTION_END;
        byte[] padding = new byte[3];
        dhcp_options_part = new byte[option_msg_type.length +
                client_identifier.length +
                requested_ip_addr.length +
                host_name.length +
                vendor.length +
                parameter_request_list.length +
                maximun_dhcp_msg_size.length +
                ip_lease_time.length +
                end.length +
                padding.length
                ];

        buffer = ByteBuffer.wrap(dhcp_options_part);
        buffer.put(option_msg_type);                    //53
        buffer.put(client_identifier);                  //61
        buffer.put(requested_ip_addr);                  //50
        buffer.put(host_name);                          //12
        buffer.put(vendor);                             //60
        buffer.put(parameter_request_list);             //55
        buffer.put(maximun_dhcp_msg_size);              //57
        buffer.put(ip_lease_time);                      //51
        buffer.put(end);                                //255
        buffer.put(padding);

        return buffer.array();
    }
}
