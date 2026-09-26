package org.aventyrs.core.sheet;

import lombok.Getter;
import org.aventyrs.core.modifier.ModifierType;

/**
 * A {@link TemporaryBonus} that heals instead of contributing to a stat — {@link
 * ModifierType#REGENERATION}, the shape Regeneração Reativa takes once it starts. Built from a
 * {@link Blessing} by {@link TemporaryBonus#from}, and advanced by {@link
 * CombatantSheet#tickTemporaryEffects()} — called once per Rodada from {@link
 * CombatantSheet#finishTurn()}, which is exactly the "em seus Turnos" the clause asks for.
 *
 * <p><b>A budget, not just a countdown.</b> "A quantidade de PV recuperados desta forma não pode
 * superar os danos sofridos" caps the <i>total</i> healed over this effect's whole life at the
 * damage that triggered it — the {@link Blessing#getTotalLimit()} whoever resolved the trigger
 * put there. {@link #getRemainingRecovery()} is spent down alongside the Rodada count, and the
 * effect stops healing the moment either runs out. It counts what the sheet <i>actually</i>
 * recovered, not what was offered: a holder already at full PV, or one whose healing is prevented
 * outright (Feridas Dolorosas — see {@link CombatantSheet#heal(int)}), spends none of the budget.
 * A {@code null} limit means no cap at all, for a regenerating effect whose own rules text states
 * none.
 *
 * <p><b>Its stacking ceiling is inherited, not its own.</b> A sourced {@link TemporaryBonus}
 * already refuses to accumulate with another from the same source — it replaces it, renewing the
 * duration — which is exactly the base Característica's "Efeito não cumulativo"; {@code
 * TrollFeat#REGENERACAO_REATIVA_SUPERIOR} raises that ceiling, and {@code
 * TrollsRacialAbility#REGENERACAO_REATIVA} is what resolves the figure and states it on the
 * {@link Blessing}. See {@link TemporaryBonus} for why it is a count rather than a flag. Each
 * surviving instance keeps its own Rodada count and its own budget, so several hits genuinely
 * stack their healing.
 *
 * <p><b>Healing interrupts Sangramento</b>, since this goes through {@link
 * CombatantSheet#heal(int)} like every other recovery — a regenerating combatant stops bleeding
 * on their next Turn. That is Sangramento's own "Efeitos de cura interrompem a perda de PV por
 * rodada" reaching a new source, not a special case of this one.
 */
@Getter
public class Regeneration extends TemporaryBonus {

    private Integer remainingRecovery;

    /**
     * @param valuePerRound       PV recovered on each of the holder's Turns
     * @param rounds              how many Rodadas it lasts
     * @param totalLimit          the most it may ever recover, or {@code null} for no cap
     * @param source              the trait that granted it — what "the same grant" means
     * @param maximumSimultaneous how many of these one sheet may hold at once
     */
    public Regeneration(final int valuePerRound, final int rounds, final Integer totalLimit,
                        final String source, final int maximumSimultaneous) {
        this(valuePerRound, (Integer) rounds, totalLimit, source, maximumSimultaneous);
    }

    private Regeneration(final int valuePerRound, final Integer rounds, final Integer totalLimit,
                         final String source, final int maximumSimultaneous) {
        super(ModifierType.REGENERATION, valuePerRound, rounds, source, maximumSimultaneous);
        this.remainingRecovery = totalLimit == null ? null : Math.max(0, totalLimit);
    }

    /**
     * A standing Regeneração with no end — "Recupera Vigor PV por Rodada" as a creature's own
     * anatomy, not a timed grant. One per source; lifted only by {@code removeEffectsFrom(source)}.
     */
    public static Regeneration openEnded(final int valuePerRound, final String source) {
        return new Regeneration(valuePerRound, (Integer) null, null, source, 1);
    }

    /** PV recovered per Rodada — {@link TemporaryBonus#getValue()} under the name this reads as. */
    public int getValuePerRound() {
        return getValue();
    }

    @Override
    void applyRoundEffect(final CombatantSheet sheet) {
        int offered = remainingRecovery == null
                ? getValuePerRound()
                : Math.min(getValuePerRound(), remainingRecovery);
        if (offered <= 0) {
            return;
        }
        int damageBefore = sheet.getDamageTaken();
        int damageAfter = sheet.heal(offered, HealingSource.regeneration(this));
        if (remainingRecovery != null) {
            remainingRecovery -= damageBefore - damageAfter;
        }
    }
}
