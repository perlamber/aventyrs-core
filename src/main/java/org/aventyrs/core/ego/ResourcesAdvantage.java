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
     * The "comprar equipamentos ... custam 2 Pontos de Equipamentos a menos, até o mínimo de
     * 1PE" half is real: {@link #resolveEquipmentPurchaseDiscount()} returns 2, and {@code
     * org.aventyrs.core.character.services.ItemPurchaseService} subtracts it and applies the 1PE
     * floor. Still TODO: the "-2PE a menos" on <i>producing</i> equipment ({@code
     * EquipmentCraftingService}/{@code ItemForgery} report a cost but nothing spends it), and the
     * "-1PE (mínimo 1PE)" on Obra-Prima upgrades / Aprimoramentos / Habilidade de Título usage —
     * none of those have a PE spender to discount.
     */
    BARGANHISTA("Comprar ou produzir equipamentos após a criação do Personagem, em " +
            "campanha, custam 2 Pontos de Equipamentos a menos, até o mínimo de 1PE; " +
            "Melhorias de Obras-Primas, Aprimoramentos e uso de Habilidades de Título " +
            "custam -1PE (mínimo 1PE).") {
        @Override
        public int resolveEquipmentPurchaseDiscount() {
            return 2;
        }
    },

    // TODO: grants a chosen Equipamento Comum Ofensivo (any Raridade) at character
    // creation, upgraded to a Comum/Incomum Obra-Prima with no Aprimoramentos, excluding
    // Equipamentos Tecnológicos/Regalias — Obra-Prima tiers/Aprimoramentos and the Regalia
    // marker ({@code Item#isRegalia()}/{@code getRegaliaGrade()}) are all modeled now, and
    // {@code Character#equipment} holds items; the remaining blockers are a Tecnológico
    // classification and, above all, no character-creation flow that makes such a choice
    // (only {@code CharacterCreationServiceImpl}'s fixed path exists — the same gap {@code
    // MoralHerdadaAbility} cites).
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

    private final String description;

    @Override
    public EgoDomain getEgoDomain() {
        return EgoDomain.RECURSOS;
    }
}
