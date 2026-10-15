package org.aventyrs.core.effect.monstrous;

import org.aventyrs.core.effect.AbstractEffect;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.MonstrousAbilityGrant;
import org.aventyrs.core.monster.model.MonsterModel;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TemporaryBonus;

/**
 * Veneno Primaveril — "Efeito de veneno. Personagens afetados sofrem redutor de -1 Multiplicador de PV e -1
 * Multiplicador de PD por 2 Rodadas. Apenas personagens vivos que não sejam Feéricos ou outros Brotos de Mapinguari
 * podem ser infectados" ({@code BrotosDeMapinguariAbility#PRIMAVERIL}'s Lançar Esporos, 20261015; Predador 3
 * Rodadas).
 *
 * <p>The two Multiplicador losses share one source, so a second infection renews rather than stacks. {@link #roubos}
 * builds the Primaveril's "Roubo de Mana 1 e Roubo de Determinação 1" against an infected target — a Corrente that
 * looks at its target when it lands, since the attack hook cannot see who is being attacked.
 */
public class VenenoPrimaveril extends AbstractEffect implements EffectChain {

    public static final String SOURCE = "Veneno Primaveril";

    private final int rounds;

    public VenenoPrimaveril(final int rounds) {
        this.rounds = rounds;
    }

    /** Whether target is infected right now. */
    public static boolean infects(final CombatantSheet target) {
        return target.hasEffectFrom(SOURCE);
    }

    /** "Apenas personagens vivos que não sejam Feéricos ou outros Brotos de Mapinguari". */
    public static boolean canInfect(final CombatantSheet target) {
        return !target.hasUndeadAnatomy()
                && target.getCreatureType() != CreatureType.FEERICO
                && !target.isImmuneToCondition(ConditionType.ENVENENADO)
                && MonstrousAbilityGrant.heldBy(target.getCharacter())
                .noneMatch(grant -> grant.getAbility().getModel() == MonsterModel.BROTOS_DE_MAPINGUARI);
    }

    @Override
    public String getDescription() {
        return "Efeito de veneno. Personagens afetados sofrem redutor de -1 Multiplicador de PV e -1 Multiplicador de PD "
                + "por " + rounds + " Rodadas.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        if (canInfect(target)) {
            target.applyEffect(new TemporaryBonus(ModifierType.LIFE_MULTIPLIER, -1, rounds, SOURCE));
            target.applyEffect(new TemporaryBonus(ModifierType.DETERMINATION_MULTIPLIER, -1, rounds, SOURCE));
        }
        return reportChain(InteractionResult.builder().resultStatus(resolveStatus(target))).build();
    }

    /** The Primaveril's Roubos against an infected target, as one Corrente judged when it lands. */
    public static EffectChain roubos(final CombatantSheet attacker) {
        return new Roubos(attacker);
    }

    private static final class Roubos extends AbstractEffect implements EffectChain {

        private final CombatantSheet attacker;

        private Roubos(final CombatantSheet attacker) {
            this.attacker = attacker;
        }

        @Override
        public String getDescription() {
            return "Contra personagens afetados pelo Veneno Primaveril: Roubo de Mana 1 e Roubo de Determinação 1.";
        }

        @Override
        public InteractionResult applyTo(final CombatantSheet target) {
            if (infects(target)) {
                new RouboDeBonusBase(attacker, 1, 1).applyTo(target);
            }
            return reportChain(InteractionResult.builder().resultStatus(resolveStatus(target))).build();
        }
    }
}
