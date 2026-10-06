package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.Burning;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * Fogo Vivo — "Este ataque aplica o Efeito Crítico Menor de Inflamar" (Bruxo's Maldição das Chamas do Norte, carried
 * by the holder's invocations). The Corrente applies {@link Inflamar}'s Menor tier directly: a {@code Burning} of
 * {@link Inflamar#DAMAGE_PER_ROUND} per Rodada, put out with 2PA.
 */
public class FogoVivo extends AbstractEffect implements EffectChain {

    /** Inflamar Menor: "até que o apague 2PA". */
    static final int EXTINGUISH_ACTION_POINTS = 2;

    @Override
    public String getDescription() {
        return "Este ataque aplica o Efeito Crítico Menor de Inflamar.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        target.applyEffect(new Burning(Inflamar.DAMAGE_PER_ROUND, EXTINGUISH_ACTION_POINTS));
        return reportChain(InteractionResult.builder().resultStatus(resolveStatus(target))).build();
    }
}
