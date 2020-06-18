package Application;

import protocol.IProtocol;
import protocol.ProtocolManager;
import protocol.UDPProtocolLayer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * @Author Cherry
 * @Date 2020/6/17
 * @Time 18:54
 * @Brief TFTP 客户端，实现 TFTP 简单文件传输协议
 * 其中 TFTP 包含三种包，首先是 Read Request 请求包，向服务器请求数据
 * Data 包，用来传输数据，然后是 ACK 包，用来确认整个文件传输完成
 */

public class TFTPClient extends Application {
    private byte[] server_ip = null;                    //请求的服务器IP
    private static short OPTION_CODE_READ = 1;          //读请求操作码
    private static short OPTION_CODE_WRITE = 2;         //写请求操作码
    private static short OPTION_CODE_ACK = 4;           //应答ACK
    private static final short OPTION_CODE_DATA = 3;    //数据块
    private static final short OPTION_CODE_ERROR = 5;   //错误消息
    private static short TFTP_ERROR_FILE_NOT_FOUND = 1; //未找到文件错误类型码

    private static short OPTION_CODE_LENGTH = 2;//操作码字段占2B
    private short data_block = 1;               //数据块编号从1开始
    private static char TFTP_SERVER_PORT = 69;  //TFTP协议的目的端口
    private static char server_port = 0;        //TFTP回复的时候选择一个不同于69号端口的其他端口，为了能够同时接受多个客户端的请求
    private File download_file = null;          //下载的文件
    private String filename;                    //下载的文件的文件名
    FileOutputStream download_file_stream = null;//下载文件的文件流

    /**
     * 构造器，传入服务器 IP
     *
     * @param server_ip 要获取文件的 TFTP 服务器 IP
     */
    public TFTPClient(byte[] server_ip) {
        this.server_ip = server_ip;
        //随机指定一个源端口
        this.port = (short) 60706;
        //目的端口设为69
        server_port = TFTP_SERVER_PORT;
    }

