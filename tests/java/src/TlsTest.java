import java.io.InputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpsConnection;

/** args: URLs. Fetches each over https and prints the status and the first bytes. */
public class TlsTest {
    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            try {
                HttpsConnection c = (HttpsConnection) Connector.open(args[i]);
                int code = c.getResponseCode();
                InputStream in = c.openInputStream();
                StringBuffer sb = new StringBuffer();
                int ch, n = 0;
                while ((ch = in.read()) >= 0) { if (n++ < 40 && ch >= 32) sb.append((char) ch); }
                System.out.println(args[i] + " -> " + code + " " + c.getSecurityInfo().getProtocolName() + " port " + c.getPort()
                                   + ", " + n + " bytes [" + sb + "]");
                c.close();
            } catch (Throwable t) {
                System.out.println(args[i] + " -> failed: " + t);
            }
        }
    }
}
