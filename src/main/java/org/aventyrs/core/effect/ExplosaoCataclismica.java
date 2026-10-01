package org.aventyrs.core.effect;

import org.aventyrs.core.scene.AreaOfEffect;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * Explosão Cataclísmica — "Este ataque recebe Área de Efeito – Explosão e Cataclismo como um Efeito Crítico
 * adicional." A Corrente de Efeitos ({@code ElementalFeat#GOLPE_CATACLISMICO}'s critical, gated on a Gana Elemental
 * nothing activates yet; and Impacto Ymiriano's Fiel rung, a Devoto Talento not in this core).
 *
 * <p>Like {@link GolpeTrovejante} it changes the attack it rides rather than its target:
 * <ul>
 *   <li><b>Cataclismo</b> — {@code AttackDelivery} adds {@link #ADDITIONAL_CRITICAL_EFFECT} to the attack's Efeitos
 *       Críticos when a triggered Corrente is one of these, so it applies wherever an Efeito Crítico would (on a
 *       critical, and filtered by the target's anatomy like any other).</li>
 *   <li><b>Área de Efeito – Explosão</b> — {@link AreaOfEffect#ATTACK_EXPLOSION} on the target, reported on {@code
 *       InteractionResult#getTriggeredAreaOfEffect()}. The caller resolves the footprint's other occupants and deals
 *       them the hit: this core holds no positions, and the occupants are only knowable once the Corrente fired.</li>
 * </ul>
 */
public class ExplosaoCataclismica extends AbstractEffect implements EffectChain {

    /** "Cataclismo como um Efeito Crítico adicional". */
    public static final CriticalEffectType ADDITIONAL_CRITICAL_EFFECT = CriticalEffectType.CATACLISMO;

    /** "Área de Efeito – Explosão" — the attack's 1 UD burst on its target (table ruling, 2026-09-26). */
    public static final AreaOfEffect AREA = AreaOfEffect.ATTACK_EXPLOSION;

    @Override
    public String getDescription() {
        return "Este ataque recebe Área de Efeito – Explosão e Cataclismo como um Efeito Crítico adicional.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        return reportChain(InteractionResult.builder()
                .triggeredAreaOfEffect(AREA)
                .resultStatus(resolveStatus(target)))
                .build();
    }
}
