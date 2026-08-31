package org.aventyrs.core.item;

/**
 * How rare an {@link Item} is — the second half of an item's own "(Pesado/Raro)"-style
 * heading, alongside {@link ItemWeightClass}. Already referenced by rules text elsewhere in
 * this codebase before any Item entity existed: {@code
 * org.aventyrs.core.ego.ResourcesAdvantage#HERANCA_FAMILIAR}'s "um Equipamento Comum
 * Ofensivo de qualquer Raridade" and its "Obra-Prima Comum ou Incomum" upgrade tier.
 *
 * <p>Only the tiers actually confirmed by rules text so far are modeled — {@link
 * ArmorItem#ARMADURA_DE_JUSTA}'s own "(Pesado/Épico)" heading added {@link #EPIC}, while the
 * Couro de Dragão and Espírito Umbral Masterpieces add {@link #MYTHIC}. If the ruleset has
 * further tiers, add a constant once its real text exists.
 *
 * <p>Obra-Prima tiers and Aprimoramentos are deliberately *not* modeled here: those are
 * per-acquired-copy upgrades, not a property of the catalog entry (see {@link Item}'s own
 * javadoc on the catalog-versus-owned-copy split).
 */
public enum ItemRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    MYTHIC
}
