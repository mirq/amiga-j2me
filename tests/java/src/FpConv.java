/** Conversions between floating point and integers; the output must match on every machine. */
public class FpConv {
    public static void main(String[] args) {
        float[] f = { 0f, 0.5f, -0.5f, 1.25f, 2.5f, 3.75f, -3.75f, 1e9f, -1e9f, 3e9f, 1e18f, -1e18f, 9.9e18f, 1e30f, -1e30f,
                      0.999999f, 16777217f, 2147483520f, -2147483648f, Float.NaN, Float.POSITIVE_INFINITY, 4.5f, 5.5f, -0.99f };
        double[] d = { 0, 0.5, -0.5, 1.25, 2.5, 3.75, -3.75, 1e9, -1e9, 3e9, 1e18, -1e18, 9.2e18, 1e30, -1e30, 0.9999999999,
                       4503599627370497.5, 2147483647.9, -2147483648.9, Double.NaN, Double.NEGATIVE_INFINITY, 4.5, 5.5, 123456789012.75 };
        long[] l = { 0, 1, -1, 16777217L, 9007199254740993L, -9007199254740993L, Long.MAX_VALUE, Long.MIN_VALUE, 123456789012345L,
                     4611686018427387905L, 0x7FFFFF8000000001L };
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < f.length; i++) sb.append((int) f[i]).append(',').append((long) f[i]).append(' ');
        System.out.println("f2i,f2l " + sb);
        sb.setLength(0);
        for (int i = 0; i < d.length; i++) sb.append((int) d[i]).append(',').append((long) d[i]).append(' ');
        System.out.println("d2i,d2l " + sb);
        sb.setLength(0);
        for (int i = 0; i < l.length; i++) {
            sb.append(Integer.toHexString(Float.floatToIntBits((float) l[i]))).append(',')
              .append(Long.toString(Double.doubleToLongBits((double) l[i]), 16)).append(' ');
        }
        System.out.println("l2f,l2d " + sb);
        sb.setLength(0);
        int[] iv = { 0, 1, -1, 16777217, 2147483647, -2147483648, 123456789 };
        for (int i = 0; i < iv.length; i++) {
            sb.append(Integer.toHexString(Float.floatToIntBits((float) iv[i]))).append(',')
              .append(Long.toString(Double.doubleToLongBits((double) iv[i]), 16)).append(' ');
        }
        System.out.println("i2f,i2d " + sb);
        sb.setLength(0);
        for (int i = 0; i < d.length; i++) sb.append(Integer.toHexString(Float.floatToIntBits((float) d[i]))).append(' ');
        System.out.println("d2f " + sb);
        double x = 7.5, y = 2.0; float fx = 7.5f, fy = 2f;
        System.out.println("rem " + (x % y) + " " + (-x % y) + " " + (fx % fy) + " " + (1e10 % 7.3) + " " + (5.5 % 0.0) + " " + (Double.POSITIVE_INFINITY % 2));
    }
}
