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

    // TODO: two effects, both blocked. (1) +3 Danos Críticos — no critical-damage system
    // exists yet. (2) lets this Perícia (and other attack Perícias) use the "bônus de
    // conjuração de Habilidades e de Itens" when the roll is used to cast a Magia — that
    // casting bonus doesn't exist yet either (no Ability/Item grants one), and this
    // codebase can't tell whether a given Ataque à Distância roll is a mundane shot or a
    // spell delivery; see org.aventyrs.core.magic.SpellCastingService, which is where this
    // would eventually plug in once Magias and that casting bonus exist.
    LENDA(ExcellencyTier.LENDA, "Danos Críticos +3. No ataque a distância e em outras " +
            "perícias de ataque, pode ser utilizado o bônus de conjuração de Habilidades e " +
            "de Itens quando se utiliza essa Habilidade para conjurar magias.");

    private final ExcellencyTier tier;
    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ATAQUE_A_DISTANCIA;
    }
}
