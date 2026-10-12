package org.aventyrs.core.effect;

import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;

/**
 * Disparo Esmagador — the Esférica projectile's Corrente: "Adicionalmente o alvo e seus Equipamentos Defensivos
 * sofrem 1 ponto de dano."
 *
 * <p>The target's point lands on its PV as this stage runs, after the hit's own mitigation — ⚠️ like {@link Rugido}'s
 * +2, RD does not reach it. Each worn Equipamento Defensivo takes its point through {@link Item#applyDamage(int)}, so
 * the item's own enhancements mitigate it and a catalog template (never a real copy) shrugs it off.
 */
public class DisparoEsmagador extends AbstractEffect implements EffectChain {

    /** "sofrem 1 ponto de dano" — the target and each Equipamento Defensivo alike. */
    public static final int DAMAGE = 1;

    @Override
    public String getDescription() {
        return "Adicionalmente o alvo e seus Equipamentos Defensivos sofrem 1 ponto de dano.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        int before = target.getDamageTaken();
        target.applyDamage(DAMAGE);
        if (target.getCharacter() != null) {
            for (Item item : target.getCharacter().getEquipment()) {
                if (item.getType() == ItemType.DEFENSIVE) {
                    item.applyDamage(DAMAGE);
                }
            }
        }
        return reportChain(InteractionResult.builder()
                .resourceLossValue(target.getDamageTaken() - before)
                .resourceLossType(ResourceType.HIT_POINTS)
                .resultStatus(resolveStatus(target)))
                .build();
    }
}
