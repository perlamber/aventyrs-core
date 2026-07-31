package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmpatiaSelvagemInteractionTest {

    private final EmpatiaSelvagemInteraction empatiaSelvagemInteraction = new EmpatiaSelvagemInteraction();

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
            builder.skill(SkillType.EMPATIA_SELVAGEM, characterSkill);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    @Test
    void applyToReturnsAttributeTotalPlusGraduationWhenTrained() {
        CharacterSkill empatiaSelvagemSkill = CharacterSkillFixture.blank(CharacterSkillFixture.EMPATIA_SELVAGEM_1).build();
        empatiaSelvagemSkill.increaseGraduation(1);
        CharacterSheet sheet = sheetWithCharismaAndSkill(2, empatiaSelvagemSkill);

        InteractionResult result = empatiaSelvagemInteraction.applyTo(sheet);

        assertEquals(3, result.getSkillRollBonus());
        assertEquals(CharacterStatus.CLEAN, result.getResultStatus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToAppliesTheUntrainedPenaltyWhenNeverTrained() {
        CharacterSheet sheet = sheetWithCharismaAndSkill(3, null);

        InteractionResult result = empatiaSelvagemInteraction.applyTo(sheet);

        assertEquals(1, result.getSkillRollBonus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToReflectsProdigioDifficultyReductionOnceUnlocked() {
        CharacterSkill empatiaSelvagemSkill = CharacterSkillFixture.blank(CharacterSkillFixture.EMPATIA_SELVAGEM_1).build();
        empatiaSelvagemSkill.increaseGraduation(7);
        CharacterSheet sheet = sheetWithCharismaAndSkill(2, empatiaSelvagemSkill);

        InteractionResult result = empatiaSelvagemInteraction.applyTo(sheet);

        assertEquals(1, result.getDifficultyReduction());
    }

    @Test
    void applyToAddsTheAdvantageBonusOnceFocadoIsUnlocked() {
        CharacterSkill empatiaSelvagemSkill = CharacterSkillFixture.blank(CharacterSkillFixture.EMPATIA_SELVAGEM_1).build();
        empatiaSelvagemSkill.increaseGraduation(3);
        CharacterSheet sheet = sheetWithCharismaAndSkill(2, empatiaSelvagemSkill);

        InteractionResult result = empatiaSelvagemInteraction.applyTo(sheet);

        assertEquals(2 + 3 + Skill.ADVANTAGE_BONUS, result.getSkillRollBonus());
    }

    @Test
    void characterSheetReceiveInteractionDelegatesToApplyTo() {
        CharacterSheet sheet = sheetWithCharismaAndSkill(2, null);

        InteractionResult result = sheet.receiveInteraction(empatiaSelvagemInteraction);

        assertEquals(0, result.getSkillRollBonus());
    }

    @Test
    void applyToAddsTheAdvantageBonusWhenCharacterHasAmainarASelvageria() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .charisma(AttributeValue.builder().base(2).build())
                        .build())
                .skillCompetencyAbility(EmpatiaSelvagemCompetencyAbility.AMAINAR_A_SELVAGERIA)
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        InteractionResult result = empatiaSelvagemInteraction.applyTo(sheet);

        assertEquals(2, result.getSkillRollBonus());
    }
}
