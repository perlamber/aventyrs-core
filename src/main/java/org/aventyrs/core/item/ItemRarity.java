package org.aventyrs.core.item;

/**
 * How rare an {@link Item} is — the second half of an item's own "(Pesado/Raro)"-style
 * heading, alongside {@link ItemWeightClass}. Already referenced by rules text elsewhere in
 * this codebase before any Item entity existed: {@code
 * org.aventyrs.core.ego.ResourcesAdvantage#HERANCA_FAMILIAR}'s "um Equipamento Comum
 * Ofensivo de qualquer Raridade" and its "Obra-Prima Comum ou Incomum" upgrade tier.
 *
 * <p>Only the tiers actually confirmed by rules text so far are modeled — {@link
 * ArmorItem#ARMADURA_DE_JUSTA}'s own "(Pesado/Épico)" heading is what added {@link #EPIC},
 * after the first three. If the ruleset has further ones (Lendário or similar), add the
 * constant once its real text exists — same "don't invent content" discipline {@code
 * AventyrTitleSpecialization} follows for a Título's still-unsupplied second Especialização.
 *
 * <p>Obra-Prima tiers and Aprimoramentos are deliberately *not* modeled here: those are
 * per-acquired-copy upgrades, not a property of the catalog entry (see {@link Item}'s own
 * javadoc on the catalog-versus-owned-copy split).
 */
public enum ItemRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC
}
