package Application;

import java.util.HashMap;

public interface IApplication {
    public  int getPort();
    public boolean isClosed(); 
    public  void handleData(HashMap<String, Object> data);
}
