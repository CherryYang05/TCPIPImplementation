package utils;

/**
 * @Author Cherry
 * @Date 2020/5/10
 * @Time 22:34
 * @Brief ARPProtocolLayer要求所有通过它获取mac地址的对象都必须实现
 * IMacReceiver接口，有可能很多个上层协议对象都需要获得同一个ip对应
 * 设备的mac地址，它会把这些对象存储在一个队里中，一旦给定ip设备返回
 * 包含它mac地址的ARP消息后，ARPProtocolLayer从消息中解读出mac地址，
 * 它就会把该地址推送给所有需要的接收者
 */

public interface IMacReceiver {
    public void receiveMacAddress(byte[] ip, byte[] mac);
}
