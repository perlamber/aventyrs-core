package org.aventyrs.core.effect.monstrous;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.effect.AbstractEffect;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TemporaryBonus;

/**
 * Sanguenomancia Inata's hit — "o alvo sofre Redutor Temporário de -1 em Vigor … (Efeito de Doença, não cumulativo),
 * pontos de Vigor perdidos desta forma são recuperados cada após 1 Descanso" ({@code
 * EscolhidoDeMacariaAbility#RENASCIDO}, 20261015; Predador -2, Apex -3, "mínimo Vigor 1").
 *
 * <p>A Doença, so a target immune to Doente refuses it. One per target (a sourced bonus renews), lifted by its next
 * Descanso of any tier. Never takes Vigor below 1.
 */
public class Sanguenomancia extends AbstractEffect implements EffectChain {

    public static final String SOURCE = "Sanguenomancia Inata";

    private final int reduction;

    public Sanguenomancia(final int reduction) {
        this.reduction = reduction;
    }

    @Override
    public String getDescription() {
        return "O alvo sofre Redutor Temporário de -" + reduction + " em Vigor (Efeito de Doença, não cumulativo, "
                + "mínimo Vigor 1), recuperado após 1 Descanso.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        if (!target.isImmuneToCondition(ConditionType.DOENTE)) {
            target.removeEffectsFrom(SOURCE);
            int loss = Math.min(reduction, Math.max(0, target.getAttributeTotal(AttributeDomain.VIGOR) - 1));
            if (loss > 0) {
                target.applyEffectUntilRest(TemporaryBonus.openEnded(ModifierType.VIGOR_BONUS, -loss, SOURCE),
                        RestType.MINIMO);
            }
        }
        return reportChain(InteractionResult.builder().resultStatus(resolveStatus(target))).build();
    }
}
