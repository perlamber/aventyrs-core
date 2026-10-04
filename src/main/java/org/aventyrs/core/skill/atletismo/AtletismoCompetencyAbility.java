package org.aventyrs.core.skill.atletismo;

import org.aventyrs.core.character.MovementMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.skill.CompetencyUses;
import org.aventyrs.core.skill.UseWindow;
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

    /**
     * Real (core 0.0.102) as a limited use: 1/2/3 per Cena ({@link CompetencyUses}). What a use does — one
     * movement that ignores Terreno Difícil — is the mover's: the client prices that movement's difficult hexes
     * as ordinary ones.
     */
    SALTO_PODEROSO("Uma vez por Cena você pode ignorar Terreno Difícil, novos usos desta " +
            "Habilidade são adquiridos ao alcançar a 5ª e 10ª Graduação.") {
        @Override
        public int resolveUseLimit(final Character holder) {
            return tiered(holder, SkillType.ATLETISMO);
        }

        @Override
        public UseWindow getUseWindow() {
            return UseWindow.CENA;
        }
    },

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

    /** 1, then +1 at the 5ª and +1 at the 10ª Graduação in {@code skill} — 1 with no holder. */
    private static int tiered(final Character holder, final SkillType skill) {
        int graduation = holder == null ? 0 : holder.getEffectiveGraduation(skill);
        return 1 + (graduation >= 5 ? 1 : 0) + (graduation >= 10 ? 1 : 0);
    }

}
