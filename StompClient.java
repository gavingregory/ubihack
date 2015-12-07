// Quick-and-dirty STOMP Client (not suitable for production)
// Dan Jackson, 2015

import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

public class StompClient {		// extends java.io.Closeable

	// Message
	public static class Message {
		private String method = "";
		private HashMap<String, String> headers = new HashMap<String, String>();
		private byte[] body = null;

		// STOMP message to send
		public Message(String method) {
			this(method, null);
		}

		// STOMP message to send
		public Message(String method, String body) {
			// Message method
			this.method = method;
			setBody(body);
		}

		public static byte[] toByteArray(List<Byte> list) {
		    byte array[] = new byte[list.size()];
		    for (int i = 0; i < list.size(); i++) {
		        array[i] = list.get(i);
		    }
		    return array;
		}

		private static byte[] read(InputStream in, int length) throws IOException {
			int offset = 0;
			byte[] data = new byte[length];
			while (offset < length) {
				int r = in.read(data, offset, length);
				offset += r;
				length -= r;
			}
			return data;
		}

		private static byte[] readUntil(InputStream in, int end) throws IOException {
			List<Byte> body = new ArrayList<Byte>();
			for (;;) {
				int c = in.read();
				if (c == end) { break; }
				body.add((byte)c);
			}
			return toByteArray(body);
		}

		private static String readLine(InputStream in) throws IOException {
			byte[] lineBytes = readUntil(in, 10);
			String line = new String(lineBytes, "UTF-8");
			return line.trim();
		}

		public String getMethod() {
			return method;
		}

		public void setBody(String bodyString) {
			// Message body
			if (bodyString == null) {
				body = null;
			} else {
				try {
					body = bodyString.getBytes("UTF-8");
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				}
			}
		}

		public void setBody(byte[] body) {
			this.body = body;
		}

		public byte[] getBody() {
			return body;
		}

