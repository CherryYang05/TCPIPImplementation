package Application;

import java.util.HashMap;

public class Application implements IApplication{
    protected  int port = 0;
    private boolean closed = false;
    
    public Application() {
    	ApplicationManager manager = ApplicationManager.getInstance();
    	manager.addApplication(this);
    }
    
	@Override
	public int getPort() {
		return port;
	}

	@Override
	public void handleData(HashMap<String, Object> data) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isClosed() {
		
		return closed;
	}

}
