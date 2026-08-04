package org.aventyrs.core.sheet;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.modifier.ModifierType;
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
    }

    @Test
    void builderAssignsATemporaryBonusScopedToASpecificSkill() {
        InteractionResult result = InteractionResult.builder()
                .temporaryBonusValue(2)
                .temporaryBonusModifierType(ModifierType.ATLETISMO_ROLL_BONUS)
                .temporaryBonusRounds(1)
                .build();

        assertEquals(2, result.getTemporaryBonusValue());
        assertEquals(ModifierType.ATLETISMO_ROLL_BONUS, result.getTemporaryBonusModifierType());
        assertEquals(1, result.getTemporaryBonusRounds());
    }

    @Test
    void builderAssignsATemporaryBonusThatAppliesBroadly() {
        InteractionResult result = InteractionResult.builder()
                .temporaryBonusValue(1)
                .temporaryBonusModifierType(ModifierType.SKILL_ROLL_BONUS)
                .temporaryBonusRounds(2)
                .build();

        assertEquals(1, result.getTemporaryBonusValue());
        assertEquals(ModifierType.SKILL_ROLL_BONUS, result.getTemporaryBonusModifierType());
        assertEquals(2, result.getTemporaryBonusRounds());
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
