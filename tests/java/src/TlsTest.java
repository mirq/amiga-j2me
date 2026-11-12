import java.io.InputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpsConnection;

/** args: URLs. Fetches each over https and prints the status, the time and the first bytes. */
public class TlsTest {
    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            long t0 = System.currentTimeMillis();
            try {
                HttpsConnection c = (HttpsConnection) Connector.open(args[i]);
                int code = c.getResponseCode();
                InputStream in = c.openInputStream();
                byte[] buf = new byte[512];
                StringBuffer sb = new StringBuffer();
                int n, total = 0;
                while ((n = in.read(buf, 0, buf.length)) > 0) {
                    for (int k = 0; k < n && sb.length() < 40; k++) if (buf[k] >= 32) sb.append((char) buf[k]);
                    total += n;
                }
                System.out.println(args[i] + " -> " + code + " " + c.getSecurityInfo().getProtocolName() + " port " + c.getPort()
                                   + ", " + total + " bytes in " + (System.currentTimeMillis() - t0) + " ms [" + sb + "]");
                c.close();
            } catch (Throwable t) {
                System.out.println(args[i] + " -> failed after " + (System.currentTimeMillis() - t0) + " ms: " + t);
            }
        }
    }
}
