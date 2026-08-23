package org.aventyrs.core.skill.persuasao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.skill.ExcellencyTier;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;

/**
 * Persuasão's automatic Excelência bonuses, granted once a character's Persuasão graduation
 * reaches each {@link ExcellencyTier}'s threshold.
 */
@Getter
@AllArgsConstructor
public enum PersuasaoExcellency implements SkillExcellency {

    // TODO: +1 per 5 points of Fama (Positiva or Negativa, whichever applies) to Persuasão
    // rolls — org.aventyrs.core.sheet.CombatantSheet already tracks famaPositiva/
    // famaNegativa directly, but this bonus is a per-character variable scaling with that
    // count, not a fixed constant, so the @Modifier/ModifierResolver mechanism (parameterless
    // methods returning a fixed value) can't express it; "a que for aplicável" also implies
    // choosing which Fama applies per roll/target, which isn't tracked either.
    FOCADO(ExcellencyTier.FOCADO, "Você recebe Bônus de +1 para 5 pontos de Fama (a que for " +
            "aplicável) às suas rolagens de Persuasão."),

    PRODIGIO(ExcellencyTier.PRODIGIO, "GD reduzido em -1 nível.") {
        @Override
        public int getDifficultyReduction() {
            return 1;
        }
    },

    // TODO: same mechanic as FOCADO, just +2 instead of +1 per 5 points of Fama — blocked on
    // the exact same variable-scaling-modifier gap.
    LENDA(ExcellencyTier.LENDA, "Os Bônus em rolagens aumentam para +2 à cada 5 Pontos de " +
            "Fama.");

    private final ExcellencyTier tier;
    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.PERSUASAO;
    }
}
