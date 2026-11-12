/** args: two files. Says whether they are the same, and the first line that differs. */
public class CmpFiles {
    public static void main(String[] args) {
        byte[] a = aj.Native.readFile(args[0]), b = aj.Native.readFile(args[1]);
        if (a == null || b == null) { System.out.println("cannot read " + (a == null ? args[0] : args[1])); return; }
        int n = Math.min(a.length, b.length), line = 1;
        for (int i = 0; i < n; i++) {
            if (a[i] != b[i]) {
                int s = i, e = i;
                while (s > 0 && a[s - 1] != '\n') s--;
                while (e < a.length && a[e] != '\n') e++;
                System.out.println("differ at line " + line + ": [" + new String(a, s, e - s) + "]");
                return;
            }
            if (a[i] == '\n') line++;
        }
        System.out.println(a.length == b.length ? "same: " + a.length + " bytes, " + (line - 1) + " lines" : "one file is longer");
    }
}