		public String getBodyString() {
			if (body == null) { return null; }
			try {
				return new String(body, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}

		public void addHeader(String name, String value) {
			headers.put(name.trim().toLowerCase(), value.trim());
		}

		public String getHeader(String name) {
			return headers.get(name.toLowerCase());
		}

		public int getHeaderValue(String name, int defaultValue) {
			String value = getHeader(name);
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				return defaultValue;
			}
		}

		// STOMP message received
		public static Message fromInputStream(InputStream in) throws IOException {
			// Read method
			String method = readLine(in);

			// Cope with optional trailing \r\n after previous message
			if (method == null || method.length() == 0) { return null; }

			Message message = new Message(method, null);

			// Read header lines
			for (;;) {
				String headerLine = readLine(in);
				if (headerLine.length() <= 0) {
					break;
				}
				String[] parts = headerLine.split("\\:", 2);
				if (parts.length >= 2) {
					message.addHeader(parts[0], parts[1]);
				}
			}

			// Read request body
			int contentLength = message.getHeaderValue("Content-Length", -1);
			byte[] body;
			if (contentLength >= 0) {
				body = read(in, contentLength);
				readUntil(in, 0);								// Discard NULL
			} else {
				body = readUntil(in, 0);
			}

			message.setBody(body);

			return message;
		}

		public void writeTo(OutputStream out) throws IOException {
			List<Byte> data = new ArrayList<Byte>();
			try {
				out.write((method + "\r\n").getBytes("UTF-8"));
				for (HashMap.Entry<String, String> entry : headers.entrySet()) {
				    String line = entry.getKey() + ":" + entry.getValue() + "\r\n";
						out.write(line.getBytes("UTF-8"));
				}
				out.write("\r\n".getBytes("UTF-8"));
				if (body != null) {
					out.write(body);
				}
				out.write(new byte[] { 0 });		// NULL ending
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}

		public static Message createConnectMessage(String login, String passcode) {
			Message message = new Message("CONNECT");
			if (login != null) {
				message.addHeader("login", login);
			}
			if (passcode != null) {
				message.addHeader("passcode", passcode);
			}
			return message;
		}

		public static Message createSubscribeMessage(String destination) {
			Message message = new Message("SUBSCRIBE");
			message.addHeader("destination", destination);
			return message;
		}

		public static Message createSendMessage(String destination, String body) {
			Message message = new Message("SEND");
			message.addHeader("destination", destination);
			message.setBody(body);
			return message;
		}

		public boolean isMessage() {
			return (method != null && method.equals("MESSAGE"));
		}
		
		public String getDestination() {
			return headers.get("destination");
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(method);
			String destination = getHeader("destination");
			if (destination != null) {
				sb.append("@").append(destination);
			}
			String body = getBodyString();
			if (body != null) {
				sb.append("=").append(body);
			}
			return sb.toString();
		}

	}


	private Socket socket;
	private InputStream in;
	private OutputStream out;
	public static final int DEFAULT_PORT = 61613;

	public StompClient(String host) throws IOException {
		this(host, (String)null, (String)null);
	}

	public StompClient(String host, String login, String passcode) throws IOException {
		this(host, DEFAULT_PORT, login, passcode);
	}

	public StompClient(String host, int port, String login, String passcode) throws IOException {
		socket = new Socket(host, port);		
		try {
			in = socket.getInputStream();
			out = socket.getOutputStream();
			Message msg = Message.createConnectMessage(login, passcode);
			//msg.writeTo(System.out);
			sendMessage(msg);
			Message message = receiveMessage();
			
			if (message == null || message.getMethod() == null) {
				throw new IOException("Did not receive CONNECTED message.");
			} else if (!message.getMethod().equals("CONNECTED")) {
				throw new IOException("Did not receive CONNECTED message, got: " + message.toString());
			}
		} catch (Exception e) {
			socket.close();
			throw e;
		}
		
	}
	
	public void close() throws IOException {
		socket.close();
	}

	public boolean isMessageAvailable() throws IOException {
		return in.available() > 0;
	}

	public Message receiveMessage() throws IOException {
		return Message.fromInputStream(in);
	}

	public Message receiveMessageIfAvailable() throws IOException {
		while (isMessageAvailable()) {
			Message message = Message.fromInputStream(in);
			if (message == null) { continue; }			// Empty message, ignore
			if (!message.isMessage()) { continue; }		// Not a "message", ignore
			return message;
		}
		return null;
	}

	public void sendMessage(Message message) throws IOException {
		message.writeTo(out);
	}

	public void subscribe(String topic) throws IOException {
		sendMessage(Message.createSubscribeMessage(topic));
	}

	public void send(String topic, String body) throws IOException {
		sendMessage(Message.createSendMessage(topic, body));
	}



	// Demonstration main method
	public static void main(String[] args) throws Exception {
		
		// Display usage
		if (args.length < 1) {
			System.err.println("Usage: StompClient <host> [<port=61613> [<login> <passcode>]]");
			return;
		}
		
		// Connection parameters
		String host = (args.length > 0) ? args[0] : "localhost";			// "stomp.di-test.com"
		int port = (args.length > 1) ? Integer.parseInt(args[1]) : 61613;	// 61613
		String login = (args.length > 2) ? args[2] : null;					// "ubicomp"
		String passcode = (args.length > 3) ? args[3] : null;				// 
		
		// Make connection
		System.err.println("Connecting to " + host + ":" + port + " (" + login + ":" + passcode + ")");
		StompClient stomp = new StompClient(host, port, login, passcode);
		
		// Subscribe to topics of interest
		stomp.subscribe("/topic/hello");

		// Main loop
		while (true) {

			// Receive any incoming STOMP messages
			StompClient.Message message;
			while ((message = stomp.receiveMessageIfAvailable()) != null) {
				System.out.println("MESSAGE: " + message.getDestination() + " = " + message.getBodyString());
			}

			// Send a message
			stomp.send("/topic/hello", "Hello World! " + System.currentTimeMillis());
			
			// Delay
			Thread.sleep(1000);
		}
	}


}
