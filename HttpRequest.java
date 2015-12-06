import java.lang.*

public class HttpRequest implements Runnable{
String apiUrl;
  public void HttpRequest(String api){
    apiUrl = api;
  }

  public void run(){
    System.out.print(t.getName());

    System.out.println(", status = "+ t.isAlive());
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
  }


}
