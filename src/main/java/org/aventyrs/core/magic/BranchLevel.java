package org.aventyrs.core.magic;

import java.util.Optional;

/**
 * A rung of an {@link SpellTree} — both what a Magia at that depth costs to cast, and where it
 * sits in its Árvore de Magia. Ordered shallowest-to-deepest, so {@link #isAtLeast} expresses
 * "at this level or shallower" by ordinal comparison, the same way {@code
 * org.aventyrs.core.scene.Range#isWithin} already does for its own bands.
 *
 * <p>The ordering is what the acquisition gates read: {@link #isAtLeast} against the
 * character's general cap, and {@link #previous()} for the rung a Magia must already have a
 * foothold on. See {@link Spell#isEligible}.
 */
public enum BranchLevel {

    SEMENTE(0),
    BROTO(1),
    MUDA(3),
    EMERGENTE(5),
    FLORESCENTE(7);

    /** Pontos de Mana a Magia at this depth costs to cast. */
    private final int manaCost;

    BranchLevel(final int manaCost) {
        this.manaCost = manaCost;
    }

    public int getManaCost() {
        return manaCost;
    }

    /** Whether this level is other or deeper — e.g. a cap of MUDA is not at least EMERGENTE. */
    public boolean isAtLeast(final BranchLevel other) {
        return this.ordinal() >= other.ordinal();
    }

    /**
     * The rung immediately above this one, or {@link Optional#empty()} for {@link #SEMENTE},
     * which is a tree's entry point and rests on nothing. This is the level {@link
     * Spell#isEligible}'s climb gate demands a foothold at.
     */
    public Optional<BranchLevel> previous() {
        return ordinal() == 0 ? Optional.empty() : Optional.of(values()[ordinal() - 1]);
    }

    /**
     * This level advanced by steps rungs, clamped to the ladder's own ends — how {@code
     * SpellService#getMaxBranchLevel} advances a cap from {@link #SEMENTE} by whatever Talentos
     * grant. A negative or oversized step is safe.
     */
    public BranchLevel advancedBy(final int steps) {
        int target = Math.min(values().length - 1, Math.max(0, ordinal() + steps));
        return values()[target];
    }
}
