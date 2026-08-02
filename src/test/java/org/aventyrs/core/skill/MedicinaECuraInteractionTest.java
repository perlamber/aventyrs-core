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

class MedicinaECuraInteractionTest {

    private final MedicinaECuraInteraction medicinaECuraInteraction = new MedicinaECuraInteraction();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private CharacterSheet sheetWithGnoseAndSkill(int gnoseBase, CharacterSkill characterSkill) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .gnose(AttributeValue.builder().base(gnoseBase).build())
                        .build());
        if (characterSkill != null) {
            builder.skill(SkillType.MEDICINA_E_CURA, characterSkill);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    @Test
    void applyToReturnsAttributeTotalPlusGraduationWhenTrained() {
        CharacterSkill medicinaECuraSkill = CharacterSkillFixture.blank(CharacterSkillFixture.MEDICINA_E_CURA_1).build();
        medicinaECuraSkill.increaseGraduation(1);
        CharacterSheet sheet = sheetWithGnoseAndSkill(2, medicinaECuraSkill);

        InteractionResult result = medicinaECuraInteraction.applyTo(sheet);

        assertEquals(3, result.getSkillRollBonus());
        assertEquals(CharacterStatus.CLEAN, result.getResultStatus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToAppliesTheUntrainedPenaltyWhenNeverTrained() {
        CharacterSheet sheet = sheetWithGnoseAndSkill(3, null);

        InteractionResult result = medicinaECuraInteraction.applyTo(sheet);

        assertEquals(1, result.getSkillRollBonus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToReflectsProdigioDifficultyReductionOnceUnlocked() {
        CharacterSkill medicinaECuraSkill = CharacterSkillFixture.blank(CharacterSkillFixture.MEDICINA_E_CURA_1).build();
        medicinaECuraSkill.increaseGraduation(7);
        CharacterSheet sheet = sheetWithGnoseAndSkill(2, medicinaECuraSkill);

        InteractionResult result = medicinaECuraInteraction.applyTo(sheet);

        assertEquals(1, result.getDifficultyReduction());
    }

    @Test
    void characterSheetReceiveInteractionDelegatesToApplyTo() {
        CharacterSheet sheet = sheetWithGnoseAndSkill(2, null);

        InteractionResult result = sheet.receiveInteraction(medicinaECuraInteraction);

        assertEquals(0, result.getSkillRollBonus());
    }
}
