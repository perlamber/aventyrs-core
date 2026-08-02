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

class DirigirECavalgarInteractionTest {

    private final DirigirECavalgarInteraction dirigirECavalgarInteraction = new DirigirECavalgarInteraction();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private CharacterSheet sheetWithDexterityAndSkill(int dexterityBase, CharacterSkill characterSkill) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().base(dexterityBase).build())
                        .build());
        if (characterSkill != null) {
            builder.skill(SkillType.DIRIGIR_E_CAVALGAR, characterSkill);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    @Test
    void applyToReturnsAttributeTotalPlusGraduationWhenTrained() {
        CharacterSkill dirigirECavalgarSkill = CharacterSkillFixture.blank(CharacterSkillFixture.DIRIGIR_E_CAVALGAR_1).build();
        dirigirECavalgarSkill.increaseGraduation(1);
        CharacterSheet sheet = sheetWithDexterityAndSkill(2, dirigirECavalgarSkill);

        InteractionResult result = dirigirECavalgarInteraction.applyTo(sheet);

        assertEquals(3, result.getSkillRollBonus());
        assertEquals(CharacterStatus.CLEAN, result.getResultStatus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToAppliesTheUntrainedPenaltyWhenNeverTrained() {
        CharacterSheet sheet = sheetWithDexterityAndSkill(3, null);

        InteractionResult result = dirigirECavalgarInteraction.applyTo(sheet);

        assertEquals(1, result.getSkillRollBonus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToReflectsProdigioDifficultyReductionOnceUnlocked() {
        CharacterSkill dirigirECavalgarSkill = CharacterSkillFixture.blank(CharacterSkillFixture.DIRIGIR_E_CAVALGAR_1).build();
        dirigirECavalgarSkill.increaseGraduation(7);
        CharacterSheet sheet = sheetWithDexterityAndSkill(2, dirigirECavalgarSkill);

        InteractionResult result = dirigirECavalgarInteraction.applyTo(sheet);

        assertEquals(1, result.getDifficultyReduction());
    }

    @Test
    void characterSheetReceiveInteractionDelegatesToApplyTo() {
        CharacterSheet sheet = sheetWithDexterityAndSkill(2, null);

        InteractionResult result = sheet.receiveInteraction(dirigirECavalgarInteraction);

        assertEquals(0, result.getSkillRollBonus());
    }

    @Test
    void applyToAddsTheAdvantageBonusWhenCharacterHasControlarAnimais() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().base(2).build())
                        .build())
                .skillCompetencyAbility(DirigirECavalgarCompetencyAbility.CONTROLAR_ANIMAIS)
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        InteractionResult result = dirigirECavalgarInteraction.applyTo(sheet);

        assertEquals(2, result.getSkillRollBonus());
    }
}
