/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package java.util;

/** One thread per Timer, tasks kept sorted by their next execution time. */
public class Timer {
    private final Vector tasks = new Vector();
    private boolean cancelled;
    private final Thread thread;

    public Timer() {
        thread = new Thread(new Runnable() {
            public void run() { loop(); }
        });
        thread.start();
    }

    public void cancel() {
        synchronized (tasks) {
            cancelled = true;
            tasks.removeAllElements();
            tasks.notifyAll();
        }
    }

    public void schedule(TimerTask task, long delay) { add(task, System.currentTimeMillis() + check(delay), 0, false); }
    public void schedule(TimerTask task, Date time) { add(task, time.getTime(), 0, false); }
    public void schedule(TimerTask task, long delay, long period) {
        add(task, System.currentTimeMillis() + check(delay), checkPeriod(period), false);
    }
    public void schedule(TimerTask task, Date firstTime, long period) {
        add(task, firstTime.getTime(), checkPeriod(period), false);
    }
    public void scheduleAtFixedRate(TimerTask task, long delay, long period) {
        add(task, System.currentTimeMillis() + check(delay), checkPeriod(period), true);
    }
    public void scheduleAtFixedRate(TimerTask task, Date firstTime, long period) {
        add(task, firstTime.getTime(), checkPeriod(period), true);
    }

    private static long check(long delay) {
        if (delay < 0) throw new IllegalArgumentException();
        return delay;
    }
    private static long checkPeriod(long period) {
        if (period <= 0) throw new IllegalArgumentException();
        return period;
    }

    private void add(TimerTask task, long when, long period, boolean fixedRate) {
        synchronized (tasks) {
            if (cancelled) throw new IllegalStateException("Timer cancelled");
            if (task.scheduled || task.cancelled) throw new IllegalStateException("Task already scheduled or cancelled");
            task.scheduled = true;
            task.nextTime = when; task.period = period; task.fixedRate = fixedRate;
            insert(task);
            tasks.notifyAll();
        }
    }

    private void insert(TimerTask task) {
        int i = 0;
        while (i < tasks.size() && ((TimerTask) tasks.elementAt(i)).nextTime <= task.nextTime) i++;
        tasks.insertElementAt(task, i);
    }

    private void loop() {
        for (;;) {
            TimerTask task;
            synchronized (tasks) {
                for (;;) {
                    if (cancelled) return;
                    if (tasks.size() == 0) {
                        try { tasks.wait(); } catch (InterruptedException e) { }
                        continue;
                    }
                    task = (TimerTask) tasks.elementAt(0);
                    if (task.cancelled) { tasks.removeElementAt(0); continue; }
                    long wait = task.nextTime - System.currentTimeMillis();
                    if (wait > 0) {
                        try { tasks.wait(wait); } catch (InterruptedException e) { }
                        continue;
                    }
                    tasks.removeElementAt(0);
                    task.lastScheduled = task.nextTime;
                    if (task.period > 0) {
                        task.nextTime = (task.fixedRate ? task.nextTime : System.currentTimeMillis()) + task.period;
                        insert(task);
                    }
                    break;
                }
            }
            try {
                task.run();
            } catch (Throwable t) {
                System.out.println("TimerTask: " + t);
                t.printStackTrace();
            }
        }
    }
}
