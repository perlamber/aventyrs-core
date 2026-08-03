package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AtaqueADistanciaCompetencyAbilityTest {

    @Test
    void everyAbilityBelongsToAtaqueADistancia() {
        for (AtaqueADistanciaCompetencyAbility ability : AtaqueADistanciaCompetencyAbility.values()) {
            assertEquals(SkillType.ATAQUE_A_DISTANCIA, ability.getSkillType());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (AtaqueADistanciaCompetencyAbility ability : AtaqueADistanciaCompetencyAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, AtaqueADistanciaCompetencyAbility.values().length);
    }

    @Test
    void noAbilityReducesDifficultyOrGrantsASkillRollBonusYet() {
        ModifierResolver modifierResolver = new ModifierResolverImpl();
        for (AtaqueADistanciaCompetencyAbility ability : AtaqueADistanciaCompetencyAbility.values()) {
            assertEquals(0, ability.getDifficultyReduction());
            assertEquals(0, modifierResolver.sumModifiers(ability, ModifierType.SKILL_ROLL_BONUS));
        }
    }

    @Test
    void onlyDisparoArcanoSubstitutesTheBaseAttribute() {
        for (AtaqueADistanciaCompetencyAbility ability : AtaqueADistanciaCompetencyAbility.values()) {
            if (ability == AtaqueADistanciaCompetencyAbility.DISPARO_ARCANO) {
                assertEquals(Optional.of(AttributeDomain.FOCUS), ability.getSubstituteAttributeDomain());
            } else {
                assertEquals(Optional.empty(), ability.getSubstituteAttributeDomain());
            }
        }
    }
}
