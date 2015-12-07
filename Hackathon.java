import grovepi.GrovePi;
import grovepi.Pin;
import grovepi.sensors.*;
import grovepi.i2c_devices.RgbLcdDisplay;
import java.util.Queue;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

public class Hackathon {

	// contains a list of messages to display
	private Queue<String>  messages;
	private boolean        isAvailable;
	private String         name;
	private GrovePi        grovePi;
	private RgbLcdDisplay  lcd;
	private int            delay;
	private float          glowSpeed;
	private ButtonSensor   button;
	private boolean        buttonState;
	private int            curtainThreshold;
	private PubSub         pubSub;         
	
	
	public Hackathon () throws IOException {
		messages 	= new LinkedList<String>();
		isAvailable	= false;
		name    	= "Elsie";
		grovePi 	= new GrovePi();
		button      = grovePi.getDeviceFactory().createButtonSensor(Pin.DIGITAL_PIN_5);
		buttonState = false;
		lcd 		= grovePi.getDeviceFactory().createRgbLcdDisplay();
		delay 		= 1000;
		glowSpeed   = 2.5f;
		curtainThreshold = (1024/2);
		
		// initialise the display
		lcd.display(true);
		displayMessage("Good Morning, " + name);
		
		// init pubsub server
		pubSub = new PubSub();
	}

	public void pollApi() throws IOException {
		List<StompClient.Message> msgs = pubSub.getMessages();
		for (StompClient.Message m : msgs) {
			messages.add(m.getBodyString());  
		}
	}
	
	public void pushToApi(String message) throws IOException {
		pubSub.sendMessage(message);
	}
	
	public void displayMessage(String message) {
		lcd.setText(message);
		float displaySpeed = 2.0f;
		int count = 0;
		for (int i = 0; i < delay; i++) {
			count += displaySpeed;
			if (count >= 255) {
				count = 255;
				displaySpeed = -displaySpeed;
			} else if (count <= 0) {
				count = 0;
				displaySpeed = -displaySpeed;
			}
			lcd.setBacklightRgb(127, count, 127);
		}
		lcd.setText("");
	}

	public void run() throws IOException {
		
		float f = 0;
		
		int time = 0;
		
		while (true) {
			
			// poll the api every so often
			time += 1;
			if (time > 510) {
				time = 0;
				pollApi();
			}
			
			// check message queue, if there is a message, display it!
			String message = messages.poll();
			if (message != null) {
				displayMessage(message);
			}
			
			// set backlight colour, red/green depending on isAvailable
			if (isAvailable) {
				lcd.setBacklightRgb(0,(int)f,0);
			} else {
				lcd.setBacklightRgb((int)f,0,0);
			}
			
			// change polarity of glowspeed (ensure that it stays within
			// 0 - 255 range
			f += glowSpeed;
			if (f > 255 || f < 0) {
				f -= glowSpeed; // this prevents a flicker!
				glowSpeed = -glowSpeed;
			}
			
			// check if curtains are open
			if (grovePi.analogRead(Pin.ANALOG_PIN_0) < curtainThreshold) {
				isAvailable = false;
			} else {
				isAvailable = true;
			}
			
			// poll api!
			if (button.isPressed() && !buttonState) {
				buttonState = true;
				pushToApi("MMM, having a good brew.");
				System.out.println("sending a brew message");
			} else {
				buttonState = false;
			}
			
		}
	}

	public static void main(String[] args) {
		try {
			Hackathon h = new Hackathon();
			h.pollApi();
			h.run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
