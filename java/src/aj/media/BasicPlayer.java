/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package aj.media;

import aj.Native;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import javax.microedition.media.Control;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.ToneControl;
import javax.microedition.media.control.VolumeControl;

/**
 * The player behind javax.microedition.media.Manager: WAV, MIDI and tone
 * sequences through the native mixer and synthesiser. The event thread polls
 * the started players (pollAll) to report the end of media.
 */
public class BasicPlayer implements Player, VolumeControl, ToneControl {
    private static final Vector started = new Vector();

    private byte[] data;
    private final String type;
    private int state = UNREALIZED;
    private int loops = 1;
    private int level = 100;
    private boolean muted;
    private int handle;         // native sound, 0 = none (silent)
    private final Vector listeners = new Vector();

    public BasicPlayer(byte[] data, String type) {
        this.data = data;
        this.type = type != null ? type : guessType(data);
    }

    public static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
        byte[] buf = new byte[2048];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        return out.toByteArray();
    }

    static String guessType(byte[] d) {
        if (d == null || d.length < 4) return "audio/x-tone-seq";
        if (d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F') return "audio/x-wav";
        if (d[0] == 'M' && d[1] == 'T' && d[2] == 'h' && d[3] == 'd') return "audio/midi";
        return "audio/x-tone-seq";
    }

    /** Manager.playTone: one note, released by itself when it has played. */
    public static void playTone(int note, int duration, int volume) {
        byte[] smf = Smf.tone(note, duration, volume);
        final int h = Native.sndCreate(smf, 0, smf.length);
        if (h == 0) return;
        Native.sndPlay(h, 1, 100);
        final int wait = duration + 600;
        new Thread(new Runnable() {
            public void run() {
                try { Thread.sleep(wait); } catch (InterruptedException e) { }
                Native.sndClose(h);
            }
        }).start();
    }

    /** Called by the event thread: players whose sound has ended. */
    public static void pollAll() {
        if (started.size() == 0) return;
        for (int i = started.size() - 1; i >= 0; i--) {
            BasicPlayer p = (BasicPlayer) started.elementAt(i);
            if (p.state == STARTED && (p.handle == 0 || Native.sndPlaying(p.handle) == 0)) {
                started.removeElementAt(i);
                p.state = PREFETCHED;
                p.post(PlayerListener.END_OF_MEDIA, new Long(p.getDuration()));
            }
        }
    }

    private void post(String event, Object eventData) {
        for (int i = 0; i < listeners.size(); i++) {
            try {
                ((PlayerListener) listeners.elementAt(i)).playerUpdate(this, event, eventData);
            } catch (Throwable t) {
                // a listener's error is its own
            }
        }
    }

    private void checkOpen() {
        if (state == CLOSED) throw new IllegalStateException("player closed");
    }

    public void realize() throws MediaException {
        checkOpen();
        if (state < REALIZED) state = REALIZED;
    }

    public void prefetch() throws MediaException {
        realize();
        if (state >= PREFETCHED) return;
        if (handle == 0 && data != null) {
            byte[] d = data;
            if ("audio/x-tone-seq".equals(type)) d = Smf.fromToneSequence(data);
            if (d != null) handle = Native.sndCreate(d, 0, d.length);
        }
        state = PREFETCHED;
    }

    public void start() throws MediaException {
        prefetch();
        if (state == STARTED) return;
        state = STARTED;
        if (handle != 0) Native.sndPlay(handle, loops, muted ? 0 : level);
        if (!started.contains(this)) started.addElement(this);
        post(PlayerListener.STARTED, new Long(getMediaTime()));
    }

    public void stop() throws MediaException {
        checkOpen();
        if (state == STARTED) {
            if (handle != 0) Native.sndStop(handle);
            started.removeElement(this);
            state = PREFETCHED;
            post(PlayerListener.STOPPED, new Long(getMediaTime()));
        }
    }

    public void deallocate() {
        checkOpen();
        if (state == STARTED) {
            try { stop(); } catch (MediaException e) { }
        }
        if (handle != 0) { Native.sndClose(handle); handle = 0; }
        if (state > REALIZED) state = REALIZED;
    }

    public void close() {
        if (state == CLOSED) return;
        started.removeElement(this);
        if (handle != 0) { Native.sndClose(handle); handle = 0; }
        state = CLOSED;
        post(PlayerListener.CLOSED, null);
    }

    public long setMediaTime(long now) throws MediaException {
        checkOpen();
        if (handle != 0 && now <= 0) Native.sndRewind(handle);     // only "back to the start" is supported
        return getMediaTime();
    }

    public long getMediaTime() {
        checkOpen();
        return handle == 0 ? 0 : Native.sndPosition(handle) * 1000L;
    }

    public int getState() { return state; }

    public long getDuration() {
        checkOpen();
        if (handle == 0) return TIME_UNKNOWN;
        int ms = Native.sndDuration(handle);
        return ms < 0 ? TIME_UNKNOWN : ms * 1000L;
    }

    public String getContentType() { checkOpen(); return type; }

    public void setLoopCount(int count) {
        if (count == 0) throw new IllegalArgumentException();
        if (state == STARTED) throw new IllegalStateException();
        loops = count;
    }

    public void addPlayerListener(PlayerListener l) { checkOpen(); if (l != null && !listeners.contains(l)) listeners.addElement(l); }
    public void removePlayerListener(PlayerListener l) { checkOpen(); listeners.removeElement(l); }

    public Control[] getControls() { return new Control[] { this }; }
    public Control getControl(String controlType) {
        if (controlType == null) throw new IllegalArgumentException();
        if (controlType.endsWith("VolumeControl")) return this;
        if (controlType.endsWith("ToneControl") && "audio/x-tone-seq".equals(type)) return this;
        return null;
    }

    public void setMute(boolean mute) {
        muted = mute;
        if (handle != 0) Native.sndVolume(handle, muted ? 0 : level);
    }
    public boolean isMuted() { return muted; }
    public int setLevel(int l) {
        level = l < 0 ? 0 : (l > 100 ? 100 : l);
        if (handle != 0) Native.sndVolume(handle, muted ? 0 : level);
        post(PlayerListener.VOLUME_CHANGED, this);
        return level;
    }
    public int getLevel() { return level; }

    public void setSequence(byte[] sequence) {
        if (sequence == null) throw new IllegalArgumentException();
        if (state >= PREFETCHED) throw new IllegalStateException();
        if (Smf.fromToneSequence(sequence) == null) throw new IllegalArgumentException("bad tone sequence");
        data = sequence;
    }
}
