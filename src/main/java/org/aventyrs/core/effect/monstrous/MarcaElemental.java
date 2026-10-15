package org.aventyrs.core.effect.monstrous;

import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.effect.AbstractEffect;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.RecurringDamage;

/**
 * Marca Elemental — "Alvo sofre 3 pontos de Dano Mágico Elemental por 2+Vigor do Alvo Rodadas (Efeito de Maldição,
 * não cumulativo)" ({@code AlmaElementalAbility#ARMAMENTO_ELEMENTAL}'s Predador Fúria Elemental).
 *
 * <p>A Maldição, so a target immune to Amaldiçoado (the Anatomia de Morto-Vivo, an Ascenção Abissal) refuses it.
 * Not cumulative: a second mark replaces the first and renews its Duração.
 */
public class MarcaElemental extends AbstractEffect implements EffectChain {

    /** "3 pontos de Dano Mágico Elemental". */
    public static final int DAMAGE_PER_ROUND = 3;

    /** "por 2+Vigor do Alvo Rodadas". */
    public static final int BASE_ROUNDS = 2;

    public static final String SOURCE = "Marca Elemental";

    private final ElementalType element;

    public MarcaElemental(@NonNull final ElementalType element) {
        this.element = element;
    }

    @Override
    public String getDescription() {
        return "Alvo sofre 3 pontos de Dano Mágico Elemental por 2+Vigor do Alvo Rodadas (Efeito de Maldição, não "
                + "cumulativo)";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        if (!target.isImmuneToCondition(ConditionType.AMALDICOADO)) {
            int rounds = BASE_ROUNDS + target.getAttributeTotal(AttributeDomain.VIGOR);
            target.applyEffect(new RecurringDamage(DAMAGE_PER_ROUND, new DamageDescriptor(DamageType.MAGICO, element),
                    rounds, SOURCE));
        }
        return reportChain(InteractionResult.builder().resultStatus(resolveStatus(target))).build();
    }
}
