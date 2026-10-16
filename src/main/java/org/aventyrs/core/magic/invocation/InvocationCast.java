package org.aventyrs.core.magic.invocation;

import lombok.Builder;
import lombok.NonNull;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.util.DiceRoller;

/**
 * What {@link SpellInvocationService#plan} needs from a cast that landed (core 0.1.7.6).
 *
 * @param caster               who cast it
 * @param target               the touched body ({@link SpellInvocation.Target#CORPSE}) or animal; {@code null} for the
 *                             rest
 * @param roller               the dice a Lacerto power, an Experimento or the rest roll at the cast
 * @param effectChainTriggered whether the cast's Corrente triggered — Canção de Flora's Fauna e Flora, Servo
 *                             Cadavérico's Escudeiro Cadavérico
 * @param choices              Canção de Flora's animal counts
 * @param options              the caster's Bruxo choices; {@link InvocationOptions#NONE} for none
 * @param extraRounds          Rodadas a critical Conjuração's Potencializar added to the nature tree's Duração
 */
@Builder
public record InvocationCast(@NonNull CombatantSheet caster, CombatantSheet target, DiceRoller roller,
                             boolean effectChainTriggered, InvocationChoices choices, InvocationOptions options,
                             int extraRounds) {

    public InvocationCast {
        choices = choices == null ? InvocationChoices.NONE : choices;
        options = options == null ? InvocationOptions.NONE : options;
    }
}
