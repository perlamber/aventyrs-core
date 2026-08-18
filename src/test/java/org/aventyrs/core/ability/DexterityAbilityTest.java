package org.aventyrs.core.ability;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DexterityAbilityTest {

    @Test
    void everyAbilityBelongsToDexterity() {
        for (DexterityAbility ability : DexterityAbility.values()) {
            assertEquals(AttributeDomain.DEXTERITY, ability.getAttributeDomain());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (DexterityAbility ability : DexterityAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, DexterityAbility.values().length);
    }

    @Test
    void onlyPassosLongosGrantsAMovementModifier() {
        ModifierResolver modifierResolver = new ModifierResolverImpl();
        for (DexterityAbility ability : DexterityAbility.values()) {
            int expected = ability == DexterityAbility.PASSOS_LONGOS ? 1 : 0;
            assertEquals(expected, modifierResolver.sumModifiers(ability, ModifierType.MOVEMENT));
        }
    }

    private SceneContext combatContext(final int currentRound) {
        return new SceneContext(List.of(), List.of(), Map.of(), null, true, currentRound, false);
    }

    @Test
    void letalidadeProgressivaGrantsPlusOneMarginInRoundsOneAndTwo() {
        assertEquals(1, DexterityAbility.LETALIDADE_PROGRESSIVA.resolveCriticalMarginIncrease(SkillType.ATAQUE_A_DISTANCIA, combatContext(1)));
        assertEquals(1, DexterityAbility.LETALIDADE_PROGRESSIVA.resolveCriticalMarginIncrease(SkillType.ATAQUE_A_DISTANCIA, combatContext(2)));
    }

    @Test
    void letalidadeProgressivaGrantsPlusTwoMarginInRoundsThreeAndFour() {
        assertEquals(2, DexterityAbility.LETALIDADE_PROGRESSIVA.resolveCriticalMarginIncrease(SkillType.ATAQUE_A_DISTANCIA, combatContext(3)));
        assertEquals(2, DexterityAbility.LETALIDADE_PROGRESSIVA.resolveCriticalMarginIncrease(SkillType.ATAQUE_A_DISTANCIA, combatContext(4)));
    }

    @Test
    void letalidadeProgressivaGrantsPlusThreeMarginFromRoundFive() {
        assertEquals(3, DexterityAbility.LETALIDADE_PROGRESSIVA.resolveCriticalMarginIncrease(SkillType.ATAQUE_A_DISTANCIA, combatContext(5)));
    }

    @Test
    void letalidadeProgressivaGrantsNoMarginFromRoundSixOnward() {
        assertEquals(0, DexterityAbility.LETALIDADE_PROGRESSIVA.resolveCriticalMarginIncrease(SkillType.ATAQUE_A_DISTANCIA, combatContext(6)));
        assertEquals(0, DexterityAbility.LETALIDADE_PROGRESSIVA.resolveCriticalMarginIncrease(SkillType.ATAQUE_A_DISTANCIA, combatContext(10)));
    }

    @Test
    void letalidadeProgressivaGrantsNoMarginBeforeCombatStarts() {
        assertEquals(0, DexterityAbility.LETALIDADE_PROGRESSIVA.resolveCriticalMarginIncrease(SkillType.ATAQUE_A_DISTANCIA, combatContext(0)));
    }

    @Test
    void letalidadeProgressivaGrantsNoMarginOutsideACombatScene() {
        SceneContext nonCombat = new SceneContext(List.of(), List.of(), Map.of(), null, false, 1, false);

        assertEquals(0, DexterityAbility.LETALIDADE_PROGRESSIVA.resolveCriticalMarginIncrease(SkillType.ATAQUE_A_DISTANCIA, nonCombat));
    }

    @Test
    void letalidadeProgressivaGrantsNoMarginWithoutASceneContext() {
        assertEquals(0, DexterityAbility.LETALIDADE_PROGRESSIVA.resolveCriticalMarginIncrease(SkillType.ATAQUE_A_DISTANCIA, null));
    }

    @Test
    void letalidadeProgressivaGrantsNoMarginForASkillOtherThanAtaqueADistancia() {
        assertEquals(0, DexterityAbility.LETALIDADE_PROGRESSIVA.resolveCriticalMarginIncrease(SkillType.ATAQUE_CORPO_A_CORPO, combatContext(1)));
    }

    @Test
    void onlyLetalidadeProgressivaEverResolvesACriticalMarginIncrease() {
        for (DexterityAbility ability : DexterityAbility.values()) {
            if (ability != DexterityAbility.LETALIDADE_PROGRESSIVA) {
                assertEquals(0, ability.resolveCriticalMarginIncrease(SkillType.ATAQUE_A_DISTANCIA, combatContext(1)));
            }
        }
    }

    @Test
    void precisaoGrantsVantagemForADestrezaBasedRoll() {
        assertEquals(Optional.of(Skill.ADVANTAGE_BONUS), DexterityAbility.PRECISAO.resolveFirstRollOfTurnBonus(AttributeDomain.DEXTERITY));
    }

    @Test
    void precisaoGrantsNothingForARollBasedOnAnotherAttribute() {
        assertEquals(Optional.empty(), DexterityAbility.PRECISAO.resolveFirstRollOfTurnBonus(AttributeDomain.STRENGTH));
    }

    @Test
    void onlyPrecisaoEverResolvesAFirstRollOfTurnBonus() {
        for (DexterityAbility ability : DexterityAbility.values()) {
            if (ability != DexterityAbility.PRECISAO) {
                assertEquals(Optional.empty(), ability.resolveFirstRollOfTurnBonus(AttributeDomain.DEXTERITY));
            }
        }
    }
}
