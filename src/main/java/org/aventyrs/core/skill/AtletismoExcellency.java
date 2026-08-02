package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;

/**
 * Atletismo's automatic Excelência bonuses, granted once a character's Atletismo graduation
 * reaches each {@link ExcellencyTier}'s threshold.
 */
@Getter
@AllArgsConstructor
public enum AtletismoExcellency implements SkillExcellency {

    FOCADO(ExcellencyTier.FOCADO, "Ação Livre adicional.") {
        @Modifier(ModifierType.FREE_ACTIONS)
        public int freeActionsBonus() {
            return 1;
        }
    },

    PRODIGIO(ExcellencyTier.PRODIGIO, "GD reduzido em -1 nível.") {
        @Override
        public int getDifficultyReduction() {
            return 1;
        }
    },

    LENDA(ExcellencyTier.LENDA, "+1PA.") {
        @Modifier(ModifierType.ACTION_POINTS)
        public int actionPointsBonus() {
            return 1;
        }
    };

    private final ExcellencyTier tier;
    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ATLETISMO;
    }
}
