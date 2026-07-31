package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Ataque à Distância's automatic Excelência bonuses, granted once a character's Ataque à
 * Distância graduation reaches each {@link ExcellencyTier}'s threshold.
 */
@Getter
@AllArgsConstructor
public enum AtaqueADistanciaExcellency implements SkillExcellency {

    // TODO: +1 to the wielded weapon's Dano Base — no weapon-damage system exists yet.
    FOCADO(ExcellencyTier.FOCADO, "Danos Base da Arma +1."),

    PRODIGIO(ExcellencyTier.PRODIGIO, "GD reduzido em -1 nível.") {
        @Override
        public int getDifficultyReduction() {
            return 1;
        }
    },

    // TODO: +3 Danos Críticos — no critical-damage system exists yet (same gap as
    // AtaqueADistanciaCompetencyAbility.MIRAR_NA_CABECA).
    LENDA(ExcellencyTier.LENDA, "Danos Críticos +3.");

    private final ExcellencyTier tier;
    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ATAQUE_A_DISTANCIA;
    }
}
