package eu.jasm.Wall2;

import javafx.application.Platform;

public class ImageLoader implements Runnable {
	
	private App myApp;
	
	public ImageLoader(App myApp) {
		super();
		this.myApp = myApp;
	}

	public void run() {
		while (true) {
			if (myApp.imgLoading.size() > 0) {
		    	int i=0;
		    	while (i < myApp.imgLoading.size()) {
		    		if (myApp.imgLoading.get(i).getProgress() < 1) {
		    			i++;
		    		} else {
		    			myApp.imgQueue.add(myApp.imgLoading.get(i));
		    			myApp.imgLoading.remove(i);
		    		}
		    	}
		    	if (!myApp.imgQueueFull) {
			    	if (myApp.imgQueue.size() == myApp.maxQueue) {
			    		myApp.imgQueueFull = true;
			    		Platform.runLater(new Runnable() {
		                    public void run() {
		                    	myApp.wallNode.getChildren().remove(myApp.pb);
		                    }
		                });
			    	} else {
			    		myApp.pb.setProgress(myApp.imgQueue.size() / (float)myApp.maxQueue);
			    	}
		    	}
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				return;
			}
		}
	}

}
