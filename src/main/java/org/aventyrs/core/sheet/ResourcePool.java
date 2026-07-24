package org.aventyrs.core.sheet;

/**
 * Tracks how much of a resource (Hit Points, Magic Points, Determination Points) has been
 * spent so far. Recovering — via Rest or any other effect — reduces that amount, never
 * below zero; spending only ever increases it, so a resource can go "into the negative"
 * relative to its (externally computed) maximum, same as damage exceeding max Hit Points.
 */
public class ResourcePool {
    private int spent = 0;

    public int getSpent() {
        return spent;
    }

    public int spend(int amount) {
        return spent += amount;
    }

    public int recover(int amount) {
        return spent = Math.max(0, spent - amount);
    }
}
