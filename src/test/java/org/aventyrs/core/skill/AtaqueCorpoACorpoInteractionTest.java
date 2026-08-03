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

class AtaqueCorpoACorpoInteractionTest {

    private final AtaqueCorpoACorpoInteraction ataqueCorpoACorpoInteraction = new AtaqueCorpoACorpoInteraction();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private CharacterSheet sheetWithStrengthAndSkill(int strengthBase, CharacterSkill characterSkill) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().base(strengthBase).build())
                        .build());
        if (characterSkill != null) {
            builder.skill(SkillType.ATAQUE_CORPO_A_CORPO, characterSkill);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    @Test
    void applyToReturnsAttributeTotalPlusGraduationWhenTrained() {
        CharacterSkill ataqueCorpoACorpoSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        ataqueCorpoACorpoSkill.increaseGraduation(1);
        CharacterSheet sheet = sheetWithStrengthAndSkill(2, ataqueCorpoACorpoSkill);

        InteractionResult result = ataqueCorpoACorpoInteraction.applyTo(sheet);

        assertEquals(3, result.getSkillRollBonus());
        assertEquals(CharacterStatus.CLEAN, result.getResultStatus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToAppliesTheUntrainedPenaltyWhenNeverTrained() {
        CharacterSheet sheet = sheetWithStrengthAndSkill(3, null);

        InteractionResult result = ataqueCorpoACorpoInteraction.applyTo(sheet);

        assertEquals(1, result.getSkillRollBonus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToReflectsProdigioDifficultyReductionOnceUnlocked() {
        CharacterSkill ataqueCorpoACorpoSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        ataqueCorpoACorpoSkill.increaseGraduation(7);
        CharacterSheet sheet = sheetWithStrengthAndSkill(2, ataqueCorpoACorpoSkill);

        InteractionResult result = ataqueCorpoACorpoInteraction.applyTo(sheet);

        assertEquals(1, result.getDifficultyReduction());
    }

    @Test
    void characterSheetReceiveInteractionDelegatesToApplyTo() {
        CharacterSheet sheet = sheetWithStrengthAndSkill(2, null);

        InteractionResult result = sheet.receiveInteraction(ataqueCorpoACorpoInteraction);

        assertEquals(0, result.getSkillRollBonus());
    }

    @Test
    void acuidadeSubstitutesDestrezaForForca() {
        CharacterSkill ataqueCorpoACorpoSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        ataqueCorpoACorpoSkill.increaseGraduation(1);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().base(9).build())
                        .dexterity(AttributeValue.builder().base(2).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, ataqueCorpoACorpoSkill)
                .skillCompetencyAbility(AtaqueCorpoACorpoCompetencyAbility.ACUIDADE)
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        InteractionResult result = ataqueCorpoACorpoInteraction.applyTo(sheet);

        assertEquals(3, result.getSkillRollBonus());
    }

    @Test
    void unrelatedCompetencyAbilitiesDoNotSubstituteTheAttribute() {
        CharacterSkill ataqueCorpoACorpoSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        ataqueCorpoACorpoSkill.increaseGraduation(1);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().base(2).build())
                        .dexterity(AttributeValue.builder().base(9).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, ataqueCorpoACorpoSkill)
                .skillCompetencyAbility(AtaqueCorpoACorpoCompetencyAbility.ATAQUE_PRECISO)
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        InteractionResult result = ataqueCorpoACorpoInteraction.applyTo(sheet);

        assertEquals(3, result.getSkillRollBonus());
    }
}
