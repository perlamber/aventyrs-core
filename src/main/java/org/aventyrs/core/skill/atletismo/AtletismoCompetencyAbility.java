package org.aventyrs.core.skill.atletismo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * The Habilidades de Competência available to characters trained in Atletismo.
 */
@Getter
@AllArgsConstructor
public enum AtletismoCompetencyAbility implements SkillCompetencyAbility {

    // TODO: grants Movimento Base Vertical — the general terrestrial Movimento Base stat
    // now exists (MovementService), but this is a different sub-stat (climbing specifically)
    // that stat doesn't track — no vertical-movement/terrain system exists yet.
    ALPINISTA_VELOZ("Você recebe Movimento Base Vertical."),

    // TODO: once per Cena, ignore Terreno Difícil, gaining an additional use at the 5th and
    // 10th Graduação — needs a Terreno Difícil/terrain system, a Cena-scoped usage-limiting
    // mechanism, and a graduation-crossing-a-threshold trigger for the extra uses (same gap
    // as ArtesExcellency.FOCADO/LENDA's Fama trigger), none of which exist yet.
    SALTO_PODEROSO("Uma vez por Cena você pode ignorar Terreno Difícil, novos usos desta " +
            "Habilidade são adquiridos ao alcançar a 5ª e 10ª Graduação."),

    // TODO: grants Movimento Base de Natação — same story as ALPINISTA_VELOZ: the general
    // Movimento Base stat exists now, but swimming is a different sub-stat it doesn't track.
    ANFIBIO("Você recebe Movimento Base de Natação."),

    // Substitutes Força for Destreza — see SkillCompetencyAbility.getSubstituteAttributeDomain().
    ACROBATA("Você pode substituir o Atributo Base desta perícia por Destreza.") {
        @Override
        public Optional<AttributeDomain> getSubstituteAttributeDomain() {
            return Optional.of(AttributeDomain.DEXTERITY);
        }
    },

    PASSO_LARGO("Movimento Base aumenta em +2UD.") {
        @Modifier(ModifierType.MOVEMENT)
        public int movementBonus() {
            return 2;
        }
    };

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ATLETISMO;
    }
}
