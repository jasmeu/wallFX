package eu.jasm.Wall2;

public class FrameGenerator implements Runnable {
	private App myApp;
	private int type;
	
	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public FrameGenerator(App myApp) {
		super();
		this.myApp = myApp;
	}

	public void run() {
		if (type == 1) {
			myApp.generatePlaceholderWorker(1);
		} else if (type == 2) {
			myApp.generatePlaceholderWorker(2);
			myApp.generatePlaceholderWorker(3);
		}
		myApp.generatingFrame = false;
	}
	
	
}
