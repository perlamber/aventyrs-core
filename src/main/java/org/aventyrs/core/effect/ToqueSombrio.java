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
 * <p>TODO "é Profano em substituição aos seus tipos": Profano is not a {@code DamageType} — it names where damage
 *  comes from ({@code MagicType#PROFANA}), and nothing in this core reads a hit as Profano. The retyping waits on a
 *  first reader (a resistance or immunity to Profano damage).
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
        target.applyDamage(EXTRA_DAMAGE);
        return reportChain(InteractionResult.builder()
                .resourceLossValue(target.getDamageTaken() - before)
                .resourceLossType(ResourceType.HIT_POINTS)
                .resultStatus(resolveStatus(target)))
                .build();
    }
}
