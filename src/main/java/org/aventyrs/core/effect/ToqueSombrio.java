package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;

/**
 * Toque Sombrio — "O dano causado aumenta em +1 e é Profano em substituição aos seus tipos." A Corrente de Efeitos
 * (Adepto da Escuridão Profunda's Fiel rung — a Devoto Talento not yet in this core).
 *
 * <p>The +{@link #EXTRA_DAMAGE} lands on the target's PV as this stage runs, after the hit's own mitigation — ⚠️ like
 * {@link Rugido}'s +2, RD does not reach it.
 *
 * <p>"é Profano em substituição aos seus tipos" is real (core 0.0.89): when this Corrente triggers, {@code
 *  AttackDelivery} marks the hit's head {@code DamageSanctity#PROFANO} ({@code DamageInteraction#withSanctity}),
 *  replacing any other nature, and its own +1 takes the same immunity and reduction ({@code SanctityMitigation}).
 */
public class ToqueSombrio extends AbstractEffect implements EffectChain {

    /** "O dano causado aumenta em +1". */
    public static final int EXTRA_DAMAGE = 1;

    @Override
    public String getDescription() {
        return "O dano causado aumenta em +1 e é Profano em substituição aos seus tipos.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        int before = target.getDamageTaken();
        // Profano (core 0.0.89): the +1 shares the hit's type, and an immunity or reduction of Profano reaches it.
        org.aventyrs.core.character.DamageType type = target.getLastDamageReceived()
                .map(org.aventyrs.core.sheet.DamageReceipt::damageType).orElse(null);
        target.applyDamage(org.aventyrs.core.character.services.SanctityMitigation.apply(target, type,
                org.aventyrs.core.character.DamageSanctity.PROFANO, EXTRA_DAMAGE));
        return reportChain(InteractionResult.builder()
                .resourceLossValue(target.getDamageTaken() - before)
                .resourceLossType(ResourceType.HIT_POINTS)
                .resultStatus(resolveStatus(target)))
                .build();
    }
}
