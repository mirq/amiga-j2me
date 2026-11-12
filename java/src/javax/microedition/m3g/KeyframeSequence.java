/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

public class KeyframeSequence extends Object3D {
    public static final int LINEAR = 176, SLERP = 177, SPLINE = 178, SQUAD = 179, STEP = 180;
    public static final int CONSTANT = 192, LOOP = 193;

    private final int count, comps, interpolation;
    private final int[] times;
    private final float[] values;
    private int duration, repeatMode = CONSTANT, first, last;

    public KeyframeSequence(int numKeyframes, int numComponents, int interpolation) {
        if (numKeyframes < 1 || numComponents < 1 || interpolation < LINEAR || interpolation > STEP) {
            throw new IllegalArgumentException();
        }
        if ((interpolation == SLERP || interpolation == SQUAD) && numComponents != 4) throw new IllegalArgumentException();
        count = numKeyframes; comps = numComponents; this.interpolation = interpolation;
        times = new int[count];
        values = new float[count * comps];
        last = count - 1;
    }

    public void setKeyframe(int index, int time, float[] value) {
        if (index < 0 || index >= count) throw new IndexOutOfBoundsException();
        if (value.length < comps || time < 0) throw new IllegalArgumentException();
        times[index] = time;
        System.arraycopy(value, 0, values, index * comps, comps);
        if (interpolation == SLERP || interpolation == SQUAD) {       // keep quaternions unit length
            int o = index * 4;
            float n = (float) Math.sqrt(values[o] * values[o] + values[o + 1] * values[o + 1]
                                        + values[o + 2] * values[o + 2] + values[o + 3] * values[o + 3]);
            if (n > 0) for (int i = 0; i < 4; i++) values[o + i] /= n;
        }
    }

    public void setValidRange(int first, int last) {
        if (first < 0 || first >= count || last < 0 || last >= count) throw new IndexOutOfBoundsException();
        this.first = first; this.last = last;
    }
    public void setDuration(int d) {
        if (d <= 0) throw new IllegalArgumentException();
        duration = d;
    }
    public int getDuration() { return duration; }
    public void setRepeatMode(int mode) {
        if (mode != CONSTANT && mode != LOOP) throw new IllegalArgumentException();
        repeatMode = mode;
    }
    public int getRepeatMode() { return repeatMode; }
    public int getComponentCount() { return comps; }
    public int getKeyframeCount() { return count; }
    public int getInterpolationType() { return interpolation; }
    public int getValidRangeFirst() { return first; }
    public int getValidRangeLast() { return last; }
    public int getKeyframe(int index, float[] value) {
        if (index < 0 || index >= count) throw new IndexOutOfBoundsException();
        if (value != null) System.arraycopy(values, index * comps, value, 0, comps);
        return times[index];
    }

    /** The value at a sequence time. SPLINE is drawn as LINEAR, SQUAD as SLERP. */
    void sample(float time, float[] out) {
        // the keyframes in use, in order: first..last, wrapping round when first > last
        int n = first <= last ? last - first + 1 : count - first + last + 1;
        if (repeatMode == LOOP && duration > 0) {
            time = time % duration;
            if (time < 0) time += duration;
        }
        int a = first, b = first;
        int ta = times[first];
        if (time <= ta) {
            if (repeatMode == LOOP && duration > 0 && n > 1) {       // before the first key: from the last, wrapped
                a = (first + n - 1) % count; b = first;
                float span = times[b] + duration - times[a];
                interpolate(a, b, span <= 0 ? 0 : (time + duration - times[a]) / span, out);
                return;
            }
            System.arraycopy(values, first * comps, out, 0, comps);
            return;
        }
        for (int i = 1; i < n; i++) {
            b = (first + i) % count;
            if (time < times[b]) {
                float span = times[b] - times[a];
                interpolate(a, b, span <= 0 ? 0 : (time - times[a]) / span, out);
                return;
            }
            a = b;
        }
        if (repeatMode == LOOP && duration > 0 && n > 1) {           // after the last key: towards the first
            b = first;
            float span = times[b] + duration - times[a];
            interpolate(a, b, span <= 0 ? 0 : (time - times[a]) / span, out);
            return;
        }
        System.arraycopy(values, a * comps, out, 0, comps);
    }

    private void interpolate(int a, int b, float s, float[] out) {
        int oa = a * comps, ob = b * comps;
        if (interpolation == STEP) {
            System.arraycopy(values, oa, out, 0, comps);
        } else if (interpolation == SLERP || interpolation == SQUAD) {
            float dot = values[oa] * values[ob] + values[oa + 1] * values[ob + 1]
                      + values[oa + 2] * values[ob + 2] + values[oa + 3] * values[ob + 3];
            float sign = 1;
            if (dot < 0) { dot = -dot; sign = -1; }
            float wa = 1 - s, wb = s;
            if (dot < 0.9995f) {
                double theta = M3GMath.acos(dot), sin = Math.sin(theta);
                wa = (float) (Math.sin((1 - s) * theta) / sin);
                wb = (float) (Math.sin(s * theta) / sin);
            }
            for (int i = 0; i < 4; i++) out[i] = wa * values[oa + i] + sign * wb * values[ob + i];
        } else {
            for (int i = 0; i < comps; i++) out[i] = values[oa + i] + (values[ob + i] - values[oa + i]) * s;
        }
    }
}
