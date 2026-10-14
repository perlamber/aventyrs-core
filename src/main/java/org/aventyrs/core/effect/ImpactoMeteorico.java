package org.aventyrs.core.effect;

import org.aventyrs.core.scene.AreaOfEffect;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;

/**
 * Impacto Meteórico — "Este ataque inflige +2 pontos de danos e recebe Área de Efeito – Explosão." A generic Corrente
 * de Efeitos (V19 20261012), put on attacks with its host weapon by a bound Adamante Bruto's Efeito Base ({@code
 * item.PowerStoneType#ADAMANTE_BRUTO}, table ruling 2026-10-12: host weapon only).
 *
 * <p>The {@link Rugido} and {@link ExplosaoCataclismica} shapes together:
 * <ul>
 *   <li>the +2 lands on the target's PV as this stage runs — ⚠️ after the hit's own mitigation, so RD does not reach
 *       it (table ruling, as Rugido's);</li>
 *   <li>the Explosão is {@link AreaOfEffect#ATTACK_EXPLOSION}, reported on {@code
 *       InteractionResult#getTriggeredAreaOfEffect()} for the caller to resolve.</li>
 * </ul>
 */
public class ImpactoMeteorico extends AbstractEffect implements EffectChain {

    /** "inflige +2 pontos de danos". */
    public static final int DAMAGE_BONUS = 2;

    /** "Área de Efeito – Explosão" — the attack's 1 UD burst on its target. */
    public static final AreaOfEffect AREA = AreaOfEffect.ATTACK_EXPLOSION;

    @Override
    public String getDescription() {
        return "Este ataque inflige +2 pontos de danos e recebe Área de Efeito – Explosão.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        target.applyDamage(DAMAGE_BONUS);
        return reportChain(InteractionResult.builder()
                .resourceLossValue(DAMAGE_BONUS)
                .resourceLossType(ResourceType.HIT_POINTS)
                .triggeredAreaOfEffect(AREA)
                .resultStatus(resolveStatus(target)))
                .build();
    }
}
