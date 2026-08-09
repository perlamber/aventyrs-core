package org.aventyrs.core.skill.dirigirecavalgar;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.skill.ExcellencyTier;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;

/**
 * Dirigir e Cavalgar's automatic Excelência bonuses, granted once a character's Dirigir e
 * Cavalgar graduation reaches each {@link ExcellencyTier}'s threshold.
 */
@Getter
@AllArgsConstructor
public enum DirigirECavalgarExcellency implements SkillExcellency {

    // TODO: +1UD to the Movimento Base of mounts/vehicles the character controls — no
    // movement/distance system exists yet (same gap as
    // AtletismoCompetencyAbility.SALTO_PODEROSO).
    FOCADO(ExcellencyTier.FOCADO, "Movimento Base de Montarias e Veículos +1UD."),

    PRODIGIO(ExcellencyTier.PRODIGIO, "GD reduzido em -1 nível.") {
        @Override
        public int getDifficultyReduction() {
            return 1;
        }
    },

    // TODO: +3UD more to Movimento Base de Montarias e Veículos, on top of Focado's +1UD
    // (totaling +4UD once both are unlocked) — same missing movement/distance system as
    // Focado.
    LENDA(ExcellencyTier.LENDA, "Movimento Base de Montarias e Veículos +3UD.");

    private final ExcellencyTier tier;
    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.DIRIGIR_E_CAVALGAR;
    }
}
