package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Conhecimentos' automatic Excelência bonuses, granted once a character's Conhecimentos
 * graduation reaches each {@link ExcellencyTier}'s threshold.
 */
@Getter
@AllArgsConstructor
public enum ConhecimentosExcellency implements SkillExcellency {

    // TODO: +1 to every Perícia's Margem Crítica Menor except Perícias de Ataque and
    // Esquiva e Aparar — this is a cross-skill effect (same structural gap as
    // AtletismoExcellency.LENDA/AttentionCompetencyAbility.ARDIL_DE_MARPLE, which would need
    // to apply across every <Skill>Interaction, not just ConhecimentosInteraction), no
    // Margem Crítica (critical-margin) concept exists yet, and there's no per-skill
    // "excluded from this bonus" categorization either.
    FOCADO(ExcellencyTier.FOCADO, "Margem Crítica Menor de Perícias aumentada em +1 " +
            "números (não aplicável a Perícias de Ataque e Esquiva e Aparar)."),

    PRODIGIO(ExcellencyTier.PRODIGIO, "GD reduzido em -1 nível.") {
        @Override
        public int getDifficultyReduction() {
            return 1;
        }
    },

    // TODO: +1 to every Perícia's Margem Crítica Menor — no exclusion this time (Perícias de
    // Ataque/Esquiva e Aparar, excluded from FOCADO, are covered here), and this stacks with
    // FOCADO where it also applies, so a Perícia FOCADO already covers ends up +2 while an
    // excluded one only gets LENDA's +1. Same cross-skill/critical-margin gaps as FOCADO.
    LENDA(ExcellencyTier.LENDA, "Margem Crítica Menor de todas as Perícias aumentada em +1 " +
            "número (cumulativo com efeito Focado).");

    private final ExcellencyTier tier;
    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.CONHECIMENTOS;
    }
}
