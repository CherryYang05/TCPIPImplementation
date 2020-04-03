package utils;

import java.util.ArrayList;

import jpcap.PacketReceiver;
import jpcap.packet.Packet;

public class PacketProvider implements IPacketProvider{
    private ArrayList<PacketReceiver> receiverList = new ArrayList<PacketReceiver>();
    
	@Override
	public void registerPacketReceiver(PacketReceiver receiver) {
		if (this.receiverList.contains(receiver) != true) {
			this.receiverList.add(receiver);
		}
	}
	
	@SuppressWarnings("unused")
	protected void pushPacketToReceivers(Packet packet) {
		for (int i = 0; i < this.receiverList.size(); i++) {
			PacketReceiver receiver = (PacketReceiver) this.receiverList.get(i);
			receiver.receivePacket(packet);
		}
	}

}
