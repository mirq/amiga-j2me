/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.nokia.mid.sound;

/** Nokia Sound API. Keeps its state and reports it; playback comes with the audio layer. */
public class Sound {
    public static final int FORMAT_TONE = 1, FORMAT_WAV = 5;
    public static final int SOUND_PLAYING = 0, SOUND_STOPPED = 1, SOUND_UNINITIALIZED = 3;

    private int state = SOUND_UNINITIALIZED;
    private int gain = 255;
    private SoundListener listener;

    public Sound(byte[] data, int type) { init(data, type); }
    public Sound(int freq, long duration) { init(freq, duration); }

    public static int getConcurrentSoundCount(int type) { return 1; }
    public static int[] getSupportedFormats() { return new int[] { FORMAT_TONE, FORMAT_WAV }; }

    public void init(byte[] data, int type) {
        if (data == null) throw new NullPointerException();
        if (type != FORMAT_TONE && type != FORMAT_WAV) throw new IllegalArgumentException();
        setState(SOUND_STOPPED);
    }

    public void init(int freq, long duration) {
        if (duration <= 0) throw new IllegalArgumentException();
        setState(SOUND_STOPPED);
    }

    private void setState(int s) {
        if (state != s) {
            state = s;
            SoundListener l = listener;
            if (l != null) l.soundStateChanged(this, s);
        }
    }

    public void play(int loop) {
        if (state == SOUND_UNINITIALIZED) return;
        setState(SOUND_PLAYING);
        setState(SOUND_STOPPED);     // silent: over at once
    }

    public void stop() { if (state == SOUND_PLAYING) setState(SOUND_STOPPED); }
    public void resume() {}
    public void release() { setState(SOUND_UNINITIALIZED); }
    public int getState() { return state; }
    public void setGain(int g) { gain = g < 0 ? 0 : (g > 255 ? 255 : g); }
    public int getGain() { return gain; }
    public void setSoundListener(SoundListener l) { listener = l; }
}
