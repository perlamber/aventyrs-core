package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.DamageReceipt;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * Cataclismo — "Na Rodada seguinte o alvo sofre uma quantidade de dano igual ao dano Elemental
 * causado por este ataque" (Maior) / "metade" (Menor). Reads the hit this chain's {@code
 * DamageInteraction} just recorded ({@code CombatantSheet#getLastDamageReceived()}); only an
 * Elemental or Físico Elemental hit has an elemental figure to repeat. Scheduled as {@code
 * PostponedDamage} — already mitigated, landing at the next Rodada boundary.
 */
public class Cataclismo extends AbstractCriticalEffect {


    public Cataclismo(final CriticalEffectContext context) {
        super(context);
    }

    @Override
    public CriticalEffectType getType() {
        return CriticalEffectType.CATACLISMO;
    }

    @Override
    protected String majorDescription() {
        return "Na Rodada seguinte o alvo sofre uma quantidade de dano igual ao dano Elemental causado por este ataque.";
    }

    @Override
    protected String minorDescription() {
        return "Na Rodada seguinte o alvo sofre metade do dano Elemental deste ataque.";
    }

    @Override
    protected InteractionResult.InteractionResultBuilder resolve(final CombatantSheet target) {
        DamageReceipt hit = target.getLastDamageReceived().orElse(null);
        if (hit == null || !hit.isElemental() || hit.damage() <= 0) {
            return InteractionResult.builder();
        }
        int later = getContext().isMajor() ? hit.damage() : hit.damage() / 2;
        if (later > 0) {
            target.schedulePostponedDamage(later, attacker());
        }
        return InteractionResult.builder();
    }
}
