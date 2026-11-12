import java.util.Vector;

/** One piece of VmTest at a time: args[0] = long | fp | fpstr | str | thr | gc. To find which one a machine chokes on. */
public class Sect {
    static double sd = 3.25;
    static float sf = 1.5f;
    static final Object LOCK = new Object();

    public static void main(String[] args) {
        String w = args.length > 0 ? args[0] : "";
        if (w.equals("long")) {
            long a = 0x0123456789ABCDEFL, b = -77777777777L;
            long l = a + b; l ^= a * 3; l += a / 1000; l -= b % 12345; l ^= (a << 13) | (a >>> 7) | (b >> 3);
            System.out.println("long " + l);
        } else if (w.equals("fp")) {
            double d = sd * 7.1 - 3.0 / 7.0; d = -d + Math.sqrt(2.0) * Math.abs(-4.5);
            float f = sf * 3.1f - 2.0f / 3.0f;
            int bits = (int) (d * 1000) + (int) (f * 1000) + (int) 1e20 + (int) Float.NaN + (d > f ? 1 : 0);
            long lb = (long) -1e30;
            System.out.println("fp " + bits + " " + (lb == Long.MIN_VALUE));
        } else if (w.equals("rem")) {
            double d = 1e10 % 7.3; float f = 1e5f % 3.3f;
            System.out.println("rem " + (int) (d * 1000) + " " + (int) (f * 1000));
        } else if (w.equals("fpstr")) {
            System.out.println("fpstr " + (sd * 7.1) + " " + (sf * 3.1f) + " " + Double.toString(1.0 / 3.0) + " " + Double.parseDouble("6.02e23"));
        } else if (w.equals("str")) {
            StringBuffer sb = new StringBuffer();
            sb.append(1).append('c').append(2L).append(true).append("s").append((Object) null);
            System.out.println("str " + sb + " " + Long.toString(Long.MIN_VALUE) + " " + Integer.toHexString(-559038737));
        } else if (w.equals("thr")) {
            final int[] counter = new int[1];
            Thread[] ts = new Thread[4];
            for (int t = 0; t < ts.length; t++) {
                ts[t] = new Thread() {
                    public void run() {
                        for (int i = 0; i < 2000; i++) {
                            synchronized (LOCK) { counter[0]++; }
                            if ((i & 255) == 0) Thread.yield();
                        }
                    }
                };
                ts[t].start();
            }
            try {
                for (int t = 0; t < ts.length; t++) ts[t].join();
            } catch (InterruptedException e) { }
            System.out.println("threads " + counter[0]);
        } else if (w.equals("gc")) {
            Vector keep = new Vector();
            long l = 0;
            for (int i = 0; i < 20000; i++) {
                byte[] g = new byte[(i % 97) + 1];
                g[0] = (byte) i;
                if (i % 500 == 0) keep.addElement(g);
                l += g.length + g[0];
            }
            System.out.println("gc " + l + " " + keep.size());
        } else {
            System.out.println("which? long fp rem fpstr str thr gc");
        }
    }
}
