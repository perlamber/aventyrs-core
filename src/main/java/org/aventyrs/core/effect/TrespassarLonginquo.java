package org.aventyrs.core.effect;

import org.aventyrs.core.scene.AreaOfEffect;
import org.aventyrs.core.scene.AreaShape;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * Trespassar Longínquo — the Folha Cortante projectile's Corrente: "Tipo de ataque muda de Alvo Único para Penetrante
 * (Distância Curta)."
 *
 * <p>Like {@link ExplosaoCataclismica} it changes the attack it rides rather than its target: the Penetrante line is
 * reported on {@link InteractionResult#getTriggeredAreaOfEffect()}, and the caller resolves who else stands on it
 * ({@code AreaFootprint}) and deals them the hit — this core holds no positions.
 */
public class TrespassarLonginquo extends AbstractEffect implements EffectChain {

    /** "Penetrante (Distância Curta)". */
    public static final AreaOfEffect AREA =
            new AreaOfEffect(AreaShape.PENETRANTE, Range.DISTANCIA_CURTA.getMaxUnidadesDeDistancia());

    @Override
    public String getDescription() {
        return "Tipo de ataque muda de Alvo Único para Penetrante (Distância Curta).";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        return reportChain(InteractionResult.builder()
                .triggeredAreaOfEffect(AREA)
                .resultStatus(resolveStatus(target)))
                .build();
    }
}
