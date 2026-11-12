/** The terms of VmTest's "arrays" line one by one, to compare machines. */
public class FpBits {
    static int[] st = { 1, 2, 3, 4, 5 };
    public static void main(String[] args) {
        byte[] ba = new byte[5]; short[] sa = new short[5]; char[] ca = new char[5]; long[] la = new long[5];
        float[] fa = new float[5]; double[] da = new double[5]; boolean[] za = new boolean[5];
        for (int i = 0; i < 5; i++) {
            ba[i] = (byte) (i * 60); sa[i] = (short) (i * 9000); ca[i] = (char) (i * 15000); la[i] = i * 5000000000L;
            fa[i] = i * 1.25f; da[i] = i * 1e100; za[i] = (i & 1) == 0;
        }
        for (int i = 0; i < 5; i++) {
            System.out.println(i + ": " + ba[i] + " " + sa[i] + " " + (int) ca[i] + " " + la[i] + " " + (long) fa[i] + " "
                + (long) (da[i] / 1e99) + " " + Long.toString(Double.doubleToLongBits(da[i]), 16) + " "
                + Long.toString(Double.doubleToLongBits(da[i] / 1e99), 16) + " " + Integer.toHexString(Float.floatToIntBits(fa[i])));
        }
    }
}
