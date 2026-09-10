package org.aventyrs.core.skill.ataquecorpoacorpo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.ExcellencyTier;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;

/**
 * Ataque Corpo-a-Corpo's automatic Excelência bonuses, granted once a character's Ataque
 * Corpo-a-Corpo graduation reaches each {@link ExcellencyTier}'s threshold.
 */
@Getter
@AllArgsConstructor
public enum AtaqueCorpoACorpoExcellency implements SkillExcellency {

    FOCADO(ExcellencyTier.FOCADO, "Movimento Base +2UD.") {
        @Modifier(ModifierType.MOVEMENT)
        public int movementBonus() {
            return 2;
        }
    },

    PRODIGIO(ExcellencyTier.PRODIGIO, "GD reduzido em -1 nível.") {
        @Override
        public int getDifficultyReduction() {
            return 1;
        }
    },

    // TODO: +2 to this Perícia's Margem Crítica Menor. The Margem Crítica mechanism itself is
    // real and consumed (SkillRoll#getCriticalResult(int), fed by
    // AbstractSkillInteraction#sumCriticalMarginIncrease — AtaqueCorpoACorpoCompetencyAbility
    // .ATAQUE_PRECISO grants exactly this shape for real). What blocks it is narrower: SkillExcellency is the one ability source
    // AbstractSkillInteraction#sumCriticalMarginIncrease does not scan — it has no
    // resolveCriticalMarginIncrease hook, unlike AttributeAbility/SkillCompetencyAbility/
    // EgoAdvantage/Feat, which all carry one and are all summed for real.
    LENDA(ExcellencyTier.LENDA, "Margem Crítica Menor aumentada em +2 números.");

    private final ExcellencyTier tier;
    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ATAQUE_CORPO_A_CORPO;
    }
}
