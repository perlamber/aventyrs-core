package org.aventyrs.core.skill.ataqueadistancia;

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

class AtaqueADistanciaInteractionTest {

    private final AtaqueADistanciaInteraction ataqueADistanciaInteraction = new AtaqueADistanciaInteraction();

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
            builder.skill(SkillType.ATAQUE_A_DISTANCIA, characterSkill);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    @Test
    void applyToReturnsAttributeTotalPlusGraduationWhenTrained() {
        CharacterSkill ataqueADistanciaSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_A_DISTANCIA_1).build();
        ataqueADistanciaSkill.increaseGraduation(1);
        CharacterSheet sheet = sheetWithDexterityAndSkill(2, ataqueADistanciaSkill);

        InteractionResult result = ataqueADistanciaInteraction.applyTo(sheet);

        assertEquals(3, result.getSkillRollBonus());
        assertEquals(CharacterStatus.CLEAN, result.getResultStatus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToAppliesTheUntrainedPenaltyWhenNeverTrained() {
        CharacterSheet sheet = sheetWithDexterityAndSkill(3, null);

        InteractionResult result = ataqueADistanciaInteraction.applyTo(sheet);

        assertEquals(1, result.getSkillRollBonus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToReflectsProdigioDifficultyReductionOnceUnlocked() {
        CharacterSkill ataqueADistanciaSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_A_DISTANCIA_1).build();
        ataqueADistanciaSkill.increaseGraduation(7);
        CharacterSheet sheet = sheetWithDexterityAndSkill(2, ataqueADistanciaSkill);

        InteractionResult result = ataqueADistanciaInteraction.applyTo(sheet);

        assertEquals(1, result.getDifficultyReduction());
    }

    @Test
    void characterSheetReceiveInteractionDelegatesToApplyTo() {
        CharacterSheet sheet = sheetWithDexterityAndSkill(2, null);

        InteractionResult result = sheet.receiveInteraction(ataqueADistanciaInteraction);

        assertEquals(0, result.getSkillRollBonus());
    }
}
