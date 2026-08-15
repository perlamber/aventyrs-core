package org.aventyrs.core.sheet;

import java.util.HashMap;
import java.util.Map;

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

    /**
     * Each non-cumulative source's own currently-held contribution to {@link #amount} — see
     * {@link #gainNonCumulative}. Absent from this map is the same as 0; a source is only
     * added the first time it grants non-cumulatively.
     */
    private final Map<Object, Integer> nonCumulativeContributions = new HashMap<>();

    public int getAmount() {
        return amount;
    }

    public int gain(int amount) {
        return this.amount += amount;
    }

    /**
     * Raises source's own contribution to at least amount, without that one source stacking
     * a second point on top of one it already granted — e.g. {@code
     * org.aventyrs.core.ability.CharismaAbility#DESTINO_FAVORAVEL}'s "não cumulativo" point:
     * repeated triggers of that same ability don't add a second point on top of one it
     * already granted. {@code source} identifies *which* ability/effect is granting (e.g. the
     * {@code AttributeAbility} constant itself) — only repeat gains from that same source are
     * capped; an unrelated source's own gain (whether via this method with a different source,
     * or a plain {@link #gain}) still adds normally on top, since only one source's own
     * repeated triggers are what "não cumulativo" refers to.
     */
    public int gainNonCumulative(Object source, int amount) {
        int previousContribution = nonCumulativeContributions.getOrDefault(source, 0);
        int newContribution = Math.max(previousContribution, amount);
        nonCumulativeContributions.put(source, newContribution);
        return this.amount += newContribution - previousContribution;
    }

    public int spend(int amount) {
        return this.amount = Math.max(0, this.amount - amount);
    }
}
