/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

public class AnimationController extends Object3D {
    private int activeStart, activeEnd;
    private float speed = 1, weight = 1, refSequenceTime;
    private int refWorldTime;

    public AnimationController() {}

    public void setActiveInterval(int start, int end) {
        if (start > end) throw new IllegalArgumentException();
        activeStart = start; activeEnd = end;
    }
    public int getActiveIntervalStart() { return activeStart; }
    public int getActiveIntervalEnd() { return activeEnd; }

    public void setSpeed(float speed, int worldTime) {
        refSequenceTime = getPosition(worldTime);
        refWorldTime = worldTime;
        this.speed = speed;
    }
    public float getSpeed() { return speed; }

    public void setPosition(float sequenceTime, int worldTime) {
        refSequenceTime = sequenceTime;
        refWorldTime = worldTime;
    }
    public float getPosition(int worldTime) { return refSequenceTime + speed * (worldTime - refWorldTime); }
    public int getRefWorldTime() { return refWorldTime; }

    public void setWeight(float w) {
        if (w < 0) throw new IllegalArgumentException();
        weight = w;
    }
    public float getWeight() { return weight; }

    boolean isActive(int worldTime) {
        return activeStart == activeEnd || (worldTime >= activeStart && worldTime < activeEnd);
    }
}
