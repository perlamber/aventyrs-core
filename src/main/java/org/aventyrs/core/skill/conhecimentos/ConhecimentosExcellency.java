package org.aventyrs.core.skill.conhecimentos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.skill.ExcellencyTier;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;

/**
 * Conhecimentos' automatic Excelência bonuses, granted once a character's Conhecimentos
 * graduation reaches each {@link ExcellencyTier}'s threshold.
 */
@Getter
@AllArgsConstructor
public enum ConhecimentosExcellency implements SkillExcellency {

    // TODO: +1 to every Perícia's Margem Crítica Menor except Perícias de Ataque and Esquiva e
    // Aparar. Two of the three blockers once listed here are gone: the Margem Crítica mechanism
    // is real and consumed (SkillRoll#getCriticalResult(int)), and cross-skill scope is no
    // longer structural either — resolveCriticalMarginIncrease is passed the Perícia being
    // rolled, so one constant can answer for every <Skill>Interaction, and the exclusion is
    // expressible as SkillType#isAttackSkill() plus an ESQUIVA_E_APARAR check. What remains is
    // just the hook: SkillExcellency is the one ability source
    // AbstractSkillInteraction#sumCriticalMarginIncrease does not scan — it has no
    // resolveCriticalMarginIncrease hook, unlike AttributeAbility/SkillCompetencyAbility/
    // EgoAdvantage/Feat, which all carry one and are all summed for real.
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
    // excluded one only gets LENDA's +1. Same missing SkillExcellency hook as FOCADO, and
    // nothing else — this tier needs no exclusion, so it is the simpler of the two.
    LENDA(ExcellencyTier.LENDA, "Margem Crítica Menor de todas as Perícias aumentada em +1 " +
            "número (cumulativo com efeito Focado).");

    private final ExcellencyTier tier;
    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.CONHECIMENTOS;
    }
}
