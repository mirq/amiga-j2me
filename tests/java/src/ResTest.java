import java.io.InputStream;

/** Reads resources from the classpath (run with a deflated JAR): prints sizes and checksums. */
public class ResTest {
    static void read(String name, boolean bulk) {
        try {
            System.out.println("open " + name);
            InputStream in = ResTest.class.getResourceAsStream(name);
            if (in == null) { System.out.println("  null"); return; }
            int n = 0, sum = 0;
            if (bulk) {
                byte[] buf = new byte[300];
                int r;
                while ((r = in.read(buf)) > 0) { for (int i = 0; i < r; i++) sum = sum * 31 + buf[i]; n += r; }
            } else {
                int c;
                while ((c = in.read()) >= 0) { sum = sum * 31 + (byte) c; n++; }
            }
            in.close();
            System.out.println("  " + n + " bytes, sum " + sum);
        } catch (Throwable t) {
            System.out.println("  failed: " + t);
        }
    }

    public static void main(String[] args) {
        read("/META-INF/MANIFEST.MF", false);
        read("/META-INF/MANIFEST.MF", true);
        read("/pal.png", true);
        read("/big.bin", true);
        read("/big.bin", false);
        read("/nothing.here", true);
        System.out.println("done");
    }
}
