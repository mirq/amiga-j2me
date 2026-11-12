/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.nokia.mid.sound;

import aj.Native;
import aj.media.Smf;

/** Nokia Sound API: ringing tones (played by the MIDI synthesiser) and WAV. */
public class Sound {
    public static final int FORMAT_TONE = 1, FORMAT_WAV = 5;
    public static final int SOUND_PLAYING = 0, SOUND_STOPPED = 1, SOUND_UNINITIALIZED = 3;

    private int state = SOUND_UNINITIALIZED;
    private int gain = 255;
    private int handle;
    private SoundListener listener;

    public Sound(byte[] data, int type) { init(data, type); }
    public Sound(int freq, long duration) { init(freq, duration); }

    public static int getConcurrentSoundCount(int type) { return 4; }
    public static int[] getSupportedFormats() { return new int[] { FORMAT_TONE, FORMAT_WAV }; }

    private void load(byte[] d) {
        release();
        handle = d == null ? 0 : Native.sndCreate(d, 0, d.length);
        setState(SOUND_STOPPED);
    }

    public void init(byte[] data, int type) {
        if (data == null) throw new NullPointerException();
        if (type != FORMAT_TONE && type != FORMAT_WAV) throw new IllegalArgumentException();
        load(type == FORMAT_TONE ? Smf.fromOtt(data) : data);
    }

    public void init(int freq, long duration) {
        if (duration <= 0 || freq < 0) throw new IllegalArgumentException();
        load(Smf.tone(Smf.noteOfFrequency(freq), (int) duration, 100));
    }

    private void setState(int s) {
        if (state != s) {
            state = s;
            SoundListener l = listener;
            if (l != null) l.soundStateChanged(this, s);
        }
    }

    /** loop: number of times to play, 0 = until stopped */
    public void play(int loop) {
        if (state == SOUND_UNINITIALIZED) return;
        if (handle != 0) {
            Native.sndRewind(handle);
            Native.sndPlay(handle, loop <= 0 ? -1 : loop, gain * 100 / 255);
        }
        setState(SOUND_PLAYING);
        if (handle == 0) setState(SOUND_STOPPED);
    }

    public void stop() {
        if (handle != 0) Native.sndStop(handle);
        if (state == SOUND_PLAYING) setState(SOUND_STOPPED);
    }

    public void resume() {
        if (state == SOUND_STOPPED && handle != 0) {
            Native.sndPlay(handle, 1, gain * 100 / 255);
            setState(SOUND_PLAYING);
        }
    }

    public void release() {
        if (handle != 0) { Native.sndClose(handle); handle = 0; }
        setState(SOUND_UNINITIALIZED);
    }

    public int getState() {
        if (state == SOUND_PLAYING && handle != 0 && Native.sndPlaying(handle) == 0) setState(SOUND_STOPPED);
        return state;
    }

    public void setGain(int g) {
        gain = g < 0 ? 0 : (g > 255 ? 255 : g);
        if (handle != 0) Native.sndVolume(handle, gain * 100 / 255);
    }
    public int getGain() { return gain; }
    public void setSoundListener(SoundListener l) { listener = l; }
}
