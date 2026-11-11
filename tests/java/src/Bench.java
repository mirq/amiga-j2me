/** Small interpreter benchmark: prints ms per workload (lower is better). */
public class Bench {
    static int fib(int n) { return n < 2 ? n : fib(n - 1) + fib(n - 2); }

    int field;
    void inc() { field++; }

    public static void main(String[] args) {
        long t;
        t = System.currentTimeMillis();
        int s = 0;
        for (int i = 0; i < 1000000; i++) { s += i ^ (s >> 3); }
        System.out.println("loop1M     " + (System.currentTimeMillis() - t) + " ms (" + s + ")");

        t = System.currentTimeMillis();
        int f = fib(24);
        System.out.println("fib24      " + (System.currentTimeMillis() - t) + " ms (" + f + ")");

        t = System.currentTimeMillis();
        Bench b = new Bench();
        for (int i = 0; i < 300000; i++) b.inc();
        System.out.println("virt300k   " + (System.currentTimeMillis() - t) + " ms (" + b.field + ")");

        t = System.currentTimeMillis();
        int[] a = new int[4096];
        for (int r = 0; r < 100; r++)
            for (int i = 1; i < a.length; i++) a[i] = a[i - 1] + i;
        System.out.println("array400k  " + (System.currentTimeMillis() - t) + " ms (" + a[4095] + ")");

        t = System.currentTimeMillis();
        Object o = null;
        for (int i = 0; i < 50000; i++) o = new int[8];
        System.out.println("alloc50k   " + (System.currentTimeMillis() - t) + " ms");

        t = System.currentTimeMillis();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 5000; i++) { sb.append(i); if (sb.length() > 200) sb.setLength(0); }
        System.out.println("string5k   " + (System.currentTimeMillis() - t) + " ms");

        t = System.currentTimeMillis();
        double d = 0;
        for (int i = 1; i < 100000; i++) d += 1.0 / i;
        System.out.println("double100k " + (System.currentTimeMillis() - t) + " ms (" + d + ")");
    }
}
