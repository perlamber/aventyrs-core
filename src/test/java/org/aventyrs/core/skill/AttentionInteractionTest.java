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

class AttentionInteractionTest {

    private final AttentionInteraction attentionInteraction = new AttentionInteraction();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private CharacterSheet sheetWithInstinctAndSkill(int instinctBase, CharacterSkill characterSkill) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .instinct(AttributeValue.builder().base(instinctBase).build())
                        .build());
        if (characterSkill != null) {
            builder.skill(SkillType.ATTENTION, characterSkill);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    @Test
    void applyToReturnsAttributeTotalPlusGraduationWhenTrained() {
        CharacterSkill attentionSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATTENTION_1).build();
        attentionSkill.increaseGraduation(1);
        CharacterSheet sheet = sheetWithInstinctAndSkill(2, attentionSkill);

        InteractionResult result = attentionInteraction.applyTo(sheet);

        assertEquals(3, result.getSkillRollBonus());
        assertEquals(CharacterStatus.CLEAN, result.getResultStatus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToAppliesTheUntrainedPenaltyWhenNeverTrained() {
        CharacterSheet sheet = sheetWithInstinctAndSkill(3, null);

        InteractionResult result = attentionInteraction.applyTo(sheet);

        assertEquals(1, result.getSkillRollBonus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToReflectsProdigioDifficultyReductionOnceUnlocked() {
        CharacterSkill attentionSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATTENTION_1).build();
        attentionSkill.increaseGraduation(7);
        CharacterSheet sheet = sheetWithInstinctAndSkill(2, attentionSkill);

        InteractionResult result = attentionInteraction.applyTo(sheet);

        assertEquals(1, result.getDifficultyReduction());
    }

    @Test
    void characterSheetReceiveInteractionDelegatesToApplyTo() {
        CharacterSheet sheet = sheetWithInstinctAndSkill(2, null);

        InteractionResult result = sheet.receiveInteraction(attentionInteraction);

        assertEquals(0, result.getSkillRollBonus());
    }

}
