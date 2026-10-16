package org.aventyrs.core.effect.monstrous;

import lombok.NonNull;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.Dice;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.effect.AbstractEffect;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.RecurringDice;

/**
 * The Possessão the Aspecto Humanoide's Efeito Ativo delivers — "o Monstro toma o controle do personagem alvo por 2
 * Rodadas, apenas personagens com o Autocontrole inferior ao do Monstro pode ser alvo deste Efeito (efeito de
 * Possessão). Alvos possuídos sofrem 1d6 pontos de Dano Mágico Primordial a cada Rodada" ({@code
 * AspectoHumanoideAbility#POSSESSAO}, core 0.1.7.3). Apex: "Duração dobrada contra alvos com Autocontrole zero".
 *
 * <p>Autocontrole is compared as each side's current temporary points. The control itself is the Condição Possessão
 * (no sheet changes hands in this core); the 1d6 is queued each Rodada as Primordial damage for the caller to roll.
 */
public class Possessao extends AbstractEffect implements EffectChain {

    /** "por 2 Rodadas". */
    public static final int ROUNDS = 2;

    public static final String SOURCE = "Possessão";

    private final CombatantSheet attacker;
    private final boolean doublesAgainstZero;

    public Possessao(@NonNull final CombatantSheet attacker, final boolean doublesAgainstZero) {
        this.attacker = attacker;
        this.doublesAgainstZero = doublesAgainstZero;
    }

    @Override
    public String getDescription() {
        return "O Monstro toma o controle do personagem alvo por 2 Rodadas, apenas personagens com o Autocontrole "
                + "inferior ao do Monstro (efeito de Possessão); possuídos sofrem 1d6 pontos de Dano Mágico Primordial "
                + "a cada Rodada.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        int targetAutocontrole = target.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE);
        if (targetAutocontrole < attacker.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE)
                && !target.isImmuneToCondition(ConditionType.POSSESSAO)) {
            int rounds = doublesAgainstZero && targetAutocontrole == 0 ? ROUNDS * 2 : ROUNDS;
            target.applyCondition(new Condition(ConditionType.POSSESSAO, rounds, attacker));
            target.applyEffect(RecurringDice.damage(Dice.of(1), new DamageDescriptor(DamageType.PRIMORDIAL), rounds,
                    SOURCE));
        }
        return reportChain(InteractionResult.builder().resultStatus(resolveStatus(target))).build();
    }
}
