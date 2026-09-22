package org.aventyrs.core.sheet;

import lombok.Getter;
import lombok.NonNull;

/**
 * An {@link Enchantment} compelling its holder to spend their first attack of each Rodada on
 * whoever cast it, and halving the damage of any later attack that Rodada against somebody else —
 * {@code AbencoadoPelaLuzAbility#ORGULHO_ELDURIANO}'s effect, held by <b>the enemy it binds</b>.
 *
 * <p><b>It lives on the recipient, not on the Scene.</b> An Aura is what <i>casts</i> it — see
 * {@code scene.ActiveAura} — as foes enter its radius through movement or a teleport; what the
 * Aura hands each of them is one of these, applied through {@code
 * CombatantSheet#applyEnchantment} so the recipient's own immunity and Duração modifiers decide
 * what actually takes hold. Two consequences worth stating: an immune foe simply never receives
 * one, and each bound foe counts down their own Duração from when they were caught rather than
 * sharing the Aura's.
 *
 * <p>Always harmful — it is a compulsion laid on an enemy, which is exactly what {@code
 * SantoAbility#PROTECAO_UNGIDA}'s "efeitos nocivos de Encantamentos" halves.
 *
 * <p>Not cumulative: a second Aura catching an already-bound foe replaces the first rather than
 * compelling them twice. <b>Simplification</b> — the rules say nothing about competing
 * provocations, and the previous Scene-side model resolved the same ambiguity the opposite way
 * (first registered won). Replacing is the better reading of a fresh Encantamento landing, but it
 * is a reading.
 */
@Getter
public class ForcedTargeting extends TemporaryEffect implements Enchantment {

    /** {@link #getLastRoundAttackedEnchanter()}'s "has not attacked them yet" value. */
    static final int NEVER = -1;

    private final CombatantSheet enchanter;

    private int lastRoundAttackedEnchanter = NEVER;

    public ForcedTargeting(@NonNull final CombatantSheet enchanter, final Integer remainingRounds) {
        super(remainingRounds);
        this.enchanter = enchanter;
    }

    /** A compulsion laid on an enemy is always an efeito nocivo. */
    @Override
    public boolean isHarmful() {
        return true;
    }

    @Override
    public boolean isCumulative() {
        return false;
    }

    /** Whether the holder has already paid their first attack of round to the enchanter. */
    public boolean hasAttackedEnchanterIn(final int round) {
        return lastRoundAttackedEnchanter == round;
    }

    /**
     * Notes that the holder attacked defender in round; only an attack on the enchanter counts,
     * since that is the only one that discharges the compulsion.
     */
    public void recordAttack(final CombatantSheet defender, final int round) {
        if (defender != null && enchanter.getId().equals(defender.getId())) {
            lastRoundAttackedEnchanter = round;
        }
    }

    /** Whether an attack on defender deals Meio-Dano — a later one, aimed away from the enchanter. */
    public boolean halvesDamageAgainst(final CombatantSheet defender, final int round) {
        return hasAttackedEnchanterIn(round) && !enchanter.getId().equals(defender.getId());
    }
}
