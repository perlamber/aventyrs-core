package org.aventyrs.core.action;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.ability.DexterityAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionPointsServiceImplTest {

    private final ActionPointsService actionPointsService = new ActionPointsServiceImpl();

    private static class SkillCompetencyActionPointsBonusAbility implements SkillCompetencyAbility {
        @Override
        public SkillType getSkillType() {
            return SkillType.ATLETISMO;
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

    @BeforeEach
    void setup() {
        CharacterSkillFixture.loadTemplates();
    }

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

    private static class SkillRollDiscountAbility implements AttributeAbility {
        @Override
        public AttributeDomain getAttributeDomain() {
            return AttributeDomain.INSTINCT;
        }

        @Override
        public String getDescription() {
            return "Test-only -1 skill roll cost discount source.";
        }

        @Modifier(ModifierType.SKILL_ROLL_COST)
        public int discount() {
            return -1;
        }
    }

    private static class SmallActionPointsMalusAbility implements AttributeAbility {
        @Override
        public AttributeDomain getAttributeDomain() {
            return AttributeDomain.INSTINCT;
        }

        @Override
        public String getDescription() {
            return "Test-only -2 Action Points malus source.";
        }

        @Modifier(ModifierType.ACTION_POINTS)
        public int malus() {
            return -2;
        }
    }

    private static class LargeSkillRollDiscountAbility implements AttributeAbility {
        @Override
        public AttributeDomain getAttributeDomain() {
            return AttributeDomain.INSTINCT;
        }

        @Override
        public String getDescription() {
            return "Test-only -10 skill roll cost discount source.";
        }

        @Modifier(ModifierType.SKILL_ROLL_COST)
        public int discount() {
            return -10;
        }
    }

    private Character characterWithProfile(ActionProfile profile, AttributeAbility... abilities) {
        Character.CharacterBuilder builder = Character.builder()
                .player(new Player())
                .name("Test")
                .race(new Human())
                .actionProfile(profile)
                .egos(CharacterEgos.builder().build())
                .attributes(CharacterAttributes.builder().build());
        for (AttributeAbility ability : abilities) {
            builder.attributeAbility(ability);
        }
        return builder.build();
    }

    private Character characterWithFixedActionPoints(int actionPoints, ActionProfile profile) {
        return Character.builder()
                .player(new Player())
                .name("Test")
                .race(new Human())
                .actionProfile(profile)
                .actionPoints(actionPoints)
                .egos(CharacterEgos.builder().build())
                .attributes(CharacterAttributes.builder().build())
                .build();
    }

    @Test
    void defaultMaxActionPointsIsThreeOnAnyTurnForANeutralProfile() {
        Character character = characterWithProfile(ActionProfile.REFLEXOS_RAPIDOS);
        assertEquals(3, actionPointsService.getMaxActionPoints(character, 0));
        assertEquals(3, actionPointsService.getMaxActionPoints(character, 5));
    }

    @Test
    void maxActionPointsUsesTheCharactersOwnFixedCounterAsBaseline() {
        Character character = characterWithFixedActionPoints(5, ActionProfile.REFLEXOS_RAPIDOS);
        assertEquals(5, actionPointsService.getMaxActionPoints(character, 0));
    }

    @Test
    void temporaryActionPointsBonusIsAddedOnTopOfTheFixedCounter() {
        Character character = Character.builder()
                .player(new Player())
                .name("Test")
                .race(new Human())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .egos(CharacterEgos.builder().build())
                .attributes(CharacterAttributes.builder().build())
                .temporaryActionPointsBonus(2)
                .build();
        assertEquals(5, actionPointsService.getMaxActionPoints(character, 0));
    }

    @Test
    void temporaryActionPointsMalusReducesTheAvailablePA() {
        Character character = Character.builder()
                .player(new Player())
                .name("Test")
                .race(new Human())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .egos(CharacterEgos.builder().build())
                .attributes(CharacterAttributes.builder().build())
                .temporaryActionPointsBonus(-2)
                .build();
        assertEquals(1, actionPointsService.getMaxActionPoints(character, 0));
    }

    @Test
    void actionPointsModifierBonusIsAdded() {
        Character character = characterWithProfile(ActionProfile.REFLEXOS_RAPIDOS, new ActionPointsBonusAbility());
        assertEquals(4, actionPointsService.getMaxActionPoints(character, 0));
    }

    @Test
    void skillCompetencyAbilityActionPointsModifierIsAdded() {
        Character character = Character.builder()
                .player(new Player())
                .name("Test")
                .race(new Human())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .egos(CharacterEgos.builder().build())
                .attributes(CharacterAttributes.builder().build())
                .skillCompetencyAbility(new SkillCompetencyActionPointsBonusAbility())
                .build();
        assertEquals(4, actionPointsService.getMaxActionPoints(character, 0));
    }

    @Test
    void unlockedExcellencyActionPointsModifierIsAddedForATrainedSkill() {
        CharacterSkill atletismoSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATLETISMO_1).build();
        atletismoSkill.increaseGraduation(10);
        Character character = Character.builder()
                .player(new Player())
                .name("Test")
                .race(new Human())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .egos(CharacterEgos.builder().build())
                .attributes(CharacterAttributes.builder().build())
                .skill(SkillType.ATLETISMO, atletismoSkill)
                .build();
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

    @Test
    void defaultSkillRollCostIsTwoOnAnyTurnForANeutralProfile() {
        Character character = characterWithProfile(ActionProfile.REFLEXOS_RAPIDOS);
        assertEquals(2, actionPointsService.getSkillRollCost(character, 0));
        assertEquals(2, actionPointsService.getSkillRollCost(character, 5));
    }

    @Test
    void skillRollCostModifierFromAbilitiesOrFeatsIsApplied() {
        Character character = characterWithProfile(ActionProfile.REFLEXOS_RAPIDOS, new SkillRollDiscountAbility());
        assertEquals(1, actionPointsService.getSkillRollCost(character, 0));
    }

    @Test
    void skillRollCostNeverGoesBelowZero() {
        Character character = characterWithProfile(ActionProfile.REFLEXOS_RAPIDOS, new LargeSkillRollDiscountAbility());
        assertEquals(0, actionPointsService.getSkillRollCost(character, 0));
    }

    @Test
    void canAffordSkillRollIsFalseWhenCostExceedsAvailablePA() {
        Character character = characterWithProfile(ActionProfile.REFLEXOS_RAPIDOS, new SmallActionPointsMalusAbility());
        assertFalse(actionPointsService.canAffordSkillRoll(character, 0));
    }

    @Test
    void canAffordSkillRollReflectsADiscountedCost() {
        Character character = characterWithProfile(ActionProfile.REFLEXOS_RAPIDOS,
                new SmallActionPointsMalusAbility(), new SkillRollDiscountAbility());
        assertTrue(actionPointsService.canAffordSkillRoll(character, 0));
    }

    // --- Per-Round totals: the CombatantSheet overloads -----------------------------------------

    private static final SceneContext COMBAT_SCENE =
            new SceneContext(List.of(), List.of(), Map.of(), null, true, 1, false);
    private static final SceneContext NON_COMBAT_SCENE =
            new SceneContext(List.of(), List.of(), Map.of());

    private CharacterSheet sheetWithProfile(ActionProfile profile) {
        return CharacterSheet.of(characterWithProfile(profile), new Player());
    }

    @Test
    void theSheetOverloadMatchesTheCharacterOverloadWhenNothingTemporaryApplies() {
        CharacterSheet sheet = sheetWithProfile(ActionProfile.CALCULISTA);
        for (int turn = 0; turn < 5; turn++) {
            assertEquals(actionPointsService.getMaxActionPoints(sheet.getCharacter(), turn),
                    actionPointsService.getMaxActionPoints(sheet, turn));
        }
    }

    /**
     * The gap this closes: a {@code Blessing} typed to {@code ModifierType.ACTION_POINTS} used
     * to be silently inert, because only {@code Character#getTemporaryActionPointsBonus()} was
     * ever read. The Character-only overload still can't see it — it has no sheet to ask.
     */
    @Test
    void aGrantedActionPointsTemporaryBonusRaisesTheRoundsPA() {
        CharacterSheet sheet = sheetWithProfile(ActionProfile.CONSCIENCIA_DEFENSIVA);
        sheet.grantTemporaryBonus(ModifierType.ACTION_POINTS, 1, 1);
        assertEquals(4, actionPointsService.getMaxActionPoints(sheet, 0));
        assertEquals(3, actionPointsService.getMaxActionPoints(sheet.getCharacter(), 0));
    }

    @Test
    void anExpiredActionPointsTemporaryBonusNoLongerCounts() {
        CharacterSheet sheet = sheetWithProfile(ActionProfile.CONSCIENCIA_DEFENSIVA);
        sheet.grantTemporaryBonus(ModifierType.ACTION_POINTS, 1, 1);
        sheet.finishTurn();
        assertEquals(3, actionPointsService.getMaxActionPoints(sheet, 1));
    }

    @Test
    void theProfileIsAppliedAfterAGrantedTemporaryBonusNotBeforeIt() {
        CharacterSheet sheet = sheetWithProfile(ActionProfile.CALCULISTA);
        sheet.grantTemporaryBonus(ModifierType.ACTION_POINTS, 5, 3);
        assertEquals(0, actionPointsService.getMaxActionPoints(sheet, 0));
        assertEquals(8, actionPointsService.getMaxActionPoints(sheet, 1));
    }

    @Test
    void estrategistaRemovesOnePAOnlyInsideACenaDeCombate() {
        CharacterSheet sheet = sheetWithProfile(ActionProfile.ESTRATEGISTA);
        assertEquals(2, actionPointsService.getMaxActionPoints(sheet, 0, COMBAT_SCENE));
        assertEquals(3, actionPointsService.getMaxActionPoints(sheet, 0, NON_COMBAT_SCENE));
        assertEquals(3, actionPointsService.getMaxActionPoints(sheet, 0));
    }

    @Test
    void estrategistaRemovesOnePAOnEveryCombatRoundNotJustTheFirst() {
        CharacterSheet sheet = sheetWithProfile(ActionProfile.ESTRATEGISTA);
        for (int turn = 0; turn < 5; turn++) {
            assertEquals(2, actionPointsService.getMaxActionPoints(sheet, turn, COMBAT_SCENE));
        }
    }

    @Test
    void theSheetOverloadNeverGoesBelowZero() {
        CharacterSheet sheet = CharacterSheet.of(
                characterWithProfile(ActionProfile.IMPULSIVO, new ActionPointsMalusAbility()), new Player());
        assertEquals(0, actionPointsService.getMaxActionPoints(sheet, 2));
    }

    @Test
    void canAffordSkillRollReflectsAGrantedTemporaryBonus() {
        CharacterSheet sheet = CharacterSheet.of(
                characterWithProfile(ActionProfile.CONSCIENCIA_DEFENSIVA, new SmallActionPointsMalusAbility()),
                new Player());
        assertFalse(actionPointsService.canAffordSkillRoll(sheet, 0));
        sheet.grantTemporaryBonus(ModifierType.ACTION_POINTS, 1, 1);
        assertTrue(actionPointsService.canAffordSkillRoll(sheet, 0));
    }

    /**
     * APRESSADO is the first ability whose PA grant is conditioned on the Turn, so it can't be
     * a no-arg {@code @Modifier} — it rides {@code AttributeAbility#resolveActionPointsBonus}
     * instead, summed into the same baseline both overloads share.
     */
    @Test
    void apressadoAddsOnePAOnEvenRodadasThroughBothOverloads() {
        Character character = characterWithProfile(ActionProfile.CONSCIENCIA_DEFENSIVA, DexterityAbility.APRESSADO);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        assertEquals(3, actionPointsService.getMaxActionPoints(character, 0));
        assertEquals(4, actionPointsService.getMaxActionPoints(character, 1));
        assertEquals(3, actionPointsService.getMaxActionPoints(sheet, 2));
        assertEquals(4, actionPointsService.getMaxActionPoints(sheet, 3, COMBAT_SCENE));
    }

    @Test
    void calculistaStillZeroesTheFirstRodadaDespiteApressado() {
        // The profile is applied last, so its hard 0 isn't out-summed — and the 2nd Rodada
        // (turn 1) stacks Calculista's own recovery with Apressado's even-Rodada point.
        Character character = characterWithProfile(ActionProfile.CALCULISTA, DexterityAbility.APRESSADO);

        assertEquals(0, actionPointsService.getMaxActionPoints(character, 0));
        assertEquals(4, actionPointsService.getMaxActionPoints(character, 1));
    }

    @Test
    void canAffordSkillRollReflectsEstrategistasCombatMalus() {
        CharacterSheet sheet = CharacterSheet.of(
                characterWithFixedActionPoints(2, ActionProfile.ESTRATEGISTA), new Player());
        assertTrue(actionPointsService.canAffordSkillRoll(sheet, 0, NON_COMBAT_SCENE));
        assertFalse(actionPointsService.canAffordSkillRoll(sheet, 0, COMBAT_SCENE));
    }
}
