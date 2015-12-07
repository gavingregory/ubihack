import grovepi.GrovePi;
import grovepi.Pin;
import grovepi.sensors.*;
import grovepi.*;
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
	private boolean 		buttonState;
	private int            curtainThreshold;
	private TemperatureAndHumiditySensor	humidSense; // Humidity sensor
	private float	currentHumidity;	//Stores current humidity
	private float	baseHumidity;	//Stores initial humidity
	private float	tempHumid;	//Stores temp humidity to calculate averages
	
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
		humidSense 	= grovePi.getDeviceFactory().createTemperatureAndHumiditySensor(Pin.DIGITAL_PIN_4); //NEW
		
		// initialise the display
		lcd.display(true);
		displayMessage("Good Morning, " + name);
	}

	public void pollApi() {
		messages.add("You have received a new message");
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
		
		//Store base Humidity
			for (int i=0; i<5; i++)	{
			humidSense.update();
			tempHumid += humidSense.getHumidity();
			//System.out.println("temp Humidity: " + tempHumid);
		}
		baseHumidity = (tempHumid / 5);	//Find the average room humidity
		//System.out.println("Base Humidity: " + baseHumidity);
		
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
			
			//Humidity Sensor
			
			//Calculate average & compare to initial humidity
			
			/* tempHumid = 0;
			for (int i=0; i<5; i++)	{
				humidSense.update();
				tempHumid += humidSense.getHumidity();
			System.out.println("temp Humidity: " + tempHumid);
			}
			currentHumidity = (tempHumid / 5);
			
			System.out.println("Humidity: " + currentHumidity);*/
			
			
			//Calculate current humidity & compare to initial
			humidSense.update();
			currentHumidity = humidSense.getHumidity();
			if (currentHumidity > (baseHumidity + (baseHumidity/100*20)))	{
				System.out.println("KETTLE HAS BOILED");
				pushToApi("KETTLE HAS BOILED");
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
	
	public void checkHumid(){
		
	}

	public static void main(String[] args) {
		Hackathon h = new Hackathon();
		h.pollApi();
		h.run();
	}

}
