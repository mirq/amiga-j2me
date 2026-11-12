/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package aj.media;

import java.io.ByteArrayOutputStream;

/** Builds Standard MIDI Files (format 0) from tones, tone sequences and Nokia ringing tones. */
public final class Smf {
    private static final int DIVISION = 240;        // ticks per quarter note
    private final ByteArrayOutputStream track = new ByteArrayOutputStream(256);
    private int pending;        // ticks of silence before the next event

    private Smf(int program) {
        event(0xC0, program, -1);
    }

    private void varLen(int v) {
        if (v >= (1 << 21)) track.write(0x80 | ((v >> 21) & 0x7F));
        if (v >= (1 << 14)) track.write(0x80 | ((v >> 14) & 0x7F));
        if (v >= (1 << 7)) track.write(0x80 | ((v >> 7) & 0x7F));
        track.write(v & 0x7F);
    }

    private void event(int status, int d1, int d2) {
        varLen(pending);
        pending = 0;
        track.write(status);
        track.write(d1);
        if (d2 >= 0) track.write(d2);
    }

    private void tempo(int bpm) {
        int us = 60000000 / (bpm < 1 ? 1 : bpm);
        varLen(pending);
        pending = 0;
        track.write(0xFF); track.write(0x51); track.write(3);
        track.write(us >> 16); track.write(us >> 8); track.write(us);
    }

    /** note < 0 = silence */
    private void note(int note, int ticks, int velocity) {
        if (ticks <= 0) return;
        if (note < 0 || note > 127 || velocity <= 0) { pending += ticks; return; }
        event(0x90, note, velocity);
        pending = ticks;
        event(0x80, note, 0);
    }

    private byte[] finish() {
        varLen(pending);
        track.write(0xFF); track.write(0x2F); track.write(0);
        byte[] t = track.toByteArray();
        byte[] f = new byte[22 + t.length];
        f[0] = 'M'; f[1] = 'T'; f[2] = 'h'; f[3] = 'd'; f[7] = 6;
        f[11] = 1;                                              // format 0, one track
        f[12] = (byte) (DIVISION >> 8); f[13] = (byte) DIVISION;
        f[14] = 'M'; f[15] = 'T'; f[16] = 'r'; f[17] = 'k';
        f[18] = (byte) (t.length >> 24); f[19] = (byte) (t.length >> 16);
        f[20] = (byte) (t.length >> 8); f[21] = (byte) t.length;
        System.arraycopy(t, 0, f, 22, t.length);
        return f;
    }

    /** One note for durationMs. */
    public static byte[] tone(int note, int durationMs, int volume) {
        Smf s = new Smf(80);
        s.tempo(125);                                           // 1 quarter = 480 ms, 1 tick = 2 ms
        s.note(note, durationMs / 2, volume * 127 / 100);
        return s.finish();
    }

    public static int noteOfFrequency(int hz) {
        // 12 * log2(hz / 440) + 69 without floating point: walk the semitones
        int note = 69;
        long f = 440L << 16, target = (long) hz << 16;
        while (f < target && note < 127) { f = f * 105946 / 100000; note++; }
        while (f > target && note > 0) { f = f * 100000 / 105946; note--; }
        return note;
    }

    // ---- JSR-135 tone sequences ----

