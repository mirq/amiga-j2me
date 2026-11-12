/**
 * Garbage collections while other threads are stopped in every kind of place:
 * loops, switches, calls, exception handlers, synchronized blocks, waits.
 * The collector has to work out each stopped frame's stack layout.
 */
public class GcStress {
    static volatile boolean stop;
    static int total;
    static final Object lock = new Object();

    static int sw(int v) {
        switch (v & 7) {
        case 0: return v + 1;
        case 1: return v * 3;
        case 2: { String s = "a" + v; return s.length(); }
        case 5: return -v;
        default: return v ^ 0x55;
        }
    }

    static int look(int v) {
        switch (v) {
        case 10: return 1;
        case 1000: return 2;
        case -7: return 3;
        default: return v > 500 ? 4 : 5;
        }
    }

    static int thrower(int v) {
        try {
            if ((v & 3) == 0) throw new RuntimeException("x" + v);
            int[] a = new int[v & 15];
            return a.length + (v % 3 == 0 ? a[100] : 0);
        } catch (ArrayIndexOutOfBoundsException e) {
            return -1;
        } catch (RuntimeException e) {
            return e.getMessage().length();
        } finally {
            total++;
        }
    }

    static long mix(long a, double d, Object o, int i) {
        long r = a;
        for (int k = 0; k < 20; k++) {
            r = r * 31 + (long) (d * k) + (o == null ? 0 : o.hashCode() & 7) + (k < i ? sw(k) : look(k * 100));
            if ((k & 7) == 3) Thread.yield();
        }
        return r;
    }

    static class Worker extends Thread {
        final int kind;
        long sum;
        Worker(int kind) { this.kind = kind; }
        public void run() {
            int i = 0;
            Object keep = null;
            while (!stop) {
                i++;
                switch (kind) {
                case 0: sum += sw(i) + look(i % 1200); break;
                case 1: sum += thrower(i); break;
                case 2: sum += mix(i, i * 0.5, keep, i & 31); keep = (i & 63) == 0 ? new int[4] : keep; break;
                case 3:
                    synchronized (lock) { sum += i; if ((i & 255) == 0) { try { lock.wait(1); } catch (InterruptedException e) {} } }
                    break;
                default: {
                    StringBuffer sb = new StringBuffer();
                    for (int k = 0; k < 8; k++) sb.append(k).append(k % 2 == 0 ? "e" : "o");
                    sum += sb.toString().hashCode();
                }
                }
            }
        }
    }

    public static void main(String[] args) {
        int seconds = args.length > 0 ? Integer.parseInt(args[0]) : 5;
        Worker[] w = new Worker[10];
        for (int i = 0; i < w.length; i++) { w[i] = new Worker(i % 5); w[i].start(); }
        long end = System.currentTimeMillis() + seconds * 1000L;
        int gcs = 0;
        Object[] junk = new Object[64];
        while (System.currentTimeMillis() < end) {
            for (int i = 0; i < 200; i++) junk[i & 63] = new byte[(i * 37) & 1023];
            System.gc();
            gcs++;
            Thread.yield();
        }
        stop = true;
        synchronized (lock) { lock.notifyAll(); }
        for (int i = 0; i < w.length; i++) { try { w[i].join(); } catch (InterruptedException e) {} }
        System.out.println("gc stress ok: " + gcs + " collections, " + total + " finally blocks");
    }
}
