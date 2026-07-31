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

class AtletismoInteractionTest {

    private final AtletismoInteraction atletismoInteraction = new AtletismoInteraction();

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
            builder.skill(SkillType.ATLETISMO, characterSkill);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    @Test
    void applyToReturnsAttributeTotalPlusGraduationWhenTrained() {
        CharacterSkill atletismoSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATLETISMO_1).build();
        atletismoSkill.increaseGraduation(1);
        CharacterSheet sheet = sheetWithStrengthAndSkill(2, atletismoSkill);

        InteractionResult result = atletismoInteraction.applyTo(sheet);

        assertEquals(3, result.getSkillRollBonus());
        assertEquals(CharacterStatus.CLEAN, result.getResultStatus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToAppliesTheUntrainedPenaltyWhenNeverTrained() {
        CharacterSheet sheet = sheetWithStrengthAndSkill(3, null);

        InteractionResult result = atletismoInteraction.applyTo(sheet);

        assertEquals(1, result.getSkillRollBonus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToReflectsProdigioDifficultyReductionOnceUnlocked() {
        CharacterSkill atletismoSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATLETISMO_1).build();
        atletismoSkill.increaseGraduation(7);
        CharacterSheet sheet = sheetWithStrengthAndSkill(2, atletismoSkill);

        InteractionResult result = atletismoInteraction.applyTo(sheet);

        assertEquals(1, result.getDifficultyReduction());
    }

    @Test
    void applyToAddsDifficultyReductionFromAtletaVersatil() {
        CharacterSheet sheet = sheetWithStrengthAndSkill(2, null);
        Character character = sheet.getCharacter().toBuilder()
                .skillCompetencyAbility(AtletismoCompetencyAbility.ATLETA_VERSATIL)
                .build();
        CharacterSheet sheetWithAbility = CharacterSheet.of(character, new Player());

        InteractionResult result = atletismoInteraction.applyTo(sheetWithAbility);

        assertEquals(1, result.getDifficultyReduction());
    }

    @Test
    void characterSheetReceiveInteractionDelegatesToApplyTo() {
        CharacterSheet sheet = sheetWithStrengthAndSkill(2, null);

        InteractionResult result = sheet.receiveInteraction(atletismoInteraction);

        assertEquals(0, result.getSkillRollBonus());
    }
}