    /** Returns null when the sequence is malformed. */
    public static byte[] fromToneSequence(byte[] seq) {
        try {
            Smf s = new Smf(80);
            int[] blockStart = new int[128], blockEnd = new int[128];
            int pos = 0, tempo = 120, resolution = 64;
            if (seq[pos] == -2) pos += 2;                               // VERSION
            if (pos < seq.length && seq[pos] == -3) { tempo = (seq[pos + 1] & 0x7F) * 4; pos += 2; }
            if (pos < seq.length && seq[pos] == -4) { resolution = seq[pos + 1] & 0x7F; pos += 2; }
            if (resolution < 1) resolution = 64;
            s.tempo(tempo);
            // find the blocks
            for (int i = pos; i + 1 < seq.length; i += 2) {
                if (seq[i] == -5) blockStart[seq[i + 1] & 0x7F] = i + 2;
                else if (seq[i] == -6) blockEnd[seq[i + 1] & 0x7F] = i;
            }
            int[] state = { 100 };      // volume
            s.play(seq, pos, seq.length, resolution, blockStart, blockEnd, state, 0);
            return s.finish();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void play(byte[] seq, int pos, int end, int resolution, int[] bs, int[] be, int[] state, int depth) {
        int repeat = 1;
        while (pos + 1 < end) {
            int a = seq[pos], b = seq[pos + 1];
            pos += 2;
            if (a == -5) { pos = be[b & 0x7F] + 2; continue; }          // skip a block definition
            if (a == -6) continue;
            if (a == -7) {                                              // PLAY_BLOCK
                if (depth < 8) play(seq, bs[b & 0x7F], be[b & 0x7F], resolution, bs, be, state, depth + 1);
            } else if (a == -8) {
                state[0] = b & 0x7F;
            } else if (a == -9) {
                repeat = b & 0x7F;
            } else if (a >= -1) {
                int ticks = (b & 0x7F) * 4 * DIVISION / resolution;
                for (int i = 0; i < repeat; i++) note(a, ticks, state[0] * 127 / 100);
                repeat = 1;
            }
        }
    }

    // ---- Nokia Smart Messaging ringing tones (com.nokia.mid.sound.Sound FORMAT_TONE) ----

    private static final int[] OTT_BPM = { 25, 28, 31, 35, 40, 45, 50, 56, 63, 70, 80, 90, 100, 112, 125, 140,
        160, 180, 200, 225, 250, 285, 320, 355, 400, 450, 500, 565, 635, 715, 800, 900 };
    private byte[] bits;
    private int bitPos;

    private int get(int n) {
        int v = 0;
        for (int i = 0; i < n; i++) {
            int byteIndex = bitPos >> 3;
            int bit = byteIndex < bits.length ? (bits[byteIndex] >> (7 - (bitPos & 7))) & 1 : 0;
            v = (v << 1) | bit;
            bitPos++;
        }
        return v;
    }

    /** Returns null when the data is no ringing tone. */
    public static byte[] fromOtt(byte[] data) {
        try {
            Smf s = new Smf(80);
            s.bits = data;
            int commands = s.get(8);
            int octave = 72, volume = 100;
            boolean any = false;
            s.tempo(63);
            for (int c = 0; c < commands && s.bitPos < data.length * 8; c++) {
                int cmd = s.get(7);
                if (cmd == 0x25) continue;                  // <ringing-tone-programming>
                if (cmd != 0x1D) break;                     // only <sound> is understood
                if (s.get(3) != 1) break;                   // only the basic song type
                int titleLen = s.get(4);
                s.bitPos += titleLen * 8;
                int patterns = s.get(8);
                for (int p = 0; p < patterns; p++) {
                    s.get(3);                               // pattern header id
                    s.get(2);                               // pattern id
                    int loop = s.get(4);
                    int count = s.get(8);
                    int start = s.bitPos;
                    for (int rep = 0; rep <= (loop == 15 ? 0 : loop); rep++) {
                        s.bitPos = start;
                        for (int i = 0; i < count; i++) {
                            switch (s.get(3)) {
                            case 1: {                       // note
                                int value = s.get(4), dur = s.get(3), spec = s.get(2);
                                int ticks = (4 * DIVISION) >> dur;
                                if (spec == 1) ticks += ticks / 2;
                                else if (spec == 2) ticks += ticks * 3 / 4;
                                else if (spec == 3) ticks = ticks * 2 / 3;
                                s.note(value == 0 || value > 12 ? -1 : octave + value - 1, ticks, volume * 127 / 100);
                                any = true;
                                break;
                            }
                            case 2: octave = 60 + 12 * s.get(2); break;            // scale
                            case 3: s.get(2); break;                                // style
                            case 4: s.tempo(OTT_BPM[s.get(5)]); break;
                            case 5: volume = 20 + s.get(4) * 80 / 15; break;
                            default: i = count; break;                              // unknown: stop this pattern
                            }
                        }
                    }
                }
                break;
            }
            return any ? s.finish() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
