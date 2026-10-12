package org.aventyrs.core.effect;

import org.aventyrs.core.character.MovementMode;
import org.aventyrs.core.character.services.MovementServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TemporaryBonus;

/**
 * Desestabilizar — the Ponta Dupla projectile's Corrente: "O Movimento Base do alvo é reduzido em -1UD. Se o alvo
 * tiver Movimento Base de Voo, ele pousará obrigatoriamente no próximo Turno dele e será impedido de Voar por 2
 * Rodadas."
 *
 * <p>The -1UD is a sourced {@link ModifierType#MOVEMENT} {@link TemporaryBonus}, so a second hit renews it rather than
 * reaching -2UD. ⚠️ The text gives the reduction no Duração; it is read as lasting the same {@value #ROUNDS}
 * Rodadas as the flight lock. The landing and the flight lock are <b>reported</b> ({@link
 * InteractionResult#getGroundedRounds()}): whether someone is flying is the caller's {@code
 * EnvironmentalState#flying}, never sheet state.
 */
public class Desestabilizar extends AbstractEffect implements EffectChain {

    /** "reduzido em -1UD". */
    public static final int MOVEMENT_REDUCTION = 1;

    /** "impedido de Voar por 2 Rodadas" — and, by reading, the reduction's Duração. */
    public static final int ROUNDS = 2;

    private static final String SOURCE = "Desestabilizar";

    @Override
    public String getDescription() {
        return "O Movimento Base do alvo é reduzido em -1UD. Se o alvo tiver Movimento Base de Voo, ele pousará "
                + "obrigatoriamente no próximo Turno dele e será impedido de Voar por 2 Rodadas.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        target.applyEffect(new TemporaryBonus(ModifierType.MOVEMENT, -MOVEMENT_REDUCTION, ROUNDS, SOURCE));
        InteractionResult.InteractionResultBuilder result = InteractionResult.builder();
        if (new MovementServiceImpl().hasMovementMode(target, MovementMode.FLIGHT)) {
            result.groundedRounds(ROUNDS);
        }
        return reportChain(result.resultStatus(resolveStatus(target))).build();
    }
}
