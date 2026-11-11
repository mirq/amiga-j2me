/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package java.util;

public abstract class TimerTask implements Runnable {
    long nextTime;          // next execution, ms
    long period;            // 0 = one shot
    boolean fixedRate;
    boolean cancelled;
    boolean scheduled;
    long lastScheduled;

    protected TimerTask() {}

    public abstract void run();

    public boolean cancel() {
        boolean wasPending = scheduled && !cancelled;
        cancelled = true;
        return wasPending;
    }

    public long scheduledExecutionTime() { return lastScheduled; }
}
