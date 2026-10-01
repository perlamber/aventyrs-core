package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.PostponedWoundWard;

/**
 * Procrastinar Ferimento's Corrente — Estancar: "Se o dano sofrido fosse causar efeitos de Sangramento ele não causará
 * este efeito" (core 0.0.94). Marks the target's waiting {@link PostponedWoundWard}, so the hit it postpones leaves a
 * {@code StanchedWound} that refuses the Sangramento.
 */
public class Estancar extends AbstractEffect implements EffectChain {

    @Override
    public String getDescription() {
        return "Se o dano sofrido fosse causar efeitos de Sangramento ele não causará este efeito.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        target.getPostponedWoundWard().ifPresent(PostponedWoundWard::stanch);
        return reportChain(InteractionResult.builder()
                .resultStatus(resolveStatus(target)))
                .build();
    }
}
