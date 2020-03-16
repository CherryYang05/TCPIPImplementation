package utils;

import jpcap.PacketReceiver;

/**
 * @Author Cherry
 * @Date 2020/3/16
 * @Time 18:13
 * @Brief 注册接收包
 */

public interface IPacketProvider {
    public void registerPacketReceiver(PacketReceiver receiver);
}
