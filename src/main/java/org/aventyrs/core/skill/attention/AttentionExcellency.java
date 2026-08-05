package org.aventyrs.core.skill.attention;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.ExcellencyTier;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;

/**
 * Atenção's automatic Excelência bonuses, granted once a character's Atenção graduation
 * reaches each {@link ExcellencyTier}'s threshold.
 */
@Getter
@AllArgsConstructor
public enum AttentionExcellency implements SkillExcellency {

    FOCADO(ExcellencyTier.FOCADO, "Reação adicional.") {
        @Modifier(ModifierType.REACTIONS)
        public int reactionBonus() {
            return 1;
        }
    },

    PRODIGIO(ExcellencyTier.PRODIGIO, "GD reduzido em -1 nível.") {
        @Override
        public int getDifficultyReduction() {
            return 1;
        }
    },

    // TODO: +2 to the Iniciativa Ego, granted once when Atenção graduation first reaches
    // 10 — nothing yet detects a graduation crossing a threshold to trigger this
    // automatically (same gap as ArtesExcellency.FOCADO/LENDA).
    LENDA(ExcellencyTier.LENDA, "Iniciativa +2.");

    private final ExcellencyTier tier;
    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ATTENTION;
    }
}
