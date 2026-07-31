package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Atenção's automatic Excelência bonuses, granted once a character's Atenção graduation
 * reaches each {@link ExcellencyTier}'s threshold.
 */
@Getter
@AllArgsConstructor
public enum AttentionExcellency implements SkillExcellency {

    // TODO: grants an extra Reação — no Reação system exists yet.
    FOCADO(ExcellencyTier.FOCADO, "Reação adicional."),

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
