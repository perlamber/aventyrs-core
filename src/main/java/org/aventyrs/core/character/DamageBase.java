package org.aventyrs.core.character;

import org.aventyrs.core.sheet.IllegalOperationException;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_DAMAGE_BASE;

/**
 * The Dano Base of an attack — the raw dano an Equipamento deals in a given character's hands,
 * before a single bonus is added. Always {@code <dice>d6+<value>}, positioned on one shared,
 * strictly ordered scale that starts at {@link #UNARMED} (1d6+0) and is advanced one
 * <em>scale-up</em> at a time.
 *
 * <h2>The scale</h2>
 *
 * A scale-up raises {@code value} by one until it reaches {@link #MAX_VALUE}; the next one then
 * rolls that over — {@code value} resets to 0 and a die is added — until {@code diceCount}
 * reaches {@link #MAX_DICE}. Once <em>both</em> are capped the scale doesn't stop: every further
 * scale-up adds {@link #OVERFLOW_VALUE_INCREASE} to {@code value}, indefinitely.
 *
 * <pre>
 * scale  0 →  1d6+0   (UNARMED)      scale  8 →  3d6+0
 * scale  1 →  1d6+1                  scale  9 →  3d6+1
 * scale  2 →  1d6+2                  scale 10 →  3d6+2
 * scale  3 →  1d6+3                  scale 11 →  3d6+3   (both capped)
 * scale  4 →  2d6+0                  scale 12 →  3d6+5   (+2 per scale-up from here on)
 * scale  5 →  2d6+1                  scale 13 →  3d6+7
 * scale  6 →  2d6+2                  scale 14 →  3d6+9
 * scale  7 →  2d6+3                  scale 15 →  3d6+11
 * </pre>
 *
 * <h2>This is not a {@link DamageBonus}</h2>
 *
 * The two are separate stages and never merge. A scale-up is a <em>qualitative</em> step along
 * the table above — "+1 Dano Base" buys one row, which may or may not be worth a whole die —
 * whereas a {@link DamageBonus} is a flat number added to an already-rolled dano total. That's
 * why a character can sit at a Dano Base of 1d6+0 and still hit for 1d6+20: the twenty is
 * twenty points of {@link DamageBonus}, not twenty scale-ups. {@code
 * org.aventyrs.core.character.services.DamageBaseService} resolves this half only; bonuses are
 * added on top afterwards, by whoever assembles the dano.
 *
 * <p>The scale is the single canonical component — {@link #diceCount()}/{@link #value()} are
 * derived from it, never stored independently, so an unreachable pairing (2d6+7, say) simply
 * cannot be constructed. A negative scale clamps to 0: nothing sits below an unarmed strike.
 *
 * <p>This core never rolls dice (same boundary as {@code org.aventyrs.core.skill.SkillRoll}),
 * so {@link #diceCount()} is how many d6 a caller is expected to roll, and {@link #value()}
 * what to add to their total.
 */
public record DamageBase(int scale) {

    /** Every Dano Base die in this ruleset is a d6, the same die {@code SkillRoll} uses. */
    public static final int DICE_SIDES = 6;

    /** A Dano Base never grows past 3 dice; further scale-ups raise {@link #value()} instead. */
    public static final int MAX_DICE = 3;

    /** {@link #value()}'s cap while the scale is still rolling dice into {@link #diceCount()}. */
    public static final int MAX_VALUE = 3;

    /** How much a scale-up adds once both {@link #MAX_DICE} and {@link #MAX_VALUE} are reached. */
    public static final int OVERFLOW_VALUE_INCREASE = 2;

    /** How many rows one die spans before rolling over: {@code +0}, {@code +1}, {@code +2}, {@code +3}. */
    private static final int ROWS_PER_DIE = MAX_VALUE + 1;

    /** The scale at which both caps are reached (3d6+3) — every row past it adds {@link #OVERFLOW_VALUE_INCREASE}. */
    public static final int FULLY_CAPPED_SCALE = (MAX_DICE - 1) * ROWS_PER_DIE + MAX_VALUE;

    /**
     * 1d6+0 — the bottom rung, which is exactly what an Ataque Desarmado deals, and what any
     * {@link org.aventyrs.core.item.Item} that isn't a weapon reports.
     */
    public static final DamageBase UNARMED = new DamageBase(0);

    public DamageBase {
        scale = Math.max(0, scale);
    }

    /**
     * The Dano Base authored as {@code diceCount}d6+{@code value} — the form an item's own
     * rules-text column is written in. Only the pre-overflow region of the table is authorable:
     * {@code diceCount} must be 1..{@link #MAX_DICE} and {@code value} 0..{@link #MAX_VALUE},
     * throwing {@link IllegalOperationException} ({@code INVALID_DAMAGE_BASE}) otherwise — a
     * genuine system boundary, validated the same way {@code SkillRoll}'s own dice are. Rows
     * past 3d6+3 are only ever <em>reached</em>, by scaling up; nothing authors one directly.
     */
    public static DamageBase of(final int diceCount, final int value) {
        if (diceCount < 1 || diceCount > MAX_DICE || value < 0 || value > MAX_VALUE) {
            throw new IllegalOperationException(INVALID_DAMAGE_BASE);
        }
        return new DamageBase((diceCount - 1) * ROWS_PER_DIE + value);
    }

    /**
     * This Dano Base advanced by scaleUps rows — the entry point every "+N Dano Base" grant
     * lands on. A negative or oversized step is safe: the scale clamps at 0, and grows without
     * bound upward.
     */
    public DamageBase scaledUp(final int scaleUps) {
        return new DamageBase(scale + scaleUps);
    }

    /** How many d6 a caller rolls for this Dano Base. */
    public int diceCount() {
        return isFullyCapped() ? MAX_DICE : 1 + scale / ROWS_PER_DIE;
    }

    /** The flat amount added to the rolled dice — unbounded once both caps are reached. */
    public int value() {
        return isFullyCapped()
                ? MAX_VALUE + (scale - FULLY_CAPPED_SCALE) * OVERFLOW_VALUE_INCREASE
                : scale % ROWS_PER_DIE;
    }

    /** Whether this Dano Base sits at or past 3d6+3, where every further scale-up adds a flat +2. */
    public boolean isFullyCapped() {
        return scale >= FULLY_CAPPED_SCALE;
    }

    /** The usual notation, e.g. {@code "2d6+1"}. */
    @Override
    public String toString() {
        return diceCount() + "d" + DICE_SIDES + "+" + value();
    }
}
