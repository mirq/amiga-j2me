public class Hello {
    static int fib(int n) { return n < 2 ? n : fib(n - 1) + fib(n - 2); }

    public static void main(String[] args) {
        System.out.println("Hello from CLDC-HI");
        long t0 = System.currentTimeMillis();
        int f = fib(25);
        long t1 = System.currentTimeMillis();
        System.out.println("fib(25)=" + f + " in " + (t1 - t0) + " ms");
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 5; i++) sb.append(i).append(',');
        System.out.println("sb=" + sb + " float=" + (1.5f * 3) + " double=" + Math.sqrt(2.0));
        int[] a = new int[100000];
        for (int i = 0; i < a.length; i++) a[i] = i * 3;
        long s = 0; for (int i = 0; i < a.length; i++) s += a[i];
        System.out.println("sum=" + s + " freeMem=" + Runtime.getRuntime().freeMemory());
        try { Object o = null; o.hashCode(); } catch (NullPointerException e) { System.out.println("NPE caught"); }
    }
}
