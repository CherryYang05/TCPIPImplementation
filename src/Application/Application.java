package Application;

import java.util.HashMap;

/**
 * @Author Cherry
 * @Date 2020/4/8
 * @Time 12:20
 * @Brief
 */

public class Application implements IApplication {
    protected int port = 0;     //设置源端口
    private final static boolean CLOSED = false;

    public Application() {
        //ApplicationManager manager = ApplicationManager.getInstance();
        //manager.addApplication(this);
        ApplicationManager.addApplication(this);
    }

    /**
     * 获取目的端口号，进行查找相应应用实例
     *
     * @return 目的端口号
     */
    @Override
    public int getPort() {
        return port;
    }

    @Override
    public boolean isClosed() {
        return CLOSED;
    }

    /**
     * 处理接收到的数据包
     *
     * @param data
     */
    @Override
    public void handleData(HashMap<String, Object> data) {

    }
}
