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
            "-1PE, estes efeitos não reduzem custos para menos de 1PE."),

    PRODIGIO(ExcellencyTier.PRODIGIO, "GD reduzido em -1 nível.") {
        @Override
        public int getDifficultyReduction() {
            return 1;
        }
    },

    // TODO: replaces (not adds to) FOCADO's -1PE with -3PE, still floored at 1PE — the rules
    // text reads as a new total ("muda para -3PE"), not an additional -3PE on top of
    // Focado's -1PE, unlike the additive Excellency tiers seen elsewhere (e.g.
    // ArtesExcellency.LENDA) — blocked on the same missing Item/Equipamento/PE-cost system
    // as FOCADO.
    LENDA(ExcellencyTier.LENDA, "Redução dos Custos de criação e Compras de Equipamentos " +
            "muda para -3PE, mas ainda não reduzem os custos para menos que 1PE.");

    private final ExcellencyTier tier;
    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.PROFISSAO;
    }
}
