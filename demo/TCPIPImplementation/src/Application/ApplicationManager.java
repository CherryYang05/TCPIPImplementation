package Application;

import java.util.ArrayList;
/*
 * 将所有应用对象注册在队列中，当协议部分需要将数据提交给对应应用时，
 * Manager通过Application导出的port进行查找
 */
public class ApplicationManager  {
	private static ArrayList<IApplication> application_list = new ArrayList<IApplication>();
	private static ApplicationManager instance = null;
	
	private  ApplicationManager() {
		
	}
	
	public static  ApplicationManager getInstance() {
		if (instance == null) {
			instance = new ApplicationManager();
		}
		
		return instance;
	}
	
	public static void addApplication(IApplication app) {
		application_list.add(app);
	}

	public IApplication getApplicationByPort(int port) {
		for (int i = 0; i < application_list.size(); i++) {
			IApplication app = application_list.get(i);
			if (app.getPort() == port) {
				return app;
			}
		}
		
		return null;
	}

}
