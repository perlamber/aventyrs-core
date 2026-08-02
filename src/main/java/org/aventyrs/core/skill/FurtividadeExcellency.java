package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Furtividade's automatic Excelência bonuses, granted once a character's Furtividade
 * graduation reaches each {@link ExcellencyTier}'s threshold.
 */
@Getter
@AllArgsConstructor
public enum FurtividadeExcellency implements SkillExcellency {

    // TODO: once per Cena, a Furtividade-based Perícia/Habilidade's Tempo de Ação costs -1PA
    // — needs both a way to identify which roll is "Furtividade-based" (not just
    // FurtividadeInteraction's own roll — a cross-skill/ability scope, same gap as
    // FurtividadeCompetencyAbility.ACAO_SURPRESA) and a once-per-Cena usage-limiting
    // mechanism (same gap as DominioDoManaExcellency.LENDA's once-per-Rodada limit), neither
    // of which exist yet.
    FOCADO(ExcellencyTier.FOCADO, "Uma vez por Cena, uso de Perícia ou Habilidade baseada em " +
            "Furtividade tem o Tempo de Ação reduzido em -1PA."),

    PRODIGIO(ExcellencyTier.PRODIGIO, "GD reduzido em -1 nível.") {
        @Override
        public int getDifficultyReduction() {
            return 1;
        }
    },

    // TODO: same effect as FOCADO, but usable three times per Cena instead of once — blocked
    // on the exact same two missing pieces (cross-skill "Furtividade-based" scoping and
    // once-per-Cena usage tracking).
    LENDA(ExcellencyTier.LENDA, "Três vezes por Cena, uso de Perícia ou Habilidade baseada " +
            "em Furtividade tem o Tempo de Ação reduzido em -1PA.");

    private final ExcellencyTier tier;
    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.FURTIVIDADE;
    }
}
