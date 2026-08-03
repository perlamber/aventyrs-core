package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The Habilidades de Competência available to characters trained in Profissão. Every one of
 * these needs an Item/Equipamento entity this core doesn't have yet — production time,
 * Dureza, Carga, and per-item combat properties (critical resistance/margin, damage
 * reduction, Defesas, Dano Base, Conjuração bonuses) are all properties of equipment that
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

    // TODO: produced Equipamentos Defensivos permanently grant Resistência a Críticos (a
    // new critical-hit-resistance concept, distinct from anything modeled today) plus a
    // choice — baked in at item creation — between Redução de Danos Sofridos 1 (RD is now
    // mechanically real — see DamageService.getTotalDamageReduction — but unlike
    // ArtesAprimorarComArteAbility's now-working RDS branch, this choice lives on the
    // produced item, not the character) or +1 Defesas; produced Equipamentos
    // Ofensivos grant Margem Crítica Maior +1 (a *different* axis from every other skill's
    // "Margem Crítica Menor" — no critical-margin concept of either kind exists yet) plus a
    // choice between +1 Dano Base or +1 Conjuração (a Magia-effect bonus, same gap as
    // DominioDoManaCompetencyAbility.ARCANISMO_EXPLOSIVA). Needs an
    // Item/Equipamento entity carrying who produced it and which choice was made at
    // creation, none of which exist yet.
    FORJA_VULCANA("Equipamentos que você produz tem benefícios adicionais: Equipamentos " +
            "Defensivos concedem Resistência à Críticos, além disso concedem Redução de " +
            "Danos Sofridos 1 ou Bônus de +1 em Defesas (definido na criação do item). " +
            "Equipamentos Ofensivos: Margem Crítica Maior +1, além disso possuem Dano Base " +
            "+1 ou Conjuração +1."),

    // TODO: +2 to Dureza recovered when repairing an item/equipment (no longer a dice roll,
    // so that particular blocker is gone), extendable to Magias/Habilidades at 5 Graduações,
    // then the bonus itself becomes +5 total at 10 Graduações — still needs an
    // Item/Equipamento entity with a Dureza stat and a repair mechanic (neither exists), a
    // Dureza-equivalent concept on Magias/Habilidades (doesn't exist either), and a
    // graduation-crossing-a-threshold trigger for the 10th-Graduação bump (same gap as
    // ArtesExcellency.FOCADO/LENDA's Fama trigger).
    REPARO_MELHORADO("Sempre que você reparar um item ou equipamento a Dureza recuperada " +
            "aumenta em +2, com 5 Graduações você pode estender este efeito às suas Magias " +
            "e Habilidades, com 10 Graduações este benefício muda para +5."),

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
