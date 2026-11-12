import java.util.Vector;

/** Forced full collections with live data of several kinds. */
public class GcTest {
    public static void main(String[] args) {
        System.out.println("start free " + Runtime.getRuntime().freeMemory());
        System.gc();
        System.out.println("gc 1 ok");
        Vector v = new Vector();
        for (int i = 0; i < 2000; i++) {
            v.addElement(new int[(i % 50) + 1]);
            v.addElement("s" + i);
            if (i % 3 == 0) v.removeElementAt(0);
        }
        System.gc();
        System.out.println("gc 2 ok, kept " + v.size());
        Thread t = new Thread() { public void run() { System.gc(); System.out.println("gc in thread ok"); } };
        t.start();
        try { t.join(); } catch (InterruptedException e) { }
        long sum = 0;
        for (int i = 0; i < v.size(); i++) {
            Object o = v.elementAt(i);
            sum += o instanceof int[] ? ((int[]) o).length : ((String) o).length();
        }
        System.gc();
        System.out.println("gc 3 ok, sum " + sum + " free " + (Runtime.getRuntime().freeMemory() > 0));
    }
}
