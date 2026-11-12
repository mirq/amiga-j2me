import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.StreamConnection;

/** args: host port. A GET and a POST over http, and a raw socket. */
public class NetTest {
    static String read(InputStream in) throws Exception {
        StringBuffer sb = new StringBuffer();
        int c;
        while ((c = in.read()) >= 0) sb.append((char) c);
        return sb.toString();
    }

    public static void main(String[] args) {
        String base = "//" + args[0] + ":" + args[1];
        try {
            HttpConnection c = (HttpConnection) Connector.open("http:" + base + "/hello.txt?x=1");
            System.out.println("GET " + c.getResponseCode() + " type=" + c.getType() + " len=" + c.getLength());
            System.out.println("  body [" + read(c.openInputStream()).trim() + "]");
            c.close();

            c = (HttpConnection) Connector.open("http:" + base + "/echo");
            c.setRequestMethod(HttpConnection.POST);
            c.setRequestProperty("Content-Type", "text/plain");
            OutputStream o = c.openOutputStream();
            o.write("posted data".getBytes());
            System.out.println("POST " + c.getResponseCode() + " body [" + read(c.openInputStream()).trim() + "]");
            c.close();

            StreamConnection s = (StreamConnection) Connector.open("socket:" + base);
            OutputStream so = s.openOutputStream();
            so.write("GET /hello.txt HTTP/1.0\r\n\r\n".getBytes());
            String r = read(s.openInputStream());
            System.out.println("socket first line [" + r.substring(0, r.indexOf('\r')) + "]");
            s.close();
        } catch (Throwable t) {
            System.out.println("failed: " + t);
            t.printStackTrace();
        }
        try {
            Connector.open("http://127.0.0.1:1/none").close();
            ((HttpConnection) Connector.open("http://127.0.0.1:1/none")).getResponseCode();
            System.out.println("refused port: no exception?");
        } catch (Throwable t) {
            System.out.println("refused port: " + t.getClass().getName());
        }
        System.out.println("done");
    }
}