    /**
     * 先在本地生成一个要下载的文件的空文件流，等待从服务器读取数据块之后写入
     *
     * @param filename 要下载的文件名
     */
    public void getFile(String filename) {
        download_file = new File(filename);
        this.filename = filename;
        try {
            download_file_stream = new FileOutputStream(download_file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        sendReadPacket();
    }

    /**
     * 向服务器发送读文件请求包,格式包括：
     * 2B 的 OpCode，n字节的文件名，最后以 0 结尾，
     * 然后是类型 netascii(也是以 0 结尾)
     * 然后封装 IP 包头和 UDP 包头，发送数据包
     */
    private void sendReadPacket() {
        String mode = "netascii";
        byte[] read_request = new byte[OPTION_CODE_LENGTH + this.filename.length() + 1
                + mode.length() + 1];
        ByteBuffer buffer = ByteBuffer.wrap(read_request);
        buffer.putShort(OPTION_CODE_READ);
        buffer.put(this.filename.getBytes());
        buffer.put((byte) 0);
        buffer.put(mode.getBytes());
        buffer.put((byte) 0);
        byte[] udp_header = createUDPHeader(read_request);
        byte[] ip_header = createIPHeader(udp_header.length);

        byte[] readRequestPacket = new byte[udp_header.length + ip_header.length];
        buffer.clear();
        buffer = ByteBuffer.wrap(readRequestPacket);
        buffer.put(ip_header);
        buffer.put(udp_header);
        try {
            ProtocolManager.getInstance().sendData(readRequestPacket, server_ip);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 老生常谈了，构造 UDP 报头
     *
     * @param data UDP 数据，即 TFTP 报头
     * @return UDP 报头
     */
    private byte[] createUDPHeader(byte[] data) {
        IProtocol udpPro = ProtocolManager.getInstance().getProtocol("udp");
        if (udpPro == null) {
            return null;
        }
        HashMap<String, Object> headerInfo = new HashMap<>();
        headerInfo.put("source_port", (char) this.port);//char转换不能少
        headerInfo.put("dest_port", server_port);
        headerInfo.put("data", data);
        return udpPro.createHeader(headerInfo);
    }

    /**
     * 构造 IP 报头
     *
     * @param length UDP 报头长度
     * @return IP 报头
     */
    private byte[] createIPHeader(int length) {
        IProtocol ipPro = ProtocolManager.getInstance().getProtocol("ip");
        if (ipPro == null) {
            return null;
        }
        //创建IP报头默认情况下只需要发送数据长度，下层协议号，接收方IP地址
        HashMap<String, Object> headerInfo = new HashMap<>();
        headerInfo.put("data_length", length);      //UDP 报文部分
        ByteBuffer dest_ip = ByteBuffer.wrap(server_ip);
        headerInfo.put("destination_ip", dest_ip.getInt());
        //假装是别的IP发送数据包，否则本机向本机进行TFTP请求不会发送数据包
        try {
            InetAddress fake_ip = InetAddress.getByName("192.168.1.101");
            ByteBuffer buf = ByteBuffer.wrap(fake_ip.getAddress());
            headerInfo.put("source_ip", buf.getInt());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        byte protocol = UDPProtocolLayer.PROTOCOL_UDP;
        headerInfo.put("protocol", protocol);
        return ipPro.createHeader(headerInfo);
    }

    /**
     * 处理服务器返回来的 Data 数据包
     *
     * @param headerInfo Data数据包
     */
    @Override
    public void handleData(HashMap<String, Object> headerInfo) {
        byte[] data = (byte[]) headerInfo.get("data");
        if (data == null) {
            System.out.println("数据包为空!!!");
            return;
        }
        short port = (short) headerInfo.get("src_port");
        server_port = (char) port;
        ByteBuffer buffer = ByteBuffer.wrap(data);
        short opCode = buffer.getShort();
        switch (opCode) {
            case OPTION_CODE_DATA:
                //处理正确消息
                handleDataPacket(buffer);
                break;
            case OPTION_CODE_ERROR:
                //处理错误消息
                handleErrorPacket(buffer);
                break;
            default:
                break;
        }
    }

    /**
     * 处理正确的 Data 数据包
     *
     * @param buffer TFTP 报头
     */
    private void handleDataPacket(ByteBuffer buffer) {
        //获取数据块的编号
        data_block = buffer.getShort();
        System.out.println("Receive data block num: " + data_block);
        byte[] data = buffer.array();
        int left_len = data.length - buffer.position();
        //将数据写入文件
        byte[] file_content = new byte[left_len];
        buffer.get(file_content);
        try {
            download_file_stream.write(file_content);
            System.out.println("[*] Write data block " + data_block + " to file...");
        } catch (IOException e) {
            e.printStackTrace();
        }

        //如果剩余数据长度等于512，说明后面还有数据块，若长度小于512，说明是最后一个数据块,则关闭输出流
        if (left_len == 512) {
            sendACKPacket();
            data_block++;
        } else if (left_len < 512) {
            sendACKPacket();
            try {
                download_file_stream.close();   //关闭文件输出流
            } catch (IOException e) {
                e.printStackTrace();
            }
            data_block = 1;
        }
    }

    /**
     * 处理错误的 Data 数据包
     *
     * @param buffer TFTP 报头
     */
    private void handleErrorPacket(ByteBuffer buffer) {
        short error_code = buffer.getShort();
        if (error_code == TFTP_ERROR_FILE_NOT_FOUND) {
            System.out.println("TFTP server return file not found packet!!!");
        }
        byte[] data = buffer.array();
        //计算剩余数据的长度
        int left_len = data.length - buffer.position();
        byte[] error_msg = new byte[left_len];
        //将缓冲区当前position至最后的所有内容存到error_msg中，长度必须一致
        buffer.get(error_msg);
        String err_context = new String(error_msg);
        System.out.println("Error msg from server is: " + err_context);
    }

    /**
     * 每成功接收一个数据块便发送一个 ACK 数据包
     */
    private void sendACKPacket() {
        byte[] ack_msg = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(ack_msg);
        buffer.putShort(OPTION_CODE_ACK);
        buffer.putShort(data_block);
        byte[] udpHeader = createUDPHeader(ack_msg);
        byte[] ipHeader = createIPHeader(udpHeader.length);
        byte[] ack_packet = new byte[udpHeader.length + ipHeader.length];
        buffer = ByteBuffer.wrap(ack_packet);
        buffer.put(ipHeader);
        buffer.put(udpHeader);
        try {
            ProtocolManager.getInstance().sendData(ack_packet, server_ip);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
