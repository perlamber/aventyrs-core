package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;

/**
 * Ferida Infecciosa — "Este ataque causa 3 pontos de danos adicionais, estes danos adicionais só podem ser
 * recuperados com Descansos." A Corrente de Efeitos ({@code FeralFeat#DESPREZO_NATURAL}'s Armas Naturais).
 *
 * <p>The {@link #EXTRA_DAMAGE} lands on the target's PV as this stage runs, after the hit's own mitigation — ⚠️ like
 * {@link Rugido}'s +2 it rides the Corrente rather than the dano roll, so RD does not reach it. What landed is then
 * locked with {@code CombatantSheet#lockDamageUntilRest}: no heal recovers it but a Descanso, of any tier (table
 * ruling, 2026-10-01).
 */
public class FeridaInfecciosa extends AbstractEffect implements EffectChain {

    /** "3 pontos de danos adicionais". */
    public static final int EXTRA_DAMAGE = 3;

    @Override
    public String getDescription() {
        return "Este ataque causa 3 pontos de danos adicionais, estes danos adicionais só podem ser recuperados com "
                + "Descansos.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        int before = target.getDamageTaken();
        target.applyDamage(EXTRA_DAMAGE);
        int landed = target.getDamageTaken() - before;
        target.lockDamageUntilRest(landed);
        return reportChain(InteractionResult.builder()
                .resourceLossValue(landed)
                .resourceLossType(ResourceType.HIT_POINTS)
                .resultStatus(resolveStatus(target)))
                .build();
    }
}
