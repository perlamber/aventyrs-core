package org.aventyrs.core.sheet;

/**
 * Tracks a directly-held temporary point count — unlike {@link ResourcePool}, which tracks
 * how much of a computed maximum has been spent, this pool's stored value IS how many
 * points are currently available: it increases when points are gained and decreases (never
 * below zero) when they're spent. Used for temporary Ego points, gained piecemeal (e.g. as
 * Narrador rewards) and spent for small, temporary advantages. Session-based recovery
 * (1 per game session) isn't tracked here — this project doesn't model game sessions.
 */
public class TemporaryPointPool {
    private int amount = 0;

    public int getAmount() {
        return amount;
    }

    public int gain(int amount) {
        return this.amount += amount;
    }

    public int spend(int amount) {
        return this.amount = Math.max(0, this.amount - amount);
    }
}
