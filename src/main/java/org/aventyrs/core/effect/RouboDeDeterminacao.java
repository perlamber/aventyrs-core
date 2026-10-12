package org.aventyrs.core.effect;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;

/**
 * A fixed Roubo de Determinação on a landed hit — the target loses {@link #getAmount()} PD and the attacker
 * recovers what was actually taken (table ruling, 2026-10-01). {@link Oprimir} is the Corrente that carries one
 * scaled by the critical; this is the standing figure a weapon carries on every hit ({@code
 * OffensiveImprovement#LADRA_DO_AETHER}), chained by {@code AttackDelivery} right behind the damage.
 */
@Getter
public class RouboDeDeterminacao extends AbstractEffect {

    private static final DeterminationPointsService DETERMINATION = new DeterminationPointsServiceImpl();

    private final CombatantSheet attacker;
    private final int amount;

    public RouboDeDeterminacao(@NonNull final CombatantSheet attacker, final int amount) {
        this.attacker = attacker;
        this.amount = amount;
    }

    @Override
    public String getDescription() {
        return "Roubo de Determinação " + amount;
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        int taken = Math.min(Math.max(0, amount),
                Math.max(0, DETERMINATION.getCurrentDeterminationPoints(target.getCharacter(), target)));
        if (taken > 0) {
            target.spendDeterminationPoints(taken);
            attacker.recoverDeterminationPoints(taken);
        }
        return reportChain(InteractionResult.builder()
                .resourceLossValue(taken)
                .resourceLossType(ResourceType.DETERMINATION_POINTS)
                .resultStatus(resolveStatus(target)))
                .build();
    }
}
