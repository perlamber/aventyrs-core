package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.TargetScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ArtesInteractionTest {

    private final ArtesInteraction artesInteraction = new ArtesInteraction();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private CharacterSheet sheetWithCharismaAndSkill(int charismaBase, CharacterSkill characterSkill) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .charisma(AttributeValue.builder().base(charismaBase).build())
                        .build());
        if (characterSkill != null) {
            builder.skill(SkillType.ARTES, characterSkill);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    private CharacterSheet sheetWithDomBardicoAndArtesGraduation(final int graduationValue) {
        CharacterSkill artesSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ARTES_1).build();
        artesSkill.increaseGraduation(graduationValue);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.ARTES, artesSkill)
                .skillCompetencyAbility(ArtesCompetencyAbility.DOM_BARDICO)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void applyToReturnsAttributeTotalPlusGraduationWhenTrained() {
        CharacterSkill artesSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ARTES_1).build();
        artesSkill.increaseGraduation(1);
        CharacterSheet sheet = sheetWithCharismaAndSkill(2, artesSkill);

        InteractionResult result = artesInteraction.applyTo(sheet);

        assertEquals(3, result.getSkillRollBonus());
        assertEquals(CharacterStatus.CLEAN, result.getResultStatus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToAppliesTheUntrainedPenaltyWhenNeverTrained() {
        CharacterSheet sheet = sheetWithCharismaAndSkill(3, null);

        InteractionResult result = artesInteraction.applyTo(sheet);

        assertEquals(1, result.getSkillRollBonus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToReflectsProdigioDifficultyReductionOnceUnlocked() {
        CharacterSkill artesSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ARTES_1).build();
        artesSkill.increaseGraduation(7);
        CharacterSheet sheet = sheetWithCharismaAndSkill(2, artesSkill);

        InteractionResult result = artesInteraction.applyTo(sheet);

        assertEquals(1, result.getDifficultyReduction());
    }

    @Test
    void characterSheetReceiveInteractionDelegatesToApplyTo() {
        CharacterSheet sheet = sheetWithCharismaAndSkill(2, null);

        InteractionResult result = sheet.receiveInteraction(artesInteraction);

        assertEquals(0, result.getSkillRollBonus());
    }

    @Test
    void applyToIncludesAnActiveTemporarySkillRollBonus() {
        CharacterSkill artesSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ARTES_1).build();
        artesSkill.increaseGraduation(1);
        CharacterSheet sheet = sheetWithCharismaAndSkill(2, artesSkill);

        // e.g. an ally motivated by another character's DOM_BARDICO.
        sheet.grantTemporaryBonus(ModifierType.SKILL_ROLL_BONUS, 3, 1);

        InteractionResult result = artesInteraction.applyTo(sheet);

        assertEquals(6, result.getSkillRollBonus());
    }

    @Test
    void applyToIgnoresAnExpiredTemporarySkillRollBonus() {
        CharacterSkill artesSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ARTES_1).build();
        artesSkill.increaseGraduation(1);
        CharacterSheet sheet = sheetWithCharismaAndSkill(2, artesSkill);
        sheet.grantTemporaryBonus(ModifierType.SKILL_ROLL_BONUS, 3, 1);

        sheet.tickTemporaryBonuses();
        InteractionResult result = artesInteraction.applyTo(sheet);

        assertEquals(3, result.getSkillRollBonus());
    }

    @Test
    void applyToIncludesATemporaryBonusScopedSpecificallyToArtes() {
        CharacterSkill artesSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ARTES_1).build();
        artesSkill.increaseGraduation(1);
        CharacterSheet sheet = sheetWithCharismaAndSkill(2, artesSkill);

        sheet.grantTemporaryBonus(ModifierType.ARTES_ROLL_BONUS, 4, 1);

        InteractionResult result = artesInteraction.applyTo(sheet);

        assertEquals(7, result.getSkillRollBonus());
    }

    @Test
    void applyToDoesNotLeakATemporaryBonusScopedToADifferentSkill() {
        CharacterSkill artesSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ARTES_1).build();
        artesSkill.increaseGraduation(1);
        CharacterSheet sheet = sheetWithCharismaAndSkill(2, artesSkill);

        // A bonus granted for Atletismo specifically must not affect an Artes roll.
        sheet.grantTemporaryBonus(ModifierType.ATLETISMO_ROLL_BONUS, 10, 1);

        InteractionResult result = artesInteraction.applyTo(sheet);

        assertEquals(3, result.getSkillRollBonus());
    }

    @Test
    void applyToCombinesTheGenericAndArtesSpecificTemporaryBonusesAdditively() {
        CharacterSkill artesSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ARTES_1).build();
        artesSkill.increaseGraduation(1);
        CharacterSheet sheet = sheetWithCharismaAndSkill(2, artesSkill);

        sheet.grantTemporaryBonus(ModifierType.SKILL_ROLL_BONUS, 3, 1);
        sheet.grantTemporaryBonus(ModifierType.ARTES_ROLL_BONUS, 4, 1);
        sheet.grantTemporaryBonus(ModifierType.ATLETISMO_ROLL_BONUS, 10, 1);

        InteractionResult result = artesInteraction.applyTo(sheet);

        assertEquals(10, result.getSkillRollBonus());
    }

    @Test
    void applyToLeavesTemporaryBonusFieldsNullWithoutDomBardico() {
        CharacterSkill artesSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ARTES_1).build();
        CharacterSheet sheet = sheetWithCharismaAndSkill(2, artesSkill);

        InteractionResult result = artesInteraction.applyTo(sheet);

        assertNull(result.getTemporaryBonusModifierType());
        assertNull(result.getTemporaryBonusRounds());
        assertNull(result.getTemporaryBonusValue());
    }

    @Test
    void applyToSetsOneRodadaForDomBardicoBelowFiveGraduacoes() {
        CharacterSheet sheet = sheetWithDomBardicoAndArtesGraduation(4);

        InteractionResult result = artesInteraction.applyTo(sheet);

        assertEquals(ModifierType.SKILL_ROLL_BONUS, result.getTemporaryBonusModifierType());
        assertEquals(1, result.getTemporaryBonusRounds());
        assertEquals(TargetScope.ALLIES, result.getTemporaryBonusScope());
        assertNull(result.getTemporaryBonusValue());
    }

    @Test
    void applyToSetsTwoRodadasForDomBardicoAtFiveGraduacoes() {
        CharacterSheet sheet = sheetWithDomBardicoAndArtesGraduation(5);

        InteractionResult result = artesInteraction.applyTo(sheet);

        assertEquals(2, result.getTemporaryBonusRounds());
    }

    @Test
    void applyToSetsThreeRodadasForDomBardicoAtTenGraduacoes() {
        CharacterSheet sheet = sheetWithDomBardicoAndArtesGraduation(10);

        InteractionResult result = artesInteraction.applyTo(sheet);

        assertEquals(3, result.getTemporaryBonusRounds());
    }

    @Test
    void applyToWithASceneContextStillSetsDomBardicoFields() {
        CharacterSheet sheet = sheetWithDomBardicoAndArtesGraduation(5);
        Scene scene = new Scene();
        UUID party = UUID.randomUUID();
        scene.addParticipant(sheet, 10, party);
        SceneContext sceneContext = scene.buildContext(sheet, Map.of());

        InteractionResult result = artesInteraction.applyTo(sheet, sceneContext);

        assertEquals(ModifierType.SKILL_ROLL_BONUS, result.getTemporaryBonusModifierType());
        assertEquals(TargetScope.ALLIES, result.getTemporaryBonusScope());
        assertEquals(2, result.getTemporaryBonusRounds());
    }

    @Test
    void oneArgApplyToDelegatesThroughToTheOverriddenTwoArgVersion() {
        // Proves AbstractSkillInteraction's applyTo(target) -> applyTo(target, null) delegation
        // still dispatches virtually to ArtesInteraction's own override, not the base class's.
        CharacterSheet sheet = sheetWithDomBardicoAndArtesGraduation(10);

        InteractionResult viaOneArg = artesInteraction.applyTo(sheet);
        InteractionResult viaTwoArgWithNullContext = artesInteraction.applyTo(sheet, null);

        assertEquals(viaTwoArgWithNullContext.getTemporaryBonusRounds(), viaOneArg.getTemporaryBonusRounds());
        assertEquals(TargetScope.ALLIES, viaOneArg.getTemporaryBonusScope());
    }

    @Test
    void applyToWithASkillRollBelowMedioLeavesTemporaryBonusValueNull() {
        // graduationValue=0 -> bonus 1 (1 carisma + 0 graduação); dice [2,3,4]=9 -> total 10,
        // short of even VERY_EASY's 12 -> reachedDifficultyLevel is null -> no bonus granted.
        CharacterSheet sheet = sheetWithDomBardicoAndArtesGraduation(0);
        SkillRoll skillRoll = new SkillRoll(List.of(2, 3, 4));

        InteractionResult result = artesInteraction.applyTo(sheet, null, skillRoll);

        assertNull(result.getTemporaryBonusValue());
        // The roll-resolution fields themselves are still set independently.
        assertEquals(TargetScope.ALLIES, result.getTemporaryBonusScope());
    }

    @Test
    void applyToGrantsPlusOneAtGdMedio() {
        // bonus 1 (1 carisma + 0 graduação); dice [6,6,6]=18 -> total 19, reaches MEDIUM
        // (18) but not HARD (23).
        CharacterSheet sheet = sheetWithDomBardicoAndArtesGraduation(0);
        SkillRoll skillRoll = new SkillRoll(List.of(6, 6, 6));

        InteractionResult result = artesInteraction.applyTo(sheet, null, skillRoll);

        assertEquals(DifficultyLevel.MEDIUM, result.getReachedDifficultyLevel());
        assertEquals(1, result.getTemporaryBonusValue());
    }

    @Test
    void applyToGrantsPlusTwoAtGdDificil() {
        // bonus 5 (1 carisma + 4 graduação); dice [6,6,6]=18 -> total 23, reaches HARD exactly.
        CharacterSheet sheet = sheetWithDomBardicoAndArtesGraduation(4);
        SkillRoll skillRoll = new SkillRoll(List.of(6, 6, 6));

        InteractionResult result = artesInteraction.applyTo(sheet, null, skillRoll);

        assertEquals(DifficultyLevel.HARD, result.getReachedDifficultyLevel());
        assertEquals(2, result.getTemporaryBonusValue());
    }

    @Test
    void applyToGrantsPlusThreeAtGdMuitoDificil() {
        // bonus 10 (1 carisma + 9 graduação); dice [6,6,6]=18 -> total 28, reaches VERY_HARD exactly.
        CharacterSheet sheet = sheetWithDomBardicoAndArtesGraduation(9);
        SkillRoll skillRoll = new SkillRoll(List.of(6, 6, 6));

        InteractionResult result = artesInteraction.applyTo(sheet, null, skillRoll);

        assertEquals(DifficultyLevel.VERY_HARD, result.getReachedDifficultyLevel());
        assertEquals(3, result.getTemporaryBonusValue());
    }

    @Test
    void applyToGrantsPlusFourAtGdImprovavel() {
        // bonus 18 (1 carisma + 17 graduação); dice [6,6,6]=18 -> total 36, reaches UNLIKELY exactly.
        CharacterSheet sheet = sheetWithDomBardicoAndArtesGraduation(17);
        SkillRoll skillRoll = new SkillRoll(List.of(6, 6, 6));

        InteractionResult result = artesInteraction.applyTo(sheet, null, skillRoll);

        assertEquals(DifficultyLevel.UNLIKELY, result.getReachedDifficultyLevel());
        assertEquals(4, result.getTemporaryBonusValue());
    }

    @Test
    void applyToGrantsPlusFourAtGdUnimaginableByInferenceFromImprovavel() {
        // UNIMAGINABLE isn't named in DOM_BARDICO's own rules text (it falls between
        // Improvável and Milagre); this inherits Improvável's +4 until Milagre is actually
        // reached — see the ability's own comment for why this is an inference, not text.
        // bonus 27 (1 carisma + 26 graduação); dice [6,6,6]=18 -> total 45, reaches UNIMAGINABLE exactly.
        CharacterSheet sheet = sheetWithDomBardicoAndArtesGraduation(26);
        SkillRoll skillRoll = new SkillRoll(List.of(6, 6, 6));

        InteractionResult result = artesInteraction.applyTo(sheet, null, skillRoll);

        assertEquals(DifficultyLevel.UNIMAGINABLE, result.getReachedDifficultyLevel());
        assertEquals(4, result.getTemporaryBonusValue());
    }

    @Test
    void applyToGrantsPlusFiveAtGdMilagre() {
        // bonus 42 (1 carisma + 41 graduação); dice [6,6,6]=18 -> total 60, reaches MIRACLE exactly.
        CharacterSheet sheet = sheetWithDomBardicoAndArtesGraduation(41);
        SkillRoll skillRoll = new SkillRoll(List.of(6, 6, 6));

        InteractionResult result = artesInteraction.applyTo(sheet, null, skillRoll);

        assertEquals(DifficultyLevel.MIRACLE, result.getReachedDifficultyLevel());
        assertEquals(5, result.getTemporaryBonusValue());
    }

    @Test
    void applyToWithASkillRollStillResolvesReachedDifficultyLevelAndCriticalResult() {
        CharacterSkill artesSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ARTES_1).build();
        artesSkill.increaseGraduation(1);
        CharacterSheet sheet = sheetWithCharismaAndSkill(2, artesSkill);
        SkillRoll skillRoll = new SkillRoll(List.of(6, 6, 6));

        InteractionResult result = artesInteraction.applyTo(sheet, null, skillRoll);

        // skillRollBonus 3 (2 charisma + 1 graduação) + dice total 18 = 21 -> reaches MEDIUM (18), not HARD (23).
        assertEquals(DifficultyLevel.MEDIUM, result.getReachedDifficultyLevel());
        assertEquals(CriticalResult.ACERTO_CRITICO_MAIOR, result.getCriticalResult());
    }
}
