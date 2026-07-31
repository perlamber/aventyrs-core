package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The Habilidades de Competência available to characters trained in Profissão. Every one of
 * these needs an Item/Equipamento entity this core doesn't have yet — production time,
 * Dureza, Carga and combat-produced-equipment bonuses are all properties of equipment that
 * isn't modeled anywhere (see {@code org.aventyrs.core.item.ItemInteraction}, an unrelated
 * pre-existing stub predating this Perícia) — so none are expressible for real today; see
 * each constant's TODO.
 */
@Getter
@AllArgsConstructor
public enum ProfissaoCompetencyAbility implements SkillCompetencyAbility {

    // TODO: -20% to item/equipment Tempo de Produção — no Item/Equipamento entity or
    // production-time system exists yet.
    CONSTRUTOR_EFICIENTE("Tempo de Produção de itens e equipamentos reduzido em 20%."),

    // TODO: Equipamentos Defensivos the character produced reduce the first damage taken
    // each Cena de Combate by -3, Equipamentos Ofensivos grant Vantagem on the first Ataque
    // Perícia roll each Cena, non-stacking and only for the producing character — needs an
    // Item/Equipamento entity tracking who produced it, plus Cena-de-Combate-scoped
    // "first damage taken"/"first attack roll" state, neither of which exist yet.
    FORJA_VULCANA("Equipamentos Defensivos que você produz reduzem o primeiro dano sofrido " +
            "em cada Cena de Combate em -3, Equipamento Ofensivos que você produz " +
            "concedem Vantagem em sua primeira rolagem de Perícia de Ataque de cada Cena. " +
            "Apenas você recebe estes benefícios, os efeitos não são cumulativos."),

    // TODO: +1d6 to Dureza recovered when repairing an item/equipment, extendable to Magias
    // and Habilidades at 5+ Graduações — needs an Item/Equipamento entity with a Dureza
    // stat and a repair mechanic (neither exists), a Dureza-equivalent concept on
    // Magias/Habilidades (doesn't exist either), and this core deliberately never rolls
    // dice (1d6) — see the skill package-info.
    REPARO_MELHORADO("Sempre que você reparar um item ou equipamento a Dureza recuperada " +
            "aumenta em +1d6, se você possuir 5 ou mais Graduações nesta Perícia pode " +
            "estender este efeito às suas Magias e Habilidades."),

    // TODO: +50% to produced equipment's Dureza — no Item/Equipamento entity or Dureza stat
    // exists yet.
    AUMENTAR_A_DUREZA("A Dureza dos equipamentos que você produz aumenta em 50%."),

    // TODO: +5 to produced equipment's Carga capacity — no Item/Equipamento entity or Carga
    // (carrying capacity) stat exists yet.
    EXPANDIR_CARGA("A capacidade de Carga dos Equipamentos que você produz aumenta em +5.");

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.PROFISSAO;
    }
}
