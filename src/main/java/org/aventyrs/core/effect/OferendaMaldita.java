package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.DamageReceipt;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;

/**
 * Oferenda Maldita — Roubo de Vida 2d6 (Maior) / 1d6 (Menor): the <b>attacker</b> recovers that many
 * PV, never more than this hit actually dealt ({@code CombatantSheet#getLastDamageReceived()}) — a
 * Roubo de Vida steals from what landed. Reported as a gain on the attacker's side of the result.
 */
public class OferendaMaldita extends AbstractCriticalEffect {


    public OferendaMaldita(final CriticalEffectContext context) {
        super(context);
    }

    @Override
    public CriticalEffectType getType() {
        return CriticalEffectType.OFERENDA_MALDITA;
    }

    @Override
    protected String majorDescription() {
        return "Roubo de Vida 2d6";
    }

    @Override
    protected String minorDescription() {
        return "Roubo de Vida 1d6";
    }

    @Override
    protected InteractionResult.InteractionResultBuilder resolve(final CombatantSheet target) {
        CombatantSheet attacker = attacker();
        int dealt = target.getLastDamageReceived().map(DamageReceipt::damage).orElse(0);
        if (attacker == null || dealt <= 0) {
            return InteractionResult.builder();
        }
        int stolen = Math.min(roll(pick(2, 1)), dealt);
        int before = attacker.getDamageTaken();
        attacker.heal(stolen);
        return InteractionResult.builder()
                .resourceGainValue(before - attacker.getDamageTaken())
                .resourceGainType(ResourceType.HIT_POINTS);
    }
}
