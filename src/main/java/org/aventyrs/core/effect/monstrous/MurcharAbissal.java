package org.aventyrs.core.effect.monstrous;

import org.aventyrs.core.effect.AbstractEffect;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TemporaryBonus;

/**
 * Murchar, Ascenção Abissal's Corrente — "Alvo sofre Redutor de -2 Multiplicador de PV por 2 Rodadas (efeito de
 * Maldição)" ({@code AlmaElementalAbility#ASCENCAO}). Named apart from the Outonal's Murchar ({@code
 * BrotosDeMapinguariAbility#OUTONAL}), an Efeito Ativo of a different Modelo that shares only the name.
 *
 * <p>A Maldição: refused by a target immune to Amaldiçoado. The text says nothing of stacking, so a second landing
 * replaces the first (one sourced bonus).
 */
public class MurcharAbissal extends AbstractEffect implements EffectChain {

    /** "-2 Multiplicador de PV". */
    public static final int LIFE_MULTIPLIER_LOSS = 2;

    /** "por 2 Rodadas". */
    public static final int ROUNDS = 2;

    public static final String SOURCE = "Murchar (Ascenção Abissal)";

    @Override
    public String getDescription() {
        return "Alvo sofre Redutor de -2 Multiplicador de PV por 2 Rodadas (efeito de Maldição).";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        if (!target.isImmuneToCondition(ConditionType.AMALDICOADO)) {
            target.applyEffect(new TemporaryBonus(ModifierType.LIFE_MULTIPLIER, -LIFE_MULTIPLIER_LOSS, ROUNDS, SOURCE));
        }
        return reportChain(InteractionResult.builder().resultStatus(resolveStatus(target))).build();
    }
}
