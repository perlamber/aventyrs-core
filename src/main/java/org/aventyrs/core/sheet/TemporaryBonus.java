package org.aventyrs.core.sheet;

import lombok.Getter;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.modifier.ModifierType;

/**
 * A bonus/malus a {@link CombatantSheet} is temporarily holding — granted by some other
 * Character's action (e.g. {@code ArtesCompetencyAbility#DOM_BARDICO} motivating an ally for
 * a few Rodadas), as opposed to a permanent ability of the CombatantSheet's own
 * {@link Character}. Its {@code type} reuses the existing {@link ModifierType} taxonomy —
 * this is deliberately generic, not scoped to Perícia-roll bonuses specifically, since
 * nothing about "a temporary effect from another Character" is inherently about rolls.
 *
 * <p>Counts down in Rodadas rather than storing an absolute expiry Round number, matching how
 * abilities like DOM_BARDICO describe their own duration ("por 1 Rodada", "por 2 Rodadas") —
 * see {@link TemporaryEffect}, its shared superclass with {@link Bleeding}, and {@link
 * CombatantSheet#tickTemporaryEffects()} for how the countdown advances.
 *
 * <p><b>Cumulativity is a number here, not a flag.</b> {@link #maximumSimultaneous()} answers the
 * question {@link TemporaryEffect#isCumulative()} asks, but as a count — because {@code
 * TrollFeat#REGENERACAO_REATIVA_SUPERIOR}'s "podendo somar uma quantidade de efeitos simultâneos
 * igual 1+ número de Títulos Aventyr Despertos" is a ceiling that is neither 1 nor unbounded, and a
 * boolean cannot say it. The figure comes off the {@link Blessing}, whose granting ability resolved
 * it; see {@link Blessing#getMaximumSimultaneous()} for why it lives there.
 *
 * <p><b>{@code source} is what makes two grants the same grant.</b> A bonus carrying one — i.e.
 * one built from a {@link Blessing}, via {@link CombatantSheet#grantBlessing} — does not
 * accumulate with another from the same source: {@link CombatantSheet#applyEffect} replaces the
 * one already held, which renews its duration rather than stacking a second copy. That is the
 * ordinary reading of a trait re-triggering while still running, and it is why {@link
 * #maximumSimultaneous()} is 1 for a sourced bonus. A {@code null} source — the bare {@link
 * CombatantSheet#grantTemporaryBonus} path — keeps the older unbounded behaviour, since without a
 * source there is nothing to call "the same grant" and two unrelated traits granting the same
 * {@link ModifierType} must both count.
 */
@Getter
public class TemporaryBonus extends TemporaryEffect {
    private final ModifierType type;
    private final int value;

    /** Which trait granted this, or {@code null} when it was granted without one. */
    private final String source;

    /**
     * How many of this same grant its holder may carry at once — {@link
     * Blessing#getMaximumSimultaneous()}, carried through so the ceiling a Blessing states is
     * honoured whatever its {@link ModifierType}, not only for the one kind that acts each Rodada.
     * Ignored entirely while {@link #source} is {@code null}: without a source there is no "same
     * grant" to bound.
     */
    private final int maximumSimultaneous;

    public TemporaryBonus(final ModifierType type, final int value, final int remainingRounds) {
        this(type, value, remainingRounds, null);
    }

    public TemporaryBonus(final ModifierType type, final int value, final int remainingRounds, final String source) {
        this(type, value, remainingRounds, source, Blessing.DEFAULT_MAXIMUM_SIMULTANEOUS);
    }

    /**
     * A bonus that <b>never expires from ticking</b> — {@code remainingRounds} {@code null}, the
     * open-ended shape {@link TemporaryEffect} already models. It lasts until whoever granted it
     * takes it away with {@link CombatantSheet#removeEffect}, which makes it the right shape for a
     * grant whose lifetime is a *condition* rather than a countdown: an Aura's bonus ends when the
     * recipient stops being in range, and would be wrong to let expire on its own beforehand.
     *
     * <p>Sourced deliberately — a caller that grants one owns revoking it, and the source is what
     * lets a second grant of the same Aura replace rather than stack with the first.
     */
    public static TemporaryBonus openEnded(final ModifierType type, final int value, final String source) {
        return new TemporaryBonus(type, value, null, source, Blessing.DEFAULT_MAXIMUM_SIMULTANEOUS);
    }

    public TemporaryBonus(final ModifierType type, final int value, final Integer remainingRounds,
                          final String source, final int maximumSimultaneous) {
        this(type, value, remainingRounds, source, maximumSimultaneous, false);
    }

    private TemporaryBonus(final ModifierType type, final int value, final Integer remainingRounds,
                           final String source, final int maximumSimultaneous, final boolean countsDownAtTurnStart) {
        super(remainingRounds, countsDownAtTurnStart);
        this.type = type;
        this.value = value;
        this.source = source;
        this.maximumSimultaneous = maximumSimultaneous;
    }

    /**
     * Builds the effect blessing describes. A {@link ModifierType#REGENERATION} Blessing becomes a
     * {@link Regeneration} — the one type that <em>acts</em> each Rodada instead of contributing to
     * a stat, and so needs behaviour rather than just a number; everything else is a plain bonus.
     * The branch lives here, at the single point a {@link Blessing} turns into a held effect, so no
     * granting site has to know which types are special.
     */
    static TemporaryBonus from(final Blessing blessing) {
        if (blessing.getModifierType() == ModifierType.REGENERATION) {
            return new Regeneration(blessing.getValue(), blessing.getRounds(),
                    blessing.getTotalLimit(), blessing.getSource(), blessing.getMaximumSimultaneous());
        }
        return new TemporaryBonus(blessing.getModifierType(), blessing.getValue(),
                blessing.getRounds(), blessing.getSource(), blessing.getMaximumSimultaneous(),
                blessing.isCountsDownAtTurnStart());
    }

    @Override
    int maximumSimultaneous() {
        return source == null ? UNLIMITED_SIMULTANEOUS : Math.max(1, maximumSimultaneous);
    }

    /**
     * Source <b>and</b> type: one trait granting two different bonuses at once (RD and Resistência
     * a Críticos, say) must not have the second evict the first — they are two grants that happen
     * to share an origin, not one grant re-applied.
     */
    @Override
    Object stackingKey() {
        return source == null ? null : java.util.List.of(source, type);
    }
}
