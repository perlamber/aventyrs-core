package org.aventyrs.core.skill.ataquecorpoacorpo;

import lombok.AllArgsConstructor;
import lombok.Getter;
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

    // TODO: +2UD to Movimento Base — no movement/distance system exists yet (same gap as
    // AtletismoCompetencyAbility.SALTO_PODEROSO / DirigirECavalgarExcellency.FOCADO).
    FOCADO(ExcellencyTier.FOCADO, "Movimento Base +2UD."),

    PRODIGIO(ExcellencyTier.PRODIGIO, "GD reduzido em -1 nível.") {
        @Override
        public int getDifficultyReduction() {
            return 1;
        }
    },

    // TODO: +2 to this Perícia's Margem Crítica Menor — no Margem Crítica (critical-margin)
    // concept exists yet (same gap as AtaqueCorpoACorpoCompetencyAbility.ATAQUE_PRECISO).
    LENDA(ExcellencyTier.LENDA, "Margem Crítica Menor aumentada em +2 números.");

    private final ExcellencyTier tier;
    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ATAQUE_CORPO_A_CORPO;
    }
}
