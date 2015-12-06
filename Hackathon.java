import grovepi.GrovePi;
import grovepi.Pin;
import grovepi.sensors.*;
import grovepi.i2c_devices.RgbLcdDisplay;
import java.util.Queue;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;

public class Hackathon {

	// contains a list of messages to display
	private Queue<String>	messages;
	private boolean		    isAvailable;
	private String			name;
	private GrovePi			grovePi;
	private RgbLcdDisplay	lcd;
	private int				delay;
	private float			glowSpeed;
	private ButtonSensor	button;
	private boolean 		buttonState;
	private int             curtainThreshold;
	private String			apiUrl;
	private List<Thread>    httpThreads;

	public Hackathon () {
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
		apiUrl      = "http://www.google.co.uk";
		httpThreads = new ArrayList<Thread>();

		// initialise the display
		lcd.display(true);
		displayMessage("Good Morning, " + name);
	}

	public void pollApi() {
		httpThreads.add(new Thread(new HttpRequest(apiUrl)));
		httpThreads.add(new Thread(new HttpRequest(apiUrl)));
		httpThreads.add(new Thread(new HttpRequest(apiUrl)));
		
		/*
		try {
			URLConnection connection = new URL(apiUrl).openConnection();
			connection.connect();
			InputStream in = connection.getInputStream();
			try {
				java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
				String m = s.hasNext() ? s.next() : null;
				if (m != null) {
					messages.add(m);
					messages.add(m);
					messages.add(m);
					messages.add(m);
					messages.add(m);
				}
			} finally {
				in.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		*/
	}

	public void pushToApi(String event) {
		// TODO: push to the API
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

	public void run() {

		float f = 0;

		while (true) {
			//// check for any finished httpThreads
			//for (int i = 0; i < httpThreads.size(); i++) {
			//	if (!httpThreads.get(i).isAlive()) {
			//		HttpRequest r = httpThreads.get(i).currentThread();
			//		Queue<String> msgs = r.getMessages();
			//		for (String s: msgs) {
			//			displayMessage(s);
			//		}
			//	}
			//}
			
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
				pollApi();
				System.out.println("polling API ...");
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
				pollApi();
			} else {
				buttonState = false;
			}

		}
	}

	public static void main(String[] args) {
		Hackathon h = new Hackathon();
		h.pollApi();
		h.run();
	}

}
