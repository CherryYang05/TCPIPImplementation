package Application;

import java.util.ArrayList;

/**
 * @Author Cherry
 * @Date 2020/4/8
 * @Time 12:21
 * @Brief 将所有应用对象注册在队列中，当协议部分需要将数据提交给对应应用时
 * Manager 通过 Application 导出的 port 进行查找
 */

public class ApplicationManager {
    private static ArrayList<IApplication> app_list = new ArrayList<>();
    private static ApplicationManager instance = null;

    private ApplicationManager() {
    }

    /**
     * 单例
     *
     * @return
     */
    public static ApplicationManager getInstance() {
        if (instance == null) {
            instance = new ApplicationManager();
        }
        return instance;
    }

    public static void addApplication(IApplication app) {
        app_list.add(app);
    }

    /**
     * @param port port
     * @return application
     */
    public IApplication getApplicationByPort(int port) {
        for (IApplication app : app_list) {
            if (app.getPort() == port) {
                return app;
            }
        }
        return null;
    }
}
