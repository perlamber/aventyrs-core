package org.aventyrs.core.effect.monstrous;

import lombok.NonNull;
import org.aventyrs.core.effect.AbstractEffect;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TemporaryBonus;

/**
 * Ofuscar — "Alvo sofre Redutor de -1PA por 1 Rodada, este Monstro recebe Bônus de +1PA por 1 Rodada. Efeito
 * Cumulativo." ({@code CireneiaAbility#OFUSCAR}).
 *
 * <p>Cumulative, so both halves are <b>unsourced</b> bonuses: a sourced one would replace the last landing instead of
 * adding to it.
 */
public class Ofuscar extends AbstractEffect implements EffectChain {

    /** "-1PA" on the target, "+1PA" on the monster. */
    public static final int ACTION_POINTS = 1;

    /** "por 1 Rodada". */
    public static final int ROUNDS = 1;

    private final CombatantSheet attacker;

    public Ofuscar(@NonNull final CombatantSheet attacker) {
        this.attacker = attacker;
    }

    @Override
    public String getDescription() {
        return "Alvo sofre Redutor de -1PA por 1 Rodada, este Monstro recebe Bônus de +1PA por 1 Rodada. Efeito "
                + "Cumulativo.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        target.applyEffect(new TemporaryBonus(ModifierType.ACTION_POINTS, -ACTION_POINTS, ROUNDS));
        attacker.applyEffect(new TemporaryBonus(ModifierType.ACTION_POINTS, ACTION_POINTS, ROUNDS));
        return reportChain(InteractionResult.builder().resultStatus(resolveStatus(target))).build();
    }
}
