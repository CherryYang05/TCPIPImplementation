package Application;

import java.util.HashMap;

/**
 * @Author Cherry
 * @Date 2020/4/8
 * @Time 12:18
 * @Brief 应用层接口
 */

public interface IApplication {
    public int getPort();

    public boolean isClosed();

    public void handleData(HashMap<String, Object> data);
}
