package org.aventyrs.core.effect;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.DamageReceipt;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;

/**
 * Roubo de Vida on a landed hit — the attacker recovers {@link #getAmount()} PV, never more than the hit actually
 * dealt ({@code CombatantSheet#getLastDamageReceived()}), through {@code CombatantSheet#healFromLifeSteal}. The same
 * reading {@link OferendaMaldita} and {@link MagicaeMortis} make of "Roubo de Vida N".
 *
 * <p>{@code AttackDelivery} chains it right behind the damage (core 0.0.86), with the attacker's standing Roubo de
 * Vida ({@code LifeStealService#getTotalLifeSteal}) plus any granted against that target ({@code
 * Feat#resolveTargetedLifeSteal}). Like every stage behind the damage it runs only when the hit dealt some. This
 * is the combat caller {@code LifeStealService} had lacked.
 */
@Getter
public class RouboDeVida extends AbstractEffect {

    private final CombatantSheet attacker;
    private final int amount;

    public RouboDeVida(@NonNull final CombatantSheet attacker, final int amount) {
        this.attacker = attacker;
        this.amount = amount;
    }

    @Override
    public String getDescription() {
        return "Roubo de Vida " + amount;
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        int dealt = target.getLastDamageReceived().map(DamageReceipt::damage).orElse(0);
        int before = attacker.getDamageTaken();
        if (dealt > 0 && amount > 0
                && org.aventyrs.core.character.services.LifeStealGuard.allows(target, attacker)) {
            attacker.healFromLifeSteal(Math.min(amount, dealt));
        }
        return reportChain(InteractionResult.builder()
                .resourceGainValue(before - attacker.getDamageTaken())
                .resourceGainType(ResourceType.HIT_POINTS)
                .resultStatus(resolveStatus(target)))
                .build();
    }
}
