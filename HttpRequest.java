import java.lang.*;
import java.net.*;
import java.io.*;
import java.util.Queue;
import java.util.LinkedList;

public class HttpRequest implements Runnable{

	private String apiUrl;
	private Queue<String> messages;

	public HttpRequest(String api){
		messages = new LinkedList<String>();
		apiUrl = api;
	}

	public Queue<String> getMessages() {
		return messages;
	}

	public void run() {
		try {
		Thread.sleep(2000);
		System.out.println("I AM ALIVE, YO");
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
	}


}
