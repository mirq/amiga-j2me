import java.util.Hashtable;
import java.util.Vector;

/** Bytecode coverage: prints one checksum line per area; must match on every platform. */
public class VmTest {
    interface Shape { int area(); }
    static class Rect implements Shape {
        int w, h;
        Rect(int w, int h) { this.w = w; this.h = h; }
        public int area() { return w * h; }
    }
    static class Sq extends Rect {
        Sq(int s) { super(s, s); }
        public int area() { return super.area() + 1; }
    }
    static long sl = 0x123456789ABCDEFL;
    static double sd = 3.25;
    static float sf = 1.5f;
    static int[] st = { 5, 4, 3, 2, 1 };
    static final Object LOCK = new Object();

    static int tswitch(int v) {
        switch (v) {
        case 0: return 10;
        case 1: return 11;
        case 2: return 12;
        case 3: return 13;
        case 5: return 15;
        default: return -1;
        }
    }

    static int lswitch(int v) {
        switch (v) {
        case -1000: return 1;
        case 7: return 2;
        case 100000: return 3;
        case 0x7fffffff: return 4;
        default: return 0;
        }
    }

    static int fin(int v) {
        int r = 0;
        try {
            if (v == 1) throw new IllegalArgumentException("x");
            if (v == 2) { int[] a = new int[1]; a[v] = 1; }
            if (v == 3) { Object o = "s"; Integer i = (Integer) o; }
            if (v == 4) r = 10 / (v - 4);
            r += 1;
        } catch (IllegalArgumentException e) {
            r += 100;
        } catch (ArrayIndexOutOfBoundsException e) {
            r += 200;
        } catch (ClassCastException e) {
            r += 300;
        } catch (ArithmeticException e) {
            r += 400;
        } finally {
            r += 1000;
        }
        return r;
    }

    static int manyLocals(int a) {
        int l0=a,l1=1,l2=2,l3=3,l4=4,l5=5,l6=6,l7=7,l8=8,l9=9;
        long g0=10,g1=11,g2=12,g3=13,g4=14,g5=15,g6=16,g7=17,g8=18,g9=19;
        double d0=1,d1=2,d2=3,d3=4,d4=5,d5=6,d6=7,d7=8,d8=9,d9=10;
        int[] arr = new int[300];
        for (int i = 0; i < 300; i++) arr[i] = i * l3 + l9;
        int s = 0;
        for (int i = 0; i < 300; i++) s += arr[i];
        return s + l0 + l1 + l2 + l4 + l5 + l6 + l7 + l8 + (int) (g0 + g1 + g2 + g3 + g4 + g5 + g6 + g7 + g8 + g9)
             + (int) (d0 + d1 + d2 + d3 + d4 + d5 + d6 + d7 + d8 + d9);
    }

