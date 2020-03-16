package utils;

import jpcap.PacketReceiver;
import jpcap.packet.Packet;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author Cherry
 * @Date 2020/3/16
 * @Time 18:08
 * @Brief 观察者模式的实现，
 * 所有想获取数据包的对象都要通过 PacketProvider 注册，
 * 收到数据后，将数据包推送给所有观察者
 */

public class PacketProvider implements IPacketProvider {

    private List<PacketReceiver> receiverList = new ArrayList<>();

    @Override
    public void registerPacketReceiver(PacketReceiver receiver) {
        if (!receiverList.contains(receiver)) {
            receiverList.add(receiver);
        }
    }

    protected void pushPacketToReceivers(Packet packet) {
        for (PacketReceiver receiver : receiverList) {
            receiver.receivePacket(packet);
        }
    }
}
