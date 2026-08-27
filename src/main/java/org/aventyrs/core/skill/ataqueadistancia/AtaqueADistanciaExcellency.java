package org.aventyrs.core.skill.ataqueadistancia;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.skill.ExcellencyTier;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;

/**
 * Ataque à Distância's automatic Excelência bonuses, granted once a character's Ataque à
 * Distância graduation reaches each {@link ExcellencyTier}'s threshold.
 */
@Getter
@AllArgsConstructor
public enum AtaqueADistanciaExcellency implements SkillExcellency {

    /**
     * "+1 to the wielded weapon's Dano Base" — real since {@link
     * org.aventyrs.core.character.DamageBase}/{@code DamageBaseService} landed. It raises the
     * Dano Base of Ataque à Distância attacks only, which needs no check of its own here: the
     * service consults just the attacking Perícia's own unlocked tiers.
     */
    FOCADO(ExcellencyTier.FOCADO, "Danos Base da Arma +1.") {
        @Override
        public int resolveDamageBaseIncrease() {
            return 1;
        }
    },

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
