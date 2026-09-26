package org.aventyrs.core.skill.atletismo;

import org.aventyrs.core.character.MovementMode;
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

    // Real — grants MovementMode.CLIMB, read by MovementService#hasMovementMode.
    ALPINISTA_VELOZ("Você recebe Movimento Base Vertical.") {
        @Override
        public boolean grantsMovementMode(final MovementMode mode) {
            return mode == MovementMode.CLIMB;
        }
    },

    // TODO: once per Cena, ignore Terreno Difícil, gaining an additional use at the 5th and
    // 10th Graduação — the Terreno Difícil cost is real (MovementTerrainService), but this is a
    // spent use rather than a passive, so it needs a Cena-scoped usage-limiting
    // mechanism, and a graduation-crossing-a-threshold trigger for the extra uses (same gap
    // as ArtesExcellency.FOCADO/LENDA's Fama trigger), none of which exist yet.
    SALTO_PODEROSO("Uma vez por Cena você pode ignorar Terreno Difícil, novos usos desta " +
            "Habilidade são adquiridos ao alcançar a 5ª e 10ª Graduação."),

    // Real — grants MovementMode.SWIM.
    ANFIBIO("Você recebe Movimento Base de Natação.") {
        @Override
        public boolean grantsMovementMode(final MovementMode mode) {
            return mode == MovementMode.SWIM;
        }
    },

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
