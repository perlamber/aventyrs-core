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

    // TODO: -2PE (minimum 1PE) on buying/producing equipment in campaign, and -1PE
    // (minimum 1PE) on Obra-Prima upgrades/Aprimoramentos/Habilidade de Título usage — {@code
    // org.aventyrs.core.item.Item#getPrice()} is now a real PE figure per catalog entry, but
    // no PE *budget/economy* exists to spend it from, and Obra-Prima tiers/Aprimoramentos are
    // per-owned-copy upgrades this core still doesn't model at all.
    BARGANHISTA("Comprar ou produzir equipamentos após a criação do Personagem, em " +
            "campanha, custam 2 Pontos de Equipamentos a menos, até o mínimo de 1PE; " +
            "Melhorias de Obras-Primas, Aprimoramentos e uso de Habilidades de Título " +
            "custam -1PE (mínimo 1PE)."),

    // TODO: grants a chosen Equipamento Comum Ofensivo (any Raridade) at character
    // creation, upgraded to a Comum/Incomum Obra-Prima with no Aprimoramentos, excluding
    // Equipamentos Tecnológicos/Regalias — {@code org.aventyrs.core.item.ItemRarity} and
    // {@code ItemType#OFFENSIVE} are real now, but Obra-Prima tiers/Aprimoramentos (per-owned-
    // copy upgrades) and Tecnológico/Regalia classifications still aren't modeled, and nothing
    // on {@code Character}/{@code CharacterSheet} holds an item to grant one onto.
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
     * plain {@code Character} — no {@code CharacterSheet} (where Fama actually lives) exists
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
