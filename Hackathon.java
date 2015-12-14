import grovepi.GrovePi;
import grovepi.Pin;
import grovepi.sensors.*;
import grovepi.*;
import grovepi.i2c_devices.RgbLcdDisplay;
import java.util.Queue;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import grovepi.sensors.UltrasonicRangerSensor;
import grovepi.PinMode;

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
	private TemperatureAndHumiditySensor  humidSense; // Humidity sensor
	private float          currentHumidity;	//Stores current humidity
	private float          baseHumidity;	//Stores initial humidity
	private boolean 	   kettleBoiled;
	private float          tempHumid;	//Stores temp humidity to calculate averages
	private PubSub         pubSub;
	private UltrasonicRangerSensor		range;
	
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
		humidSense 	= grovePi.getDeviceFactory().createTemperatureAndHumiditySensor(Pin.DIGITAL_PIN_4); //Humid
		range		= grovePi.getDeviceFactory().createUltraSonicSensor(Pin.DIGITAL_PIN_3);	//Proximity Sensor
		humidSense 	= grovePi.getDeviceFactory().createTemperatureAndHumiditySensor(Pin.DIGITAL_PIN_4); //NEW
		kettleBoiled = false;
		
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
		
		//Store base Humidity
		for (int i=0; i<5; i++)	{
			humidSense.update();
			tempHumid += humidSense.getHumidity();
			
		}
		baseHumidity = (tempHumid / 5);	//Find the average room humidity
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
			if (range.getDistance() < 100){
				lcd.setBacklightRgb(225,225,225);
			}
			else if (isAvailable) {
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
			if (time % 30 == 0) { // slowing down the loop speed, so check every 15 iterations
				humidSense.update();
				currentHumidity = humidSense.getHumidity();
				System.out.println(currentHumidity);
				if (currentHumidity > (baseHumidity + (baseHumidity/100*10)) && !kettleBoiled)	{
					System.out.println("KETTLE HAS BOILED");
					pushToApi("KETTLE HAS BOILED");
					kettleBoiled = true;
				}
				// reset kettle when humidity lowers
				if (currentHumidity < (baseHumidity + (baseHumidity/100*8))) kettleBoiled = false;	
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