    public static void main(String[] args) {
        int h;

        h = 0;
        for (int i = -2; i < 8; i++) h = h * 31 + tswitch(i);
        int[] lv = { -1000, 7, 100000, 0x7fffffff, 5, -1 };
        for (int i = 0; i < lv.length; i++) h = h * 31 + lswitch(lv[i]);
        System.out.println("switch " + h);

        long a = 0x0123456789ABCDEFL, b = -77777777777L;
        long l = a + b; l ^= a * 3; l += a / 1000; l -= b % 12345; l ^= (a << 13) | (a >>> 7) | (b >> 3);
        l += (a & b) - (a | 5) + (-a);
        l += sl;
        System.out.println("long " + l + " " + (a < b) + (a == a) + " " + (int) l + " " + (short) l + " " + (byte) l + " " + (char) (l & 0xff));

        int i1 = 0x12345678, i2 = -987654;
        int iv = i1 + i2; iv ^= i1 * i2; iv += i1 / 77; iv -= i2 % 1001; iv ^= (i1 << 5) | (i1 >>> 9) | (i2 >> 3); iv += -i1 + (i1 & i2) - (i1 | 3);
        System.out.println("int " + iv + " " + (long) iv + " " + (iv * 1.5f) + " " + (iv * 2.5));

        double d = sd * 7.1 - 3.0 / 7.0 + 1e10 % 7.3; d = -d + Math.sqrt(2.0) * Math.abs(-4.5);
        float f = sf * 3.1f - 2.0f / 3.0f; f = -f + 1e5f % 3.3f;
        System.out.println("fp " + d + " " + f + " " + (int) d + " " + (long) d + " " + (float) d + " " + (double) f
            + " " + (int) 1e20 + " " + (long) -1e30 + " " + (int) Float.NaN + " " + (d > f) + (d < f) + (Double.NaN == Double.NaN));
        System.out.println("fpbits " + Double.doubleToLongBits(1.1) + " " + Float.floatToIntBits(1.1f) + " "
            + Double.longBitsToDouble(0x400921FB54442D18L) + " " + Float.intBitsToFloat(0x40490FDB));

        byte[] ba = new byte[5]; short[] sa = new short[5]; char[] ca = new char[5]; long[] la = new long[5];
        float[] fa = new float[5]; double[] da = new double[5]; boolean[] za = new boolean[5];
        for (int i = 0; i < 5; i++) {
            ba[i] = (byte) (i * 60); sa[i] = (short) (i * 9000); ca[i] = (char) (i * 15000); la[i] = i * 5000000000L;
            fa[i] = i * 1.25f; da[i] = i * 1e100; za[i] = (i & 1) == 0;
        }
        l = 0;
        for (int i = 0; i < 5; i++) l = l * 131 + ba[i] + sa[i] + ca[i] + la[i] + (long) fa[i] + (long) (da[i] / 1e99) + (za[i] ? 1 : 0) + st[i];
        int[][] m2 = new int[3][4]; int[][][] m3 = new int[2][3][4]; String[][] ms = new String[2][2];
        m2[2][3] = 7; m3[1][2][3] = 9; ms[1][1] = "q";
        System.out.println("arrays " + l + " " + m2.length + m2[0].length + m3[1][2].length + m2[2][3] + m3[1][2][3] + ms[1][1] + ms[0][0]);

        Shape[] shapes = { new Rect(3, 4), new Sq(5), new Rect(1, 1) };
        h = 0;
        for (int i = 0; i < shapes.length; i++) {
            h = h * 31 + shapes[i].area();
            if (shapes[i] instanceof Sq) h += 1000;
            if (shapes[i] instanceof Rect) h += ((Rect) shapes[i]).w;
        }
        Object o = shapes;
        h += (o instanceof Shape[]) ? 5 : 0;
        h += (o instanceof Object[]) ? 7 : 0;
        System.out.println("objects " + h);

        h = 0;
        for (int i = 0; i < 6; i++) h = h * 7 + fin(i);
        System.out.println("exceptions " + h);

        System.out.println("locals " + manyLocals(42));

        StringBuffer sb = new StringBuffer();
        sb.append(1).append('c').append(2L).append(1.5f).append(2.5).append(true).append("s").append((Object) null);
        String s = sb.toString();
        Hashtable ht = new Hashtable(); Vector v = new Vector();
        for (int i = 0; i < 50; i++) { ht.put("k" + i, new Integer(i * i)); v.addElement(new Long(i * 1000000007L)); }
        l = 0;
        for (int i = 0; i < 50; i++) l += ((Integer) ht.get("k" + i)).intValue() + ((Long) v.elementAt(i)).longValue();
        System.out.println("strings " + s + " " + s.hashCode() + " " + "abc".compareTo("abd") + " " + s.indexOf("true") + " "
            + Integer.toHexString(-559038737) + " " + Long.toString(Long.MIN_VALUE) + " " + Integer.parseInt("-12345") + " " + l
            + " " + Double.toString(1.0 / 3.0) + " " + Float.toString(1.0f / 3.0f) + " " + Double.parseDouble("6.02e23"));

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

        // garbage: many short-lived objects, some kept
        Vector keep = new Vector();
        l = 0;
        for (int i = 0; i < 20000; i++) {
            byte[] g = new byte[(i % 97) + 1];
            g[0] = (byte) i;
            if (i % 500 == 0) keep.addElement(g);
            l += g.length + g[0];
        }
        for (int i = 0; i < keep.size(); i++) l += ((byte[]) keep.elementAt(i))[0];
        System.out.println("gc " + l + " " + keep.size());
        System.out.println("done");
    }
}
