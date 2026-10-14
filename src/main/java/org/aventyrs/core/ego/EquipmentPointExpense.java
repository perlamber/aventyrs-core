package org.aventyrs.core.ego;

/**
 * What a Pontos de Equipamento expense is for — the question {@link EgoAdvantage#resolveEquipmentPointDiscount} is
 * asked about, since {@link ResourcesAdvantage#BARGANHISTA} prices each kind differently: "Comprar ou produzir
 * equipamentos ... custam 2 Pontos de Equipamentos a menos ...; Melhorias de Obras-Primas, Aprimoramentos e uso de
 * Habilidades de Título custam -1PE (mínimo 1PE)" (core 0.1.6.4).
 */
public enum EquipmentPointExpense {

    /** Buying a copy from a store — {@code ItemPurchaseService}. */
    PURCHASE,

    /** Forging a copy, a Regalia included — {@code EquipmentCraftingService#forge}. */
    PRODUCTION,

    /** Fitting an Aprimoramento to an Obra-Prima — {@code EquipmentCraftingService#installImprovement}. */
    UPGRADE,

    /**
     * Using a Habilidade de Título. No Habilidade de Título has a PE price ({@code AventyrTitleAbility} costs PD, Ego
     * and PA only), so nothing spends this kind; BARGANHISTA's figure for it is real data with no spender yet.
     */
    TITLE_ABILITY
}
