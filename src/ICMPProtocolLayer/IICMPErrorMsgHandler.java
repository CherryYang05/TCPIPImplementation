package ICMPProtocolLayer;

/**
 * @Author Cherry
 * @Date 2020/3/19
 * @Time 21:36
 * @Brief
 */

public interface IICMPErrorMsgHandler {
    public boolean handlerICMPERRORMsg(int type, int code, byte[] data);
}
