package org.aventyrs.core.skill.esquivaeaparar;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.ExcellencyTier;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.artes.ArtesExcellency;

/**
 * Esquiva e Aparar's automatic Excelência bonuses, granted once a character's Esquiva e
 * Aparar graduation reaches each {@link ExcellencyTier}'s threshold. Focado and Lenda both
 * raise the same Defesas bonus and add up rather than Lenda replacing Focado's — +1 then +2
 * more, totaling +3 once both are active (mirrors {@link ArtesExcellency#LENDA}'s modeling).
 */
@Getter
@AllArgsConstructor
public enum EsquivaEApararExcellency implements SkillExcellency {

    FOCADO(ExcellencyTier.FOCADO, "Defesas +1.") {
        @Modifier(ModifierType.SKILL_ROLL_BONUS)
        public int defesasBonus() {
            return 1;
        }
    },

    PRODIGIO(ExcellencyTier.PRODIGIO, "GD reduzido em -1 nível.") {
        @Override
        public int getDifficultyReduction() {
            return 1;
        }
    },

    LENDA(ExcellencyTier.LENDA, "Bônus em Defesas aumenta para +3.") {
        @Modifier(ModifierType.SKILL_ROLL_BONUS)
        public int defesasBonus() {
            return 2;
        }
    };

    private final ExcellencyTier tier;
    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ESQUIVA_E_APARAR;
    }
}
