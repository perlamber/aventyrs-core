package org.aventyrs.core.sheet;

import lombok.Getter;

/**
 * A CombatantSheet-held effect that counts down in Rodadas and expires once its
 * remaining Rodadas run out — the shared shape behind {@link TemporaryBonus} (a
 * bonus/malus granted by another Character's action), {@link Bleeding} (Sangramento's own
 * ongoing PV loss), and {@link ManaDrain} (Purga-Mana's own ongoing PM loss). Registered
 * via {@link CombatantSheet#applyEffect(TemporaryEffect)} (or the {@link
 * TemporaryBonus}-specific {@link CombatantSheet#grantTemporaryBonus} convenience);
 * {@link CombatantSheet#tickTemporaryEffects()} — called once per Rodada by {@link
 * CombatantSheet#finishTurn()} — advances every held one by one Rodada and discards any
 * that expire as a result.
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
    private Integer remainingRounds;

    protected TemporaryEffect(final Integer remainingRounds) {
        this.remainingRounds = remainingRounds;
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
