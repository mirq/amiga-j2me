import javax.microedition.lcdui.Image;

/** Times what Skoki 2 does when the start light turns green: 15 PNGs from the
 *  game's JAR, then System.gc(). Run with the game's JAR on the classpath. */
public class ImgTime {
    static final String[] NAMES = { "tlo", "finish", "tree", "skoczek", "lnarta", "pnarta", "bar",
        "budka", "cien", "prog", "arrow", "tree2", "kamien", "zaspa", "balon" };
    public static void main(String[] a) throws Exception {
        for (int round = 0; round < 3; round++) {
            long t0 = System.currentTimeMillis();
            Image[] im = new Image[NAMES.length];
            long worst = 0; String worstName = "";
            for (int i = 0; i < NAMES.length; i++) {
                long s = System.currentTimeMillis();
                im[i] = Image.createImage("/data/" + NAMES[i] + ".png");
                long d = System.currentTimeMillis() - s;
                if (d > worst) { worst = d; worstName = NAMES[i] + " " + im[i].getWidth() + "x" + im[i].getHeight(); }
            }
            long t1 = System.currentTimeMillis();
            System.gc();
            long t2 = System.currentTimeMillis();
            System.out.println("images " + (t1 - t0) + " ms (slowest " + worstName + " " + worst + " ms), gc " + (t2 - t1) + " ms");
        }
    }
}
