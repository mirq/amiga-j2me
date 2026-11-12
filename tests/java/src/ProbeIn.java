/** Runs the native timing shapes from inside the running VM. */
public class ProbeIn {
    public static void main(String[] args) {
        String[] names = { "ANY", "PUBLIC", "FAST", "FAST|KICK", "FAST|LOCAL", "KICK", "LOCAL", "PUBLIC|CLEAR", "PUBLIC|FAST|LOCAL|KICK", "ANY 9MB" };
        for (int i = 0; i < names.length; i++) System.out.println(names[i] + " -> " + Integer.toHexString(aj.Native.probe(20 + i)));
        System.out.println("bss " + Integer.toHexString(aj.Native.probe(10)) + " code " + Integer.toHexString(aj.Native.probe(11))
                           + " stack " + Integer.toHexString(aj.Native.probe(12)) + " malloc " + Integer.toHexString(aj.Native.probe(13)) + " data " + Integer.toHexString(aj.Native.probe(14)));
        System.out.println("globals " + aj.Native.probe(2) + " us, call table " + aj.Native.probe(1) + " us");
    }
}
