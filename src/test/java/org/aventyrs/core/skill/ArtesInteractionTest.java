package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
