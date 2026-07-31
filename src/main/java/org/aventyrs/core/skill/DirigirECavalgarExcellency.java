package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

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

    // TODO: Vantagem on Perícia rolls in general (not just Dirigir e Cavalgar's own) while
    // riding/driving — this would need to apply across every <Skill>Interaction, not just
    // DirigirECavalgarInteraction, and there's no "is the character currently
    // riding/driving" context tracked anywhere to gate it on.
    LENDA(ExcellencyTier.LENDA, "Vantagem em rolagens de Perícias enquanto cavalgando ou " +
            "dirigindo.");

    private final ExcellencyTier tier;
    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.DIRIGIR_E_CAVALGAR;
    }
}
