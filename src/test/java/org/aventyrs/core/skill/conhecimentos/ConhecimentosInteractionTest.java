package org.aventyrs.core.skill.conhecimentos;

import org.aventyrs.core.character.AttributeDomain;
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
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConhecimentosInteractionTest {

    private final ConhecimentosInteraction conhecimentosInteraction = new ConhecimentosInteraction();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private CharacterSheet sheetWithGnoseAndSkill(int gnoseBase, CharacterSkill characterSkill) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .gnose(AttributeValue.builder().domain(AttributeDomain.GNOSE).base(gnoseBase).build())
                        .build());
        if (characterSkill != null) {
            builder.skill(SkillType.CONHECIMENTOS, characterSkill);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    @Test
    void applyToReturnsAttributeTotalPlusGraduationWhenTrained() {
        CharacterSkill conhecimentosSkill = CharacterSkillFixture.blank(CharacterSkillFixture.CONHECIMENTOS_1).build();
        conhecimentosSkill.increaseGraduation(1);
        CharacterSheet sheet = sheetWithGnoseAndSkill(2, conhecimentosSkill);

        InteractionResult result = conhecimentosInteraction.applyTo(sheet);

        assertEquals(3, result.getSkillRollBonus());
        assertEquals(CharacterStatus.CLEAN, result.getResultStatus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToAppliesTheUntrainedPenaltyWhenNeverTrained() {
        CharacterSheet sheet = sheetWithGnoseAndSkill(3, null);

        InteractionResult result = conhecimentosInteraction.applyTo(sheet);

        assertEquals(1, result.getSkillRollBonus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToReflectsProdigioDifficultyReductionOnceUnlocked() {
        CharacterSkill conhecimentosSkill = CharacterSkillFixture.blank(CharacterSkillFixture.CONHECIMENTOS_1).build();
        conhecimentosSkill.increaseGraduation(7);
        CharacterSheet sheet = sheetWithGnoseAndSkill(2, conhecimentosSkill);

        InteractionResult result = conhecimentosInteraction.applyTo(sheet);

        assertEquals(1, result.getDifficultyReduction());
    }

    @Test
    void characterSheetReceiveInteractionDelegatesToApplyTo() {
        CharacterSheet sheet = sheetWithGnoseAndSkill(2, null);

        InteractionResult result = sheet.receiveInteraction(conhecimentosInteraction);

        assertEquals(0, result.getSkillRollBonus());
    }

    @Test
    void applyToAddsTheAdvantageBonusWhenCharacterHasOProfessor() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .gnose(AttributeValue.builder().domain(AttributeDomain.GNOSE).base(2).build())
                        .build())
                .skillCompetencyAbility(ConhecimentosCompetencyAbility.O_PROFESSOR)
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        InteractionResult result = conhecimentosInteraction.applyTo(sheet);

        assertEquals(2, result.getSkillRollBonus());
    }
}
