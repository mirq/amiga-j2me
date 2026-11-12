/** Timing shapes to find what is slow: args = iterations (default 100000). */
public class Loop {
    static int sf;
    int f;
    static int id(int x) { return x; }
    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 100000;
        long t = System.currentTimeMillis();
        int s = 0;
        for (int i = 0; i < n; i++) { s += i; }
        long t1 = System.currentTimeMillis();
        System.out.println("locals only      " + (t1 - t) + " ms");
        // the same amount of work in one straight line of code, a branch only every 16 adds
        t = System.currentTimeMillis();
        for (int i = 0; i < n; i += 16) { s += i; s += i; s += i; s += i; s += i; s += i; s += i; s += i; s += i; s += i; s += i; s += i; s += i; s += i; s += i; s += i; }
        t1 = System.currentTimeMillis();
        System.out.println("few branches     " + (t1 - t) + " ms");
        t = System.currentTimeMillis();
        for (int i = 0; i < n; i++) { sf += i; }
        t1 = System.currentTimeMillis();
        System.out.println("static field     " + (t1 - t) + " ms");
        t = System.currentTimeMillis();
        for (int i = 0; i < n; i++) { s += id(i); }
        t1 = System.currentTimeMillis();
        System.out.println("static call      " + (t1 - t) + " ms (" + s + ")");
    }
}
