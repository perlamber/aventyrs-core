package org.aventyrs.core.item;

import org.aventyrs.core.effect.DefensiveCriticalEffectType;

/**
 * An {@link ItemTemplate} that grants its wearer an Efeito Crítico Defensivo — the column
 * "apenas Armaduras e Escudos recebem" ({@code docs/rules/efeitos-criticos.txt} L55). It lives
 * on this narrow interface rather than on {@link Item} for the same reason {@code
 * Weapon#getDamageBase()} does: a Capa or an Elmo grants none, and a defaulted column on {@code
 * Item} would make "a helmet" and "a breastplate" answer alike.
 *
 * <p>Promoted here from {@code ArmorItem} once {@link ShieldItem} landed and needed the identical
 * shape — the "build for the second real consumer" point in CLAUDE.md's recurring conventions.
 *
 * <p><b>Nothing reads it yet</b> — a Defensive effect fires on an Acerto Crítico in the wearer's
 * own Defesa roll, and neither {@code AttackReceiver} nor {@code EsquivaEApararInteraction}
 * resolves that branch. The values are exact authored data all the same, the same "can't apply
 * it yet doesn't mean can't compute it yet" discipline this codebase applies elsewhere.
 */
public interface CriticallyDefensiveItem extends ItemTemplate {

    /**
     * The Efeito Crítico Defensivo this piece of gear grants its wearer, from the source
     * catalog's "Atualizando os Equipamentos Defensivos" table — never {@code null}: the table
     * assigns one to every Armadura and every Escudo.
     */
    DefensiveCriticalEffectType getDefensiveCriticalEffect();
}
