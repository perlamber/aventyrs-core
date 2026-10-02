package org.aventyrs.core.sheet;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * A CombatantSheet-held effect that counts down in Rodadas and expires once its
 * remaining Rodadas run out — the shared shape behind {@link TemporaryBonus} (a
 * bonus/malus granted by another Character's action), {@link Bleeding} (Sangramento's own
 * ongoing PV loss), {@link ManaDrain} (Purga-Mana's own ongoing PM loss) and {@link
 * Regeneration} (Regeneração Reativa's ongoing PV recovery). Registered
 * via {@link CombatantSheet#applyEffect(TemporaryEffect)} (or the {@link
 * TemporaryBonus}-specific {@link CombatantSheet#grantTemporaryBonus} convenience);
 * {@link CombatantSheet#tickTemporaryEffects()} — called once per Rodada by {@link
 * CombatantSheet#finishTurn()} — advances every held one by one Rodada and discards any
 * that expire as a result.
 *
 * <p>One {@linkplain #countsDownAtTurnStart() counting down at Turn start} is instead advanced by
 * {@link CombatantSheet#startTurn(int)}: "por 1 Rodada" on something done on the holder's own Turn
 * lasts until their Turn begins in the next Rodada (table ruling) — ticked at Turn end, it would
 * lapse before anyone else acted.
 *
 * <p>{@code remainingRounds} is {@code null} for an open-ended effect — one that never
 * expires from ticking alone (see {@link Bleeding}'s own javadoc for why Sangramento
 * Maior needs this). Every {@link TemporaryBonus} always has a concrete count.
 *
 * <p>{@link #isCumulative()} governs whether more than one instance of a given concrete
 * kind can be active on the same {@link CombatantSheet} at once — true by default, since
 * neither {@link Bleeding} nor {@link ManaDrain}'s own rules text says otherwise (repeated
 * critical hits are expected to stack). {@link Withering} (Definhar's own ongoing curse
 * damage) is the first to override it, per its rules text's explicit "não cumulativo".
 */
@Getter
public abstract class TemporaryEffect {

    /** {@link #maximumSimultaneous()}'s "no ceiling at all" answer. */
    static final int UNLIMITED_SIMULTANEOUS = Integer.MAX_VALUE;

    private Integer remainingRounds;

    @Getter(AccessLevel.NONE)
    private final boolean countsDownAtTurnStart;

    protected TemporaryEffect(final Integer remainingRounds) {
        this(remainingRounds, false);
    }

    protected TemporaryEffect(final Integer remainingRounds, final boolean countsDownAtTurnStart) {
        this.remainingRounds = remainingRounds;
        this.countsDownAtTurnStart = countsDownAtTurnStart;
    }

    /**
     * Whether this effect counts down at the start of its holder's Turn rather than at its end —
     * advanced by {@link CombatantSheet#startTurn(int)}, skipped by {@link
     * CombatantSheet#tickTemporaryEffects()}.
     */
    public boolean countsDownAtTurnStart() {
        return countsDownAtTurnStart;
    }

    /**
     * Shortens this effect's remaining Duração — package-private. Used at the moment an effect is applied, and
     * once mid-countdown: {@code CombatantSheet#halveEffectDuration}, Autocontrole's temporary point (core 0.0.81),
     * the one thing the rules let halve a running one.
     *
     * <p>{@code CombatantSheet#applyEnchantment} is the one caller: a harmful Encantamento landing
     * on someone warded by an Armadura and an Escudo Ungido arrives already halved. Deliberately
     * not public — "a Duração … é reduzida pela metade" is a property of the effect that lands, and letting anything
     * but that Ego point halve a running one would make every countdown in this core a moving target.
     */
    /**
     * Starts a countdown on an effect that had none — a {@link Sustained} effect's trailing Rodadas once its caster's
     * focus breaks (core 0.0.98). For a subclass in any package; the halving below stays package-private.
     */
    protected final void startCountdown(final int rounds) {
        this.remainingRounds = rounds;
    }

    void shortenTo(final int rounds) {
        this.remainingRounds = rounds;
    }

    /** True once a finite effect has no Rodadas left — an open-ended one never expires this way. */
    public boolean isExpired() {
        return remainingRounds != null && remainingRounds <= 0;
    }

    /**
     * Whether more than one instance of this concrete kind can be active on the same
     * CombatantSheet at once. True by default; override to return false for an effect
     * whose own rules text is explicitly "não cumulativo" — {@link
     * CombatantSheet#applyEffect} then replaces any existing instance of the same
     * concrete type instead of adding another.
     */
    boolean isCumulative() {
        return true;
    }

    /**
     * How many instances of this concrete kind may be active on one {@link CombatantSheet} at
     * once — the same question {@link #isCumulative()} asks, answered as a number rather than a
     * flag, and derived from it by default so no existing effect changes behaviour: a cumulative
     * one is {@link #UNLIMITED_SIMULTANEOUS}, a non-cumulative one exactly 1.
     *
     * <p>Two overriders. {@link TemporaryBonus} returns 1 once it carries a source, which is the
     * "a Blessing does not accumulate with another from the same source, it renews it" rule; {@link
     * Regeneration} returns a ceiling that is neither 1 nor unbounded, since {@code
     * TrollFeat#REGENERACAO_REATIVA_SUPERIOR} raises it to "1+ número de Títulos Aventyr
     * Despertos". {@link CombatantSheet#applyEffect} trims the oldest of the same grant — same
     * concrete kind, same {@link #stackingKey()} — until the new one fits.
     */
    int maximumSimultaneous() {
        return isCumulative() ? UNLIMITED_SIMULTANEOUS : 1;
    }

    /**
     * What makes two of these "the same grant" for {@link CombatantSheet#applyEffect}'s ceiling —
     * compared alongside the concrete class, so only effects agreeing on both displace each other.
     * {@code null} by default, which is every effect whose kind alone identifies it.
     *
     * <p>{@link TemporaryBonus} overrides it with source <em>and</em> {@link
     * org.aventyrs.core.modifier.ModifierType}, because one trait may grant several different
     * bonuses at once and they must not evict one another: {@code AnaoFeat#VIGOR_DO_INVERNO} hands
     * its holder RD and Resistência a Críticos in the same breath.
     */
    Object stackingKey() {
        return null;
    }

    /** Counts down one Rodada; a no-op for an open-ended effect. */
    void tick() {
        if (remainingRounds != null) {
            remainingRounds--;
        }
    }

    /**
     * Applies this effect's own per-Rodada side effect (if any) to {@code sheet} —
     * {@link Bleeding}/{@link ManaDrain} draining PV/PM. No-op by default, e.g. {@link
     * TemporaryBonus}, whose only behavior is decrementing toward expiry — this is what
     * lets {@link CombatantSheet#tickTemporaryEffects()} advance every kind of {@link
     * TemporaryEffect} uniformly, without needing to know which concrete kinds exist.
     */
    void applyRoundEffect(final CombatantSheet sheet) {
    }
}
