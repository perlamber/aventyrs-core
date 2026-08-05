package org.aventyrs.core.skill.dominiodomana;

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

class DominioDoManaInteractionTest {

    private final DominioDoManaInteraction dominioDoManaInteraction = new DominioDoManaInteraction();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private CharacterSheet sheetWithFocusAndSkill(int focusBase, CharacterSkill characterSkill) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .focus(AttributeValue.builder().base(focusBase).build())
                        .build());
        if (characterSkill != null) {
            builder.skill(SkillType.DOMINIO_DO_MANA, characterSkill);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    @Test
    void applyToReturnsAttributeTotalPlusGraduationWhenTrained() {
        CharacterSkill dominioDoManaSkill = CharacterSkillFixture.blank(CharacterSkillFixture.DOMINIO_DO_MANA_1).build();
        dominioDoManaSkill.increaseGraduation(1);
        CharacterSheet sheet = sheetWithFocusAndSkill(2, dominioDoManaSkill);

        InteractionResult result = dominioDoManaInteraction.applyTo(sheet);

        assertEquals(3, result.getSkillRollBonus());
        assertEquals(CharacterStatus.CLEAN, result.getResultStatus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToAppliesTheUntrainedPenaltyWhenNeverTrained() {
        CharacterSheet sheet = sheetWithFocusAndSkill(3, null);

        InteractionResult result = dominioDoManaInteraction.applyTo(sheet);

        assertEquals(1, result.getSkillRollBonus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToReflectsProdigioDifficultyReductionOnceUnlocked() {
        CharacterSkill dominioDoManaSkill = CharacterSkillFixture.blank(CharacterSkillFixture.DOMINIO_DO_MANA_1).build();
        dominioDoManaSkill.increaseGraduation(7);
        CharacterSheet sheet = sheetWithFocusAndSkill(2, dominioDoManaSkill);

        InteractionResult result = dominioDoManaInteraction.applyTo(sheet);

        assertEquals(1, result.getDifficultyReduction());
    }

    @Test
    void characterSheetReceiveInteractionDelegatesToApplyTo() {
        CharacterSheet sheet = sheetWithFocusAndSkill(2, null);

        InteractionResult result = sheet.receiveInteraction(dominioDoManaInteraction);

        assertEquals(0, result.getSkillRollBonus());
    }
}
