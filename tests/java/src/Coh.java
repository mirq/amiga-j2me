/** Real PowerPC cards: where 68k-side allocations made for a PPC program land, and how fast 68k code runs when called. */
public class Coh {
    public static void main(String[] args) {
        String[] n = { "FAST", "FAST|KICK", "FAST|LOCAL", "KICK", "ANY", "PUBLIC" };
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < n.length; i++) sb.append(n[i]).append("=").append(Integer.toHexString(aj.Native.probe(33 + i))).append(' ');
        System.out.println(sb);
        System.out.println("8 x CopyMem of 2 MB on the 68k: " + aj.Native.probe(32) + " ms");
    }
}
