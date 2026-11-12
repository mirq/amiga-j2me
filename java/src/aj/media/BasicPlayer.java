/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package aj.media;

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
 * The player behind javax.microedition.media.Manager: state machine, listeners,
 * volume and tone controls. Sound output is added by the audio layer.
 */
public class BasicPlayer implements Player, VolumeControl, ToneControl {
    protected byte[] data;
    protected String type;
    protected int state = UNREALIZED;
    protected int loops = 1;
    protected int level = 100;
    protected boolean muted;
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

    public static void playTone(int note, int duration, int volume) {}

    protected void post(String event, Object eventData) {
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

    public void realize() throws MediaException { checkOpen(); if (state < REALIZED) state = REALIZED; }
    public void prefetch() throws MediaException { realize(); if (state < PREFETCHED) state = PREFETCHED; }

    public void start() throws MediaException {
        prefetch();
        if (state == STARTED) return;
        state = STARTED;
        post(PlayerListener.STARTED, new Long(0));
        // silent: the media is over at once
        state = PREFETCHED;
        post(PlayerListener.END_OF_MEDIA, new Long(0));
    }

    public void stop() throws MediaException {
        checkOpen();
        if (state == STARTED) {
            state = PREFETCHED;
            post(PlayerListener.STOPPED, new Long(0));
        }
    }

    public void deallocate() { checkOpen(); if (state > REALIZED) state = REALIZED; }

    public void close() {
        if (state == CLOSED) return;
        state = CLOSED;
        post(PlayerListener.CLOSED, null);
    }

    public long setMediaTime(long now) throws MediaException { checkOpen(); return 0; }
    public long getMediaTime() { checkOpen(); return 0; }
    public int getState() { return state; }
    public long getDuration() { checkOpen(); return TIME_UNKNOWN; }
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

    public void setMute(boolean mute) { muted = mute; }
    public boolean isMuted() { return muted; }
    public int setLevel(int l) {
        level = l < 0 ? 0 : (l > 100 ? 100 : l);
        post(PlayerListener.VOLUME_CHANGED, this);
        return level;
    }
    public int getLevel() { return level; }

    public void setSequence(byte[] sequence) {
        if (sequence == null) throw new IllegalArgumentException();
        if (state >= PREFETCHED) throw new IllegalStateException();
        data = sequence;
    }
}
