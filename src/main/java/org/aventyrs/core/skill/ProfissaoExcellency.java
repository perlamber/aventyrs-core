package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Profissão's automatic Excelência bonuses, granted once a character's Profissão graduation
 * reaches each {@link ExcellencyTier}'s threshold.
 */
@Getter
@AllArgsConstructor
public enum ProfissaoExcellency implements SkillExcellency {

    // TODO: -1PE to Equipamento creation/purchase costs, never below 1PE — no
    // Item/Equipamento entity, "PE" currency, or cost system exists yet.
    FOCADO(ExcellencyTier.FOCADO, "Custos de criação e compras de Equipamentos reduzido em " +
            "-1PE (mínimo 1PE)."),

    PRODIGIO(ExcellencyTier.PRODIGIO, "GD reduzido em -1 nível.") {
        @Override
        public int getDifficultyReduction() {
            return 1;
        }
    },

    // TODO: -2PE more on top of Focado's -1PE (totaling -3PE), still floored at 1PE — per
    // this codebase's convention, "muda para -3PE" is the cumulative total across tiers, not
    // a standalone override (see ArtesExcellency.LENDA, whose old/new rules text confirms
    // this reading) — blocked on the same missing Item/Equipamento/PE-cost system as FOCADO.
    LENDA(ExcellencyTier.LENDA, "Redução dos Custos de criação e Compras de Equipamentos " +
            "muda para -3PE (mínimo 1PE).");

    private final ExcellencyTier tier;
    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.PROFISSAO;
    }
}
