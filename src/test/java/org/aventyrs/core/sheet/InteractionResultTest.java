package org.aventyrs.core.sheet;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.DifficultyLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class InteractionResultTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void builderAssignsTheNextInteractableAndResultStatus() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet next = CharacterSheet.of(character, new Player());

        InteractionResult result = InteractionResult.builder()
                .nextInteractable(next)
                .resultStatus(CharacterStatus.HIGH_LIFE)
                .build();

        assertSame(next, result.getNextInteractable());
        assertEquals(CharacterStatus.HIGH_LIFE, result.getResultStatus());
    }

    @Test
    void temporaryBonusFieldsStayNullWhenNotSet() {
        InteractionResult result = InteractionResult.builder()
                .skillRollBonus(3)
                .build();

        assertNull(result.getTemporaryBonusValue());
        assertNull(result.getTemporaryBonusModifierType());
        assertNull(result.getTemporaryBonusRounds());
        assertNull(result.getTemporaryBonusScope());
    }

    @Test
    void rollResolutionFieldsStayNullWhenNotSet() {
        InteractionResult result = InteractionResult.builder()
                .skillRollBonus(3)
                .build();

        assertNull(result.getReachedDifficultyLevel());
        assertNull(result.getCriticalResult());
    }

    @Test
    void builderAssignsTheReachedDifficultyLevelAndCriticalResult() {
        InteractionResult result = InteractionResult.builder()
                .reachedDifficultyLevel(DifficultyLevel.MEDIUM)
                .criticalResult(CriticalResult.ACERTO_CRITICO_MENOR)
                .build();

        assertEquals(DifficultyLevel.MEDIUM, result.getReachedDifficultyLevel());
        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, result.getCriticalResult());
    }

    @Test
    void builderAssignsATemporaryBonusScopedToASpecificSkill() {
        InteractionResult result = InteractionResult.builder()
                .temporaryBonusValue(2)
                .temporaryBonusModifierType(ModifierType.ATLETISMO_ROLL_BONUS)
                .temporaryBonusRounds(1)
                .temporaryBonusScope(TargetScope.SINGLE_TARGET)
                .build();

        assertEquals(2, result.getTemporaryBonusValue());
        assertEquals(ModifierType.ATLETISMO_ROLL_BONUS, result.getTemporaryBonusModifierType());
        assertEquals(1, result.getTemporaryBonusRounds());
        assertEquals(TargetScope.SINGLE_TARGET, result.getTemporaryBonusScope());
    }

    @Test
    void builderAssignsATemporaryBonusThatAppliesBroadlyToAllies() {
        InteractionResult result = InteractionResult.builder()
                .temporaryBonusValue(1)
                .temporaryBonusModifierType(ModifierType.SKILL_ROLL_BONUS)
                .temporaryBonusRounds(2)
                .temporaryBonusScope(TargetScope.ALLIES)
                .build();

        assertEquals(1, result.getTemporaryBonusValue());
        assertEquals(ModifierType.SKILL_ROLL_BONUS, result.getTemporaryBonusModifierType());
        assertEquals(2, result.getTemporaryBonusRounds());
        assertEquals(TargetScope.ALLIES, result.getTemporaryBonusScope());
    }

    @Test
    void damageBonusStaysNullWhenNotSet() {
        InteractionResult result = InteractionResult.builder()
                .skillRollBonus(3)
                .build();

        assertNull(result.getDamageBonus());
    }

    @Test
    void builderAssignsTheDamageBonus() {
        DamageBonus damageBonus = new DamageBonus(2, DamageType.FISICO);

        InteractionResult result = InteractionResult.builder()
                .damageBonus(damageBonus)
                .build();

        assertSame(damageBonus, result.getDamageBonus());
    }

    @Test
    void resourceLossFieldsStayNullWhenNotSet() {
        InteractionResult result = InteractionResult.builder()
                .skillRollBonus(3)
                .build();

        assertNull(result.getResourceLossValue());
        assertNull(result.getResourceLossType());
    }

    @Test
    void builderAssignsTheResourceLoss() {
        InteractionResult result = InteractionResult.builder()
                .resourceLossValue(7)
                .resourceLossType(ResourceType.HIT_POINTS)
                .build();

        assertEquals(7, result.getResourceLossValue());
        assertEquals(ResourceType.HIT_POINTS, result.getResourceLossType());
    }

    @Test
    void egoLossFieldsStayNullWhenNotSet() {
        InteractionResult result = InteractionResult.builder()
                .skillRollBonus(3)
                .build();

        assertNull(result.getEgoLossValue());
        assertNull(result.getEgoLossDomain());
    }

    @Test
    void builderAssignsTheEgoLoss() {
        InteractionResult result = InteractionResult.builder()
                .egoLossValue(2)
                .egoLossDomain(EgoDomain.SORTE)
                .build();

        assertEquals(2, result.getEgoLossValue());
        assertEquals(EgoDomain.SORTE, result.getEgoLossDomain());
    }

    @Test
    void nextInteractionStaysNullWhenNotSet() {
        InteractionResult result = InteractionResult.builder()
                .skillRollBonus(3)
                .build();

        assertNull(result.getNextInteraction());
    }

    @Test
    void builderAssignsTheNextInteraction() {
        Interaction<CharacterSheet> nextInteraction = target -> InteractionResult.builder().build();

        InteractionResult result = InteractionResult.builder()
                .nextInteraction(nextInteraction)
                .build();

        assertSame(nextInteraction, result.getNextInteraction());
    }

    @Test
    void toBuilderPreservesExistingFieldsWhileAddingNewOnes() {
        InteractionResult base = InteractionResult.builder()
                .skillRollBonus(5)
                .difficultyReduction(1)
                .build();

        InteractionResult extended = base.toBuilder()
                .temporaryBonusModifierType(ModifierType.SKILL_ROLL_BONUS)
                .temporaryBonusRounds(1)
                .build();

        assertEquals(5, extended.getSkillRollBonus());
        assertEquals(1, extended.getDifficultyReduction());
        assertEquals(ModifierType.SKILL_ROLL_BONUS, extended.getTemporaryBonusModifierType());
        assertEquals(1, extended.getTemporaryBonusRounds());
    }
}
