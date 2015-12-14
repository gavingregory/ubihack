import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

public class PubSub {
	
	private String host;
	private int port;
	private String login;
	private String passcode;
	private StompClient stomp;
	private String group;
	private String hostname;
	
	// Constructor
	public PubSub() throws IOException {
		host = "sjmf.in";//"stomp.di-test.com";
		port = 61613;
		String login = "pi"; //"ubicomp";
		String passcode = "raspberry"; //"raspberry";
		stomp = new StompClient(host, port, login, passcode);
		group = "/topic/brewtime";
		hostname = System.getenv("HOSTNAME");
		subscribe();
	}
	
	public void sendMessage(String message) throws IOException {
		stomp.send(group, message);
		System.out.println("stomp is sending " + message);
	}
	
	public List<StompClient.Message> getMessages() throws IOException {
		
		List<StompClient.Message> msgs = new ArrayList<StompClient.Message>();
		try {
			System.out.println("we are checking for messages");
			StompClient.Message message;
			while ((message = stomp.receiveMessageIfAvailable()) != null) {
				msgs.add(message);
				System.out.println("m: " + message.getBodyString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return msgs;
	}
	
	private void subscribe() throws IOException {
		stomp.subscribe(group);
	}
	
}
