package org.aventyrs.core.skill.attention;

import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.race.Elfos;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    @Test
    void applyToWithASceneContextProducesTheSameResultAsWithout() {
        // AbstractSkillInteraction's SceneContext-accepting overload isn't consumed by any
        // ability yet, so passing one (or null) must be a no-op for a skill with no override.
        CharacterSkill attentionSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATTENTION_1).build();
        attentionSkill.increaseGraduation(1);
        CharacterSheet sheet = sheetWithInstinctAndSkill(2, attentionSkill);

        InteractionResult withoutContext = attentionInteraction.applyTo(sheet);
        InteractionResult withNullContext = attentionInteraction.applyTo(sheet, null);

        assertEquals(withoutContext.getSkillRollBonus(), withNullContext.getSkillRollBonus());
        assertEquals(withoutContext.getDifficultyReduction(), withNullContext.getDifficultyReduction());
    }

    @Test
    void applyToWithoutASkillRollLeavesRollResolutionFieldsNull() {
        CharacterSheet sheet = sheetWithInstinctAndSkill(2, null);

        InteractionResult result = attentionInteraction.applyTo(sheet, null, null);

        assertNull(result.getReachedDifficultyLevel());
        assertNull(result.getCriticalResult());
    }

    @Test
    void applyToWithASkillRollResolvesTheReachedDifficultyLevel() {
        CharacterSkill attentionSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATTENTION_1).build();
        attentionSkill.increaseGraduation(1);
        CharacterSheet sheet = sheetWithInstinctAndSkill(2, attentionSkill);
        // skillRollBonus is 3 (2 instinct + 1 graduação); dice total 10 -> grand total 13.
        SkillRoll skillRoll = new SkillRoll(List.of(2, 3, 5));

        InteractionResult result = attentionInteraction.applyTo(sheet, null, skillRoll);

        // 13 clears EASY's 14? No: 13 < 14, so only VERY_EASY's 12 is cleared.
        assertEquals(DifficultyLevel.VERY_EASY, result.getReachedDifficultyLevel());
    }

    @Test
    void applyToWithASkillRollResolvesTheCriticalResult() {
        CharacterSheet sheet = sheetWithInstinctAndSkill(2, null);
        SkillRoll skillRoll = new SkillRoll(List.of(6, 6, 6));

        InteractionResult result = attentionInteraction.applyTo(sheet, null, skillRoll);

        assertEquals(CriticalResult.ACERTO_CRITICO_MAIOR, result.getCriticalResult());
    }

    @Test
    void applyToIncludesTheElfosSentidosAbsolutosRacialBonus() {
        CharacterSkill attentionSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATTENTION_1).build();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .instinct(AttributeValue.builder().base(2).build())
                        .build())
                .skill(SkillType.ATTENTION, attentionSkill)
                .race(new Elfos())
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        InteractionResult result = attentionInteraction.applyTo(sheet);

        // 2 instinct + 0 graduação (untrained-of-extra), plus Elfos' racial Vantagem.
        assertEquals(2 + Skill.ADVANTAGE_BONUS, result.getSkillRollBonus());
    }

    @Test
    void applyToOmitsTheRacialBonusForANonElfoCharacter() {
        CharacterSheet sheet = sheetWithInstinctAndSkill(2, null);

        InteractionResult result = attentionInteraction.applyTo(sheet);

        assertEquals(0, result.getSkillRollBonus());
    }
}
