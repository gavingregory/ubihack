import grovepi.GrovePi;
import grovepi.Pin;
import grovepi.sensors.*;
import grovepi.i2c_devices.RgbLcdDisplay;
import java.util.Queue;
import java.util.LinkedList;

public class Hackathon {

	// contains a list of messages to display
	private Queue<String>	messages;
	private boolean		isAvailable;
	private String			name;
	private GrovePi			grovePi;
	private RgbLcdDisplay	lcd;
	private int				delay;
	private float			glowSpeed;
	private ButtonSensor	button;
	
	
	public Hackathon () {
		messages 	= new LinkedList<String>();
		isAvailable	= false;
		name    	= "Elsie";
		grovePi 	= new GrovePi();
		button      = grovePi.getDeviceFactory().createButtonSensor(Pin.DIGITAL_PIN_5);
		lcd 		= grovePi.getDeviceFactory().createRgbLcdDisplay();
		delay 		= 1000;
		glowSpeed   = 0.6f;
		
		// initialise the display
		lcd.display(true);
		displayMessage("Good Morning, " + name);
	}

	public void pollApi() {
		messages.add("You have received a new message");
	}
	
	public void displayMessage(String message) {
		lcd.setText(message);
		int polarity = 2;
		int count = 0;
		for (int i = 0; i < delay; i++) {
			if (count >= 255) {
				count = 255;
				polarity = -2;
			} else if (count <= 0) {
				count = 0;
				polarity = 2;
			}
			lcd.setBacklightRgb(127, count, 127);
		}
		lcd.setText("");
	}

	public void run() {
		
		float f = 0;
		
		while (true) {
			
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
			System.out.println(f);
			if (f > 255 || f < 0) {
				glowSpeed = -glowSpeed;
			}
			
			// check if button is pressed
			if (button.isPressed()) {
				isAvailable = true;
			} else {
				isAvailable = false;
			}
			
			
		}
	}

	public static void main(String[] args) {
		Hackathon h = new Hackathon();
		h.pollApi();
		h.run();
		
		
	}

}
