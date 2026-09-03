package org.aventyrs.core.skill.profissao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

/**
 * The Habilidades de Competência available to characters trained in Profissão. Every one of
 * these is about equipment a character *produces or repairs*, and none is expressible for
 * real today. {@code org.aventyrs.core.item.Item} now exists as a catalog entry — carrying
 * Dureza, Defesas (DF/DM) and Conjuração columns among others — and a forged copy now takes
 * damage for real ({@code Item#applyDamage}), so neither "no Item entity at all" nor "Dureza has
 * no consumer" is the blocker it once was; what's still missing is everything about *producing*
 * or *repairing* one: production time, Carga, a repair mechanic, who produced it, and the
 * choices baked in at its creation. Critical resistance and the Margem Crítica Maior
 * axis don't exist anywhere either. See each constant's TODO for its own remaining gap.
 */
@Getter
@AllArgsConstructor
public enum ProfissaoCompetencyAbility implements SkillCompetencyAbility {

    // TODO: -20% to item/equipment Tempo de Produção. Item is a real catalog entry now, so the
    // missing piece is narrower: nothing *produces* a copy, and no production-time stat exists
    // to reduce.
    CONSTRUTOR_EFICIENTE("Tempo de Produção de itens e equipamentos reduzido em 20%."),

    // TODO: produced Equipamentos Defensivos permanently grant Resistência a Críticos (a
    // new critical-hit-resistance concept, distinct from anything modeled today) plus a
    // choice — baked in at item creation — between Redução de Danos Sofridos 1 (RD is now
    // mechanically real — see DamageService.getTotalDamageReduction — but unlike
    // ArtesAprimorarComArteAbility's now-working RDS branch, this choice lives on the
    // produced item, not the character) or +1 Defesas; produced Equipamentos
    // Ofensivos grant Margem Crítica Maior +1 (a *different* axis from every other skill's
    // "Margem Crítica Menor" — a Menor-axis, Perícia-scoped concept now exists, see
    // ArtesAprimorarComArteAbility#getCriticalMarginReduction, but nothing models the Maior
    // axis or an item-scoped value of either) plus a choice between +1 Dano Base (a
    // Perícia-scoped Dano Base concept now exists too, same file, but again nothing models
    // an item-granted value of it) or +1 Conjuração (a Magia-effect bonus, same gap as
    // DominioDoManaCompetencyAbility.ARCANISMO_EXPLOSIVA). Needs an
    // Item/Equipamento entity carrying who produced it and which choice was made at
    // creation — Item now carries real per-copy state (Dureza taken, Obra-Prima, Aprimoramentos,
    // a Pedra do Poder) but records no producer, and nothing produces a copy at which a choice
    // could be baked in.
    FORJA_VULCANA("Equipamentos que você produz tem benefícios adicionais: Equipamentos " +
            "Defensivos concedem Resistência à Críticos, além disso concedem Redução de " +
            "Danos Sofridos 1 ou Bônus de +1 em Defesas (definido na criação do item). " +
            "Equipamentos Ofensivos: Margem Crítica Maior +1, além disso possuem Dano Base " +
            "+1 ou Conjuração +1."),

    // TODO: +2 to Dureza recovered when repairing an item/equipment (no longer a dice roll,
    // so that particular blocker is gone), extendable to Magias/Habilidades at 5 Graduações,
    // then the bonus itself becomes +5 total at 10 Graduações — a copy with mutable, damageable
    // Dureza now exists ({@code Item#applyDamage}/{@code getCurrentHardness}), but damage is
    // one-way: there is no repair entry point to add the +2 to, no
    // Dureza-equivalent concept on Magias/Habilidades (doesn't exist either), and no
    // graduation-crossing-a-threshold trigger for the 10th-Graduação bump (same gap as
    // ArtesExcellency.FOCADO/LENDA's Fama trigger).
    REPARO_MELHORADO("Sempre que você reparar um item ou equipamento a Dureza recuperada " +
            "aumenta em +2, com 5 Graduações você pode estender este efeito às suas Magias " +
            "e Habilidades, com 10 Graduações este benefício muda para +5."),

    // TODO: +50% to produced equipment's Dureza — {@code Item#getEffectiveHardness()} is now a
    // real, damageable maximum, but nothing *produces* a copy, so there is no moment at which to
    // apply an increase to the value it is created with.
    AUMENTAR_A_DUREZA("A Dureza dos equipamentos que você produz aumenta em 50%."),

    // TODO: +5 to produced equipment's Carga capacity. Item is real and carries an
    // ItemWeightClass, but Carga (carrying capacity) is a different stat that no Item column
    // holds — and nothing produces a copy to apply the increase at.
    EXPANDIR_CARGA("A capacidade de Carga dos Equipamentos que você produz aumenta em +5.");

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.PROFISSAO;
    }
}
