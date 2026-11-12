/** Real PowerPC cards: is memory consistent between the PPC and the 68k across a call? */
public class Coh {
    public static void main(String[] args) {
        System.out.println("no flush: " + aj.Native.probe(30) + "   with flush: " + aj.Native.probe(31) + "   (0 = consistent)");
    }
}
