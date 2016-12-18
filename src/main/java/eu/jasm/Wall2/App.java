package eu.jasm.Wall2;

import java.util.ArrayList;

import javafx.animation.AnimationTimer;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;

public class App extends Application
{
	protected int canW = 1920;//1280;
	protected int canH = 1080;//720;
	protected int traverseTime = 40;
	protected int fps = 40;
	protected int frameAverageDuration = 1000 / fps;
	protected int maxQueue = 5;
	protected int postX = 20;
	protected int gapX = 30;
	protected String pathForPics = "http://<your_Server>/cgi-bin/getPic2.sh";
	protected double dx = -1;
	protected int approxH;
	protected long lastRun = -1;
	protected int startX = -1;
	protected boolean imgQueueFull = false;
	protected Thread imageLoaderThread;
	protected ArrayList<Image> imgQueue;
	protected ArrayList<Image> imgLoading;
	protected ArrayList<Frame> arrObjs;
	ProgressBar pb;
	
    Canvas canvas = new Canvas(canW,canH);
    protected Group wallNode;
    AnimationTimer animTimer;
    protected boolean generatingFrame = false;
    protected FrameGenerator frameGenerator;
    
    public void start(Stage stage) {
	    wallNode = new Group(canvas);
	    stage.setFullScreenExitHint("");
	    stage.setFullScreen(true);
	    stage.setMaximized(true);
	    Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
	    Scene scene = new Scene(wallNode, screenBounds.getWidth(), screenBounds.getHeight());
	    //to make the line below work, please check the readme.txt in resources folder
	    Font.loadFont(getClass().getResource("/DejaVuSans.ttf").toExternalForm(), 10);
	    scene.getStylesheets().add(getClass().getResource("/app.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
        
    	pb = new ProgressBar(0);
    	pb.setLayoutX(900);
    	pb.setLayoutY(500);
    	wallNode.getChildren().add(pb);
        
        this.initializeWall();
        imageLoaderThread = new Thread(new ImageLoader(this));
        imageLoaderThread.start();
        
        Communicator exiter = new Communicator(this);
        exiter.start();		
        
        frameGenerator = new FrameGenerator(this);
        
        animTimer = new AnimationTimer() {
            public void handle(long now) {
                runner(now);
            }
        };
        animTimer.start();
        
        scene.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            public void handle(final KeyEvent keyEvent) {
	                if (keyEvent.getCode() == KeyCode.DOWN) {
	                	changeSpeed(-1);
	                	keyEvent.consume();
	                }
	                if (keyEvent.getCode() == KeyCode.UP) {
	                	changeSpeed(1);
	                    keyEvent.consume();
	                }
	                if (keyEvent.getCode() == KeyCode.SPACE) {
	                	changeSpeed(0);
	                    keyEvent.consume();
	                }
            }
        });
    }
    
    private void initializeWall() {
    	imgQueue = new ArrayList<Image>();
    	imgLoading = new ArrayList<Image>();
    	arrObjs = new ArrayList<Frame>();
    	
    	dx = (canW + 0.0) / (traverseTime * 1000);
    	approxH = 4 * canH / 5 + 10;
    	startX = canW + postX;
    }
    
    protected void changeSpeed(int direction) {
    	if (!imgQueueFull) {
    		return;
    	}
    	if (direction == 0 && dx != 0) {
    		dx = 0;
    		return;
    	}
    	if (direction == 1) {
    		traverseTime+=5;
    		if (traverseTime > 60) {
    			traverseTime = 60;
    		}
    	}
    	if (direction == -1) {
    		traverseTime-=5;
    		if (traverseTime < 10) {
    			traverseTime = 10;
    		}
    	}
    	//log(""+traverseTime);
    	dx = (canW + 0.0) / (traverseTime * 1000);
    }
    
    public void runner(long now) {
    		
    	if (!imgQueueFull) {
    		downloadImage();
    		return; //Do nothing till the buffer is full;
    	}
    	
    	if (lastRun != -1) { 
    		long delta = now - lastRun;
    		delta = delta / 1000000;
    		if (delta < frameAverageDuration) {
    			return; //no refresh faster than needed;
    		}
    		if (delta > frameAverageDuration) {
    			delta = frameAverageDuration;
    		}
            drawer((int)delta);
    	}
		lastRun = now;
    }

