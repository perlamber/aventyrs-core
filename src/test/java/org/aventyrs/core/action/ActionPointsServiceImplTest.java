package org.aventyrs.core.action;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.Human;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionPointsServiceImplTest {

    private final ActionPointsService actionPointsService = new ActionPointsServiceImpl();

    private static class ActionPointsBonusAbility implements AttributeAbility {
        @Override
        public AttributeDomain getAttributeDomain() {
            return AttributeDomain.INSTINCT;
        }

        @Override
        public String getDescription() {
            return "Test-only +1 Action Points bonus source.";
        }

        @Modifier(ModifierType.ACTION_POINTS)
        public int bonus() {
            return 1;
        }
    }

    private static class ActionPointsMalusAbility implements AttributeAbility {
        @Override
        public AttributeDomain getAttributeDomain() {
            return AttributeDomain.INSTINCT;
        }

        @Override
        public String getDescription() {
            return "Test-only -10 Action Points malus source.";
        }

        @Modifier(ModifierType.ACTION_POINTS)
        public int malus() {
            return -10;
        }
    }

    private Character characterWithProfile(ActionProfile profile, AttributeAbility... abilities) {
        Character.CharacterBuilder builder = Character.builder()
                .player(new Player())
                .name("Test")
                .race(new Human())
                .actionProfile(profile)
                .attributes(CharacterAttributes.builder().build());
        for (AttributeAbility ability : abilities) {
            builder.attributeAbility(ability);
        }
        return builder.build();
    }

    @Test
    void defaultMaxActionPointsIsThreeOnAnyTurnForANeutralProfile() {
        Character character = characterWithProfile(ActionProfile.REFLEXOS_RAPIDOS);
        assertEquals(3, actionPointsService.getMaxActionPoints(character, 0));
        assertEquals(3, actionPointsService.getMaxActionPoints(character, 5));
    }

    @Test
    void actionPointsModifierBonusIsAdded() {
        Character character = characterWithProfile(ActionProfile.REFLEXOS_RAPIDOS, new ActionPointsBonusAbility());
        assertEquals(4, actionPointsService.getMaxActionPoints(character, 0));
    }

    @Test
    void impulsivoAddsTwoOnTheFirstTurnAndSubtractsOneOnTheThirdAndFourthTurns() {
        Character character = characterWithProfile(ActionProfile.IMPULSIVO);
        assertEquals(5, actionPointsService.getMaxActionPoints(character, 0));
        assertEquals(3, actionPointsService.getMaxActionPoints(character, 1));
        assertEquals(2, actionPointsService.getMaxActionPoints(character, 2));
        assertEquals(2, actionPointsService.getMaxActionPoints(character, 3));
        assertEquals(3, actionPointsService.getMaxActionPoints(character, 4));
    }

    @Test
    void calculistaHasZeroOnTheFirstTurnThenNormalThenPlusOneFromTheThirdTurnOnward() {
        Character character = characterWithProfile(ActionProfile.CALCULISTA);
        assertEquals(0, actionPointsService.getMaxActionPoints(character, 0));
        assertEquals(3, actionPointsService.getMaxActionPoints(character, 1));
        assertEquals(4, actionPointsService.getMaxActionPoints(character, 2));
        assertEquals(4, actionPointsService.getMaxActionPoints(character, 10));
    }

    @Test
    void maxActionPointsNeverGoesBelowZero() {
        Character character = characterWithProfile(ActionProfile.IMPULSIVO, new ActionPointsMalusAbility());
        assertEquals(0, actionPointsService.getMaxActionPoints(character, 2));
    }

    @Test
    void canAffordSkillRollWhenAtLeastTwoActionPointsAreAvailable() {
        Character character = characterWithProfile(ActionProfile.CALCULISTA);
        assertFalse(actionPointsService.canAffordSkillRoll(character, 0));
        assertTrue(actionPointsService.canAffordSkillRoll(character, 1));
    }
}
