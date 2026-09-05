package org.aventyrs.core.skill.profissao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

/**
 * The Habilidades de Competência available to characters trained in Profissão — almost all about
 * equipment a character <i>produces or repairs</i>. The fabrication/reparo pipeline now exists
 * ({@code org.aventyrs.core.character.services.EquipmentCraftingService}), so three of these are
 * real: {@link #CONSTRUTOR_EFICIENTE} scales Tempo de Produção, {@link #AUMENTAR_A_DUREZA} scales
 * a forged copy's Dureza, and {@link #REPARO_MELHORADO} adds to repaired Dureza — via the
 * {@code resolveProductionTimeMultiplier}/{@code resolveProducedHardnessMultiplier}/{@code
 * resolveRepairHardnessBonus} hooks on {@link SkillCompetencyAbility}. {@link #FORJA_VULCANA} and
 * {@link #EXPANDIR_CARGA} stay unexpressed — see their TODOs for the narrower remaining gap
 * (Resistência a Críticos / Margem Crítica Maior / Carga, none of which exist anywhere).
 */
@Getter
@AllArgsConstructor
public enum ProfissaoCompetencyAbility implements SkillCompetencyAbility {

    CONSTRUTOR_EFICIENTE("Tempo de Produção de itens e equipamentos reduzido em 20%.") {
        @Override
        public double resolveProductionTimeMultiplier() {
            return 0.8;
        }
    },

    // TODO: only the "quem produziu" half is real now (EquipmentCraftingService#forge stamps
    // AbstractItem#producedByCharacterId). The benefits themselves stay unexpressed: produced
    // Equipamentos Defensivos grant Resistência a Críticos (a critical-hit-resistance concept
    // that exists nowhere) plus an item-scoped choice between Redução de Danos Sofridos 1 or
    // +1 Defesas (RD is real on the character, but not as a per-produced-copy value); produced
    // Equipamentos Ofensivos grant Margem Crítica Maior +1 (a *different* axis from every
    // skill's Margem Crítica Menor — nothing models the Maior axis or an item-scoped value)
    // plus a choice between +1 Dano Base or +1 Conjuração (again, no item-granted value of
    // either — same gap as DominioDoManaCompetencyAbility.ARCANISMO_EXPLOSIVA). And nothing
    // records the creation-time choice: forge() takes no such parameter.
    FORJA_VULCANA("Equipamentos que você produz tem benefícios adicionais: Equipamentos " +
            "Defensivos concedem Resistência à Críticos, além disso concedem Redução de " +
            "Danos Sofridos 1 ou Bônus de +1 em Defesas (definido na criação do item). " +
            "Equipamentos Ofensivos: Margem Crítica Maior +1, além disso possuem Dano Base " +
            "+1 ou Conjuração +1."),

    // The "+2 Dureza recuperada" is real, via resolveRepairHardnessBonus, and so is the
    // "muda para +5 com 10 Graduações" step. TODO: the "com 5 Graduações estende às suas
    // Magias e Habilidades" clause stays unimplemented — neither a Magia nor a Habilidade has
    // any Dureza-equivalent pool for a repair to restore.
    REPARO_MELHORADO("Sempre que você reparar um item ou equipamento a Dureza recuperada " +
            "aumenta em +2, com 5 Graduações você pode estender este efeito às suas Magias " +
            "e Habilidades, com 10 Graduações este benefício muda para +5.") {
        @Override
        public int resolveRepairHardnessBonus(final Character holder) {
            return profissaoGraduation(holder) >= EXTENDED_REPAIR_GRADUATION ? 5 : 2;
        }
    },

    AUMENTAR_A_DUREZA("A Dureza dos equipamentos que você produz aumenta em 50%.") {
        @Override
        public double resolveProducedHardnessMultiplier() {
            return 1.5;
        }
    },

    // TODO: +5 to produced equipment's Carga capacity. Carga (carrying capacity) is a stat no
    // Item column holds and nothing on Character reads — the fabrication pipeline gives this a
    // moment to apply at (EquipmentCraftingService#forge), but there is still no value to raise.
    EXPANDIR_CARGA("A capacidade de Carga dos Equipamentos que você produz aumenta em +5.");

    /** REPARO_MELHORADO's "+2 muda para +5" happens once Profissão Graduação reaches this. */
    private static final int EXTENDED_REPAIR_GRADUATION = 10;

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.PROFISSAO;
    }

    /** holder's Profissão Graduação, or 0 when untrained or when holder is {@code null}. */
    private static int profissaoGraduation(final Character holder) {
        if (holder == null) {
            return 0;
        }
        CharacterSkill profissao = holder.getSkills().get(SkillType.PROFISSAO);
        return profissao == null ? 0 : profissao.getGraduation().getGraduationValue();
    }
}
