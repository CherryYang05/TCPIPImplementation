package protocol;

import java.util.HashMap;

import jpcap.packet.Packet;

public interface IProtocol {
    public byte[] createHeader(HashMap<String, Object> headerInfo);
    public HashMap<String, Object> handlePacket(Packet packet);
}
