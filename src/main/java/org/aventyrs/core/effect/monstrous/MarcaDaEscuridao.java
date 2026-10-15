package org.aventyrs.core.effect.monstrous;

import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageSanctity;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.Dice;
import org.aventyrs.core.effect.AbstractEffect;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.LifeSteal;
import org.aventyrs.core.sheet.RecurringDice;

import java.util.Optional;

/**
 * Marca da Escuridão — "Alvo é Amaldiçoado e sofre 3d6 pontos de Dano Mágico Profano no início da próxima Rodada, …
 * o Monstro recebe Roubo de Vida adicional igual ao Vigor do alvo por 2 Rodadas. Efeito não cumulativo" ({@code
 * EscolhidoDeMacariaAbility#ANUNCIADOR_DA_ESCURIDAO}, 20261015).
 *
 * <p>A Maldição: a target immune to Amaldiçoado refuses all of it. The 3d6 is queued once, at the target's next
 * Rodada boundary, for the caller to roll; the monster's Roubo de Vida is sourced, so a second mark renews it.
 */
public class MarcaDaEscuridao extends AbstractEffect implements EffectChain {

    public static final String SOURCE = "Marca da Escuridão";

    /** "por 2 Rodadas". */
    public static final int LIFE_STEAL_ROUNDS = 2;

    private final CombatantSheet attacker;

    public MarcaDaEscuridao(@NonNull final CombatantSheet attacker) {
        this.attacker = attacker;
    }

    @Override
    public String getDescription() {
        return "Alvo é Amaldiçoado e sofre 3d6 pontos de Dano Mágico Profano no início da próxima Rodada; o Monstro "
                + "recebe Roubo de Vida adicional igual ao Vigor do alvo por 2 Rodadas. Efeito não cumulativo.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        if (!target.isImmuneToCondition(ConditionType.AMALDICOADO)) {
            target.applyCondition(new Condition(ConditionType.AMALDICOADO, 1, attacker));
            target.applyEffect(new RecurringDice(RecurringDice.Kind.DAMAGE, Dice.of(3),
                    new DamageDescriptor(DamageType.MAGICO, null, DamageSanctity.PROFANO), 1, SOURCE, 1));
            attacker.applyEffect(new LifeSteal(target.getAttributeTotal(AttributeDomain.VIGOR),
                    Optional.of(LIFE_STEAL_ROUNDS), SOURCE));
        }
        return reportChain(InteractionResult.builder().resultStatus(resolveStatus(target))).build();
    }
}
