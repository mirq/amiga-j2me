/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

public class Command {
    public static final int SCREEN = 1, BACK = 2, CANCEL = 3, OK = 4, HELP = 5, STOP = 6, EXIT = 7, ITEM = 8;

    private final String label, longLabel;
    private final int type, priority;

    public Command(String label, int commandType, int priority) {
        this(label, null, commandType, priority);
    }

    public Command(String shortLabel, String longLabel, int commandType, int priority) {
        if (shortLabel == null) throw new NullPointerException();
        if (commandType < SCREEN || commandType > ITEM) throw new IllegalArgumentException();
        this.label = shortLabel; this.longLabel = longLabel;
        this.type = commandType; this.priority = priority;
    }

    public String getLabel() { return label; }
    public String getLongLabel() { return longLabel; }
    public int getCommandType() { return type; }
    public int getPriority() { return priority; }

    /** BACK, CANCEL, EXIT and STOP go on the right soft key. */
    boolean isNegative() { return type == BACK || type == CANCEL || type == EXIT || type == STOP; }
}
