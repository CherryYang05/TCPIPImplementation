package protocol;

import jpcap.packet.Packet;

import java.util.HashMap;

/**
 * @Author Cherry
 * @Date 2020/4/8
 * @Time 12:45
 * @Brief 实现网络协议的模块单独形成一个独立部分，实现具体网络协议的对象继承统一的接口
 */

public interface IProtocol {
    byte[] createHeader(HashMap<String, Object> headerInfo);

    HashMap<String, Object> handlePacket(Packet packet);
}
