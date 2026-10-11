package org.aventyrs.core.ego;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.EgoDomain;

/**
 * The Vantagem de Recursos chosen once at character creation — available only to
 * characters whose Recursos base reached {@value
 * org.aventyrs.core.character.services.CharacterCreationService#EGO_ADVANTAGE_MIN_BASE}
 * through the creation-time point distribution (see {@link
 * org.aventyrs.core.character.services.CharacterCreationService#isEgoAdvantageAvailable}).
 * Reaching that base any other way (Talentos, Títulos Aventyrs, other Habilidades) never
 * grants access to this choice, and it is never lost if Recursos later drops below it.
 */
@Getter
@AllArgsConstructor
public enum ResourcesAdvantage implements EgoAdvantage {

    /**
     * Fully wired (core 0.1.6.4) through {@link #resolveEquipmentPointDiscount}: -{@value #PURCHASE_OR_PRODUCTION_DISCOUNT}PE
     * on a store purchase ({@code ItemPurchaseService}) and on forging ({@code EquipmentCraftingService#forge}, whose
     * {@code CombatantSheet} form now spends the price), -{@value #UPGRADE_DISCOUNT}PE on fitting an Aprimoramento
     * ({@code #installImprovement}) — "Melhorias de Obras-Primas" read as the same act, the rules' Aprimoramentos
     * being how an Obra-Prima is improved after creation — every price floored at 1PE by {@code
     * EquipmentPointPricing}. The Habilidade de Título -1PE is answered too, but no Habilidade de Título costs PE.
     *
     * <p>Table ruling (2026-10-10): the discount applies <b>at all times</b>, character creation included, so the
     * rules text's "após a criação do Personagem, em campanha" restriction is dropped from the description below.
     */
    BARGANHISTA("Comprar ou produzir equipamentos custam 2 Pontos de Equipamentos a menos, até o " +
            "mínimo de 1PE; Melhorias de Obras-Primas, Aprimoramentos e uso de Habilidades de Título " +
            "custam -1PE (mínimo 1PE).") {
        @Override
        public int resolveEquipmentPointDiscount(final EquipmentPointExpense kind) {
            return switch (kind) {
                case PURCHASE, PRODUCTION -> PURCHASE_OR_PRODUCTION_DISCOUNT;
                case UPGRADE, TITLE_ABILITY -> UPGRADE_DISCOUNT;
            };
        }
    },

    /**
     * Real since 0.1.4.1: {@code CharacterCreationService#isFamilyHeirloom} says which item qualifies (any Raridade of
     * Equipamento Ofensivo, an Obra-Prima Ofensiva Comum or Incomum, no Aprimoramentos, no Regalia) and {@code
     * #grantFamilyHeirloom} gives it at creation for no PE. Equipamentos Tecnológicos have no catalog entries yet,
     * so the exclusion holds without a check.
     */
    HERANCA_FAMILIAR("Durante a criação do personagem você pode escolher um Equipamento " +
            "Comum Ofensivo de qualquer Raridade, o item escolhido é uma Obra-Prima Comum " +
            "ou Incomum e não possui Aprimoramentos. Não é possível obter Equipamentos " +
            "Tecnológicos ou Regalias como Heranças Familiares."),

    /**
     * The catalog/rules-text entry — a character who actually picks this is granted a
     * {@link MoralHerdadaAbility} instance instead (carrying the Fama Positiva/Negativa
     * choice this Vantagem's own rules text requires), the same "keep the constant, redirect
     * via comment" convention as {@code ArtesCompetencyAbility#APRIMORAR_COM_ARTE}. See that
     * class for the real wiring: the starting Fama grant (via {@link
     * MoralHerdadaAbility#applyStartingFama}) and the Artes/Persuasão roll bonus (via {@link
     * EgoAdvantage#resolveSkillSpecificRollBonus}) are both real; the grant itself has no
     * automatic caller yet, since {@code CharacterCreationServiceImpl} only ever builds a
     * plain {@code Character} — no {@code CombatantSheet} (where Fama actually lives) exists
     * yet at that point for it to grant onto.
     */
    MORAL_HERDADA("Você pertence a uma família notável ou descende de um herói ou vilão " +
            "notável, possui um título de nobreza e é reconhecido por isso. Você recebe " +
            "Fama Positiva ou Negativa (à sua escolha) igual ao seu valor de Recursos e " +
            "recebe Bônus de +1 em rolagens de Artes e Persuasão, este bônus aumenta em +1 " +
            "para cada 10 pontos da Fama escolhida.");

    /** BARGANHISTA's "custam 2 Pontos de Equipamentos a menos" on buying or producing. */
    private static final int PURCHASE_OR_PRODUCTION_DISCOUNT = 2;

    /** BARGANHISTA's "-1PE" on Melhorias de Obras-Primas, Aprimoramentos and Habilidades de Título. */
    private static final int UPGRADE_DISCOUNT = 1;

    private final String description;

    @Override
    public EgoDomain getEgoDomain() {
        return EgoDomain.RECURSOS;
    }
}
