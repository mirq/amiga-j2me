/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.media;

import aj.media.BasicPlayer;
import java.io.IOException;
import java.io.InputStream;

public final class Manager {
    public static final String TONE_DEVICE_LOCATOR = "device://tone";
    public static final String MIDI_DEVICE_LOCATOR = "device://midi";

    private Manager() {}

    public static String[] getSupportedContentTypes(String protocol) {
        return new String[] { "audio/x-wav", "audio/midi", "audio/x-tone-seq", "audio/sp-midi" };
    }

    public static String[] getSupportedProtocols(String contentType) {
        return new String[] { "device", "file" };
    }

    public static Player createPlayer(String locator) throws IOException, MediaException {
        if (locator == null) throw new IllegalArgumentException();
        if (locator.equals(TONE_DEVICE_LOCATOR)) return new BasicPlayer(null, "audio/x-tone-seq");
        if (locator.equals(MIDI_DEVICE_LOCATOR)) return new BasicPlayer(null, "audio/midi");
        InputStream in = Manager.class.getResourceAsStream(locator.startsWith("/") ? locator : "/" + locator);
        if (in == null) throw new IOException("cannot open " + locator);
        return createPlayer(in, null);
    }

    public static Player createPlayer(InputStream stream, String type) throws IOException, MediaException {
        if (stream == null) throw new IllegalArgumentException();
        return new BasicPlayer(BasicPlayer.readAll(stream), type);
    }

    public static void playTone(int note, int duration, int volume) throws MediaException {
        if (note < 0 || note > 127 || duration <= 0) throw new IllegalArgumentException();
        BasicPlayer.playTone(note, duration, volume);
    }
}