    public void drawer(int delta) {
    	if (isFreeRoomForNewFrame()) {
    		getMoreFrames();
    	}
    	if (arrObjs.size() == 0) {
    		return;
    	}   
    	
    	GraphicsContext ctx = canvas.getGraphicsContext2D();
    	for (int i=0;i<arrObjs.size();i++) {
    		Frame aObj = arrObjs.get(i);
    		int newX = (int)Math.round(aObj.x - delta*dx);
    		if (newX -1 + aObj.w <= canW) {
           		ctx.clearRect(newX -1 + aObj.w ,aObj.y-1,delta*dx+6,aObj.h+6); // clear old position
    		}
    		aObj.x = newX;
    		if (aObj.x <= canW) {
        		ctx.drawImage(aObj.i,aObj.x,aObj.y,aObj.w,aObj.h);
    		}
    	}
    	removeFrames();    	
    }

    protected void downloadImage() {
    	if (imgQueue.size() + imgLoading.size() < maxQueue) {
    		//long noCache = Math.round((Math.random() * 100000000)) % 9999999;
    		long noCache = 1234;
    		String url = pathForPics+"?"+approxH+":"+noCache;
    		Image downloadingImage = new Image(url,true);
    		imgLoading.add(downloadingImage);
    	}
    }
    
    protected boolean isFreeRoomForNewFrame() {
    	Frame aObj = new Frame();
    	int arrObjsSize = arrObjs.size();
    	int val = 0;
    	if (arrObjsSize == 0) {
    		return true;
    	} else if (arrObjsSize == 1) {
    		aObj = arrObjs.get(0);
    		val = aObj.x + aObj.w + gapX;
    	} else {
    		aObj = arrObjs.get(arrObjsSize-1);
    		int val1 = aObj.x + aObj.w + gapX;
    		aObj = arrObjs.get(arrObjsSize-2);
    		int val2 = aObj.x + aObj.w + gapX;
    		val = Math.max(val1, val2);
    	}
    	if (val < startX) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    protected void getMoreFrames() {
    	if (imgQueue.size() < 2) {
    		return;
    	}
    	long rnd = Math.round((Math.random() * 10) + 1);

    	//we can have either 1 larger, or 2 smaller next to each-other
    	if (rnd % 2 == 0) { //one larger img
    		generatePlaceholder(1);
    	} else {
    		generatePlaceholder(2);
    	}

    }    

    protected void removeFrames() {
    	if (arrObjs.size() == 0) {
    		return;
    	}
    	Frame aObj = arrObjs.get(0);
    	if (aObj.x + aObj.w < -2) {
    		arrObjs.remove(0);
    		removeFrames();
    	}
    }
     
    protected void generatePlaceholder(final int type) {
    	if (!generatingFrame) {
    		generatingFrame = true;
            Thread myGenerator = new Thread(frameGenerator);
            frameGenerator.setType(type);
    		myGenerator.start();
    	}
    }
    
    protected void generatePlaceholderWorker(final int frameType) {
		int x,y,w,h,absH,absW,dW;
    	Image img = imgQueue.get(0);
    	imgQueue.remove(0);
    	downloadImage();
    	double ratio = img.getWidth() / img.getHeight();
    	int smallerRate = (int)Math.round((Math.random() * 10));
    	if (frameType == 1) {
    		h = 4 * canH / 5;
    		y = canH / 10;
    	} else if (frameType == 2) {
    		h = canH * 2 / 5;
    		y = canH / 15;
    	} else {
    		h = canH * 2 / 5;
    		y = canH * 8 / 15;
    	}
    	absH = h;
    	absW = (int)Math.round(absH * ratio);
    	h = h - smallerRate * h / 100; //yup, it might be also slightly smaller; Otherwise it would be boring :)
    	w = (int)Math.round(h * ratio);
    	dW = absW - w;
    	dW = dW / 2;
    	x = startX + dW;
    	Frame aObj = new Frame();
    	aObj.x = x;
    	aObj.y = y;
    	aObj.w = w;
    	aObj.h = h;
    	aObj.i = img;
    	arrObjs.add(aObj);
    }    
    
    protected void log(String x) {
    	System.out.println(x);
    }
    
    public static void main(String arg[]) {
        launch(arg);
    }
}

