package org.aventyrs.core.effect;

import lombok.NonNull;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * Escancarar Defesas — "Seu próximo ataque contra este mesmo alvo tem a GD reduzida em -1 Nível." A Corrente de
 * Efeitos ({@code AssassinoFeat#GOLPE_SOBRENATURAL}'s critical, unbuilt).
 *
 * <p>What it changes is the <b>attacker's</b> next roll, so it marks the attacker's sheet ({@code
 * CombatantSheet#openDefensesOf}): that attacker's next Perícia de Ataque roll against this target is {@link
 * #DIFFICULTY_REDUCTION_LEVELS} nível easier ({@code AbstractSkillInteraction}), and the mark is spent when an attack
 * naming the target is recorded ({@code CombatantSheet#recordAction}). On {@code AttackDelivery} that nível joins
 * the reported {@code unappliedDifficultyReduction}, like every attacker-side nível there.
 */
public class EscancararDefesas extends AbstractEffect implements EffectChain {

    /** "-1 Nível". */
    public static final int DIFFICULTY_REDUCTION_LEVELS = 1;

    private final CombatantSheet attacker;

    /** @param attacker whose next attack against the target finds its Defesas open */
    public EscancararDefesas(@NonNull final CombatantSheet attacker) {
        this.attacker = attacker;
    }

    @Override
    public String getDescription() {
        return "Seu próximo ataque contra este mesmo alvo tem a GD reduzida em -1 Nível.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        attacker.openDefensesOf(target);
        return reportChain(InteractionResult.builder().resultStatus(resolveStatus(target))).build();
    }
}
