/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.siemens.mp.game;

import aj.media.Smf;

/** Builds a tune note by note; it is played by the MIDI synthesiser. */
public class MelodyComposer extends com.siemens.mp.misc.NativeMem {
    public static final int TONE_C0 = 0, TONE_CIS0 = 1, TONE_D0 = 2, TONE_DIS0 = 3, TONE_E0 = 4, TONE_F0 = 5,
        TONE_FIS0 = 6, TONE_G0 = 7, TONE_GIS0 = 8, TONE_A0 = 9, TONE_AIS0 = 10, TONE_H0 = 11,
        TONE_C1 = 12, TONE_CIS1 = 13, TONE_D1 = 14, TONE_DIS1 = 15, TONE_E1 = 16, TONE_F1 = 17,
        TONE_FIS1 = 18, TONE_G1 = 19, TONE_GIS1 = 20, TONE_A1 = 21, TONE_AIS1 = 22, TONE_H1 = 23,
        TONE_C2 = 24, TONE_CIS2 = 25, TONE_D2 = 26, TONE_DIS2 = 27, TONE_E2 = 28, TONE_F2 = 29,
        TONE_FIS2 = 30, TONE_G2 = 31, TONE_GIS2 = 32, TONE_A2 = 33, TONE_AIS2 = 34, TONE_H2 = 35,
        TONE_C3 = 36, TONE_CIS3 = 37, TONE_D3 = 38, TONE_DIS3 = 39, TONE_E3 = 40, TONE_F3 = 41,
        TONE_FIS3 = 42, TONE_G3 = 43, TONE_GIS3 = 44, TONE_A3 = 45, TONE_AIS3 = 46, TONE_H3 = 47,
        TONE_C4 = 48, TONE_CIS4 = 49, TONE_D4 = 50, TONE_DIS4 = 51, TONE_E4 = 52, TONE_F4 = 53,
        TONE_FIS4 = 54, TONE_G4 = 55, TONE_GIS4 = 56, TONE_A4 = 57;

    public static final int TONE_PAUSE = 58, NO_TONE = 59, TONE_STOP = 60, TONE_REPEAT = 61, TONE_REPEV = 62,
        TONE_REPON = 63, TONE_MARK = 64, TONE_REPEAT_MARK = 65, TONE_REPEV_MARK = 66, TONE_REPON_MARK = 67;
    public static final int TONELENGTH_1_1 = 0, TONELENGTH_1_2 = 1, TONELENGTH_1_4 = 2, TONELENGTH_1_8 = 3,
        TONELENGTH_1_16 = 4, TONELENGTH_1_32 = 5, TONELENGTH_1_64 = 6, TONELENGTH_DOTTED_1_1 = 7,
        TONELENGTH_DOTTED_1_2 = 8, TONELENGTH_DOTTED_1_4 = 9, TONELENGTH_DOTTED_1_8 = 10,
        TONELENGTH_DOTTED_1_16 = 11, TONELENGTH_DOTTED_1_32 = 12, TONELENGTH_DOTTED_1_64 = 13;
    public static final int BPM = 60;

    private static final int MAX = 500;
    private final int[] notes = new int[MAX], lengths = new int[MAX];
    private int count, bpm = BPM;

    public MelodyComposer() {}

    public MelodyComposer(int[] notes, int bpm) {
        if (notes.length % 2 != 0) throw new IllegalArgumentException();
        setBPM(bpm);
        for (int i = 0; i < notes.length; i += 2) appendNote(notes[i], notes[i + 1]);
    }

    public void appendNote(int note, int length) {
        if (note < 0 || note > TONE_REPON_MARK || length < 0 || length > TONELENGTH_DOTTED_1_64) throw new IllegalArgumentException();
        if (count >= MAX) throw new ArrayIndexOutOfBoundsException();
        notes[count] = note;
        lengths[count++] = length;
    }

    public void setBPM(int bpm) {
        if (bpm < 1) throw new IllegalArgumentException();
        this.bpm = bpm;
    }

    public int length() { return count; }
    public static int maxLength() { return MAX; }
    public void resetMelody() { count = 0; bpm = BPM; }

    /** Tone 0 (C0) is the C below middle C, so that A1 is 440 Hz. */
    public Melody getMelody() {
        byte[] seq = new byte[6 + count * 2];
        int n = 0, tempo = bpm / 4;
        seq[n++] = -2; seq[n++] = 1;                                    // VERSION 1
        seq[n++] = -3; seq[n++] = (byte) (tempo < 5 ? 5 : (tempo > 127 ? 127 : tempo));
        seq[n++] = -4; seq[n++] = 64;                                   // RESOLUTION: 1/64 notes
        for (int i = 0; i < count; i++) {
            int t = notes[i], l = lengths[i];
            if (t == TONE_STOP) break;
            if (t > TONE_PAUSE) continue;                               // marks and repeats: played straight through
            int d = l < 7 ? 64 >> l : (96 >> (l - 7));
            seq[n++] = (byte) (t == TONE_PAUSE ? -1 : 48 + t);
            seq[n++] = (byte) (d < 1 ? 1 : d);
        }
        byte[] used = new byte[n];
        System.arraycopy(seq, 0, used, 0, n);
        return new Melody(count == 0 ? null : Smf.fromToneSequence(used));
    }
}
