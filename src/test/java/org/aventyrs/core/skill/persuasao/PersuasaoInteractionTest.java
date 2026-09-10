package org.aventyrs.core.skill.persuasao;

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

class PersuasaoInteractionTest {

    private final PersuasaoInteraction persuasaoInteraction = new PersuasaoInteraction();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private CharacterSheet sheetWithCharismaAndSkill(int charismaBase, CharacterSkill characterSkill) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .charisma(AttributeValue.builder().domain(AttributeDomain.CHARISMA).base(charismaBase).build())
                        .build());
        if (characterSkill != null) {
            builder.skill(SkillType.PERSUASAO, characterSkill);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    @Test
    void applyToReturnsAttributeTotalPlusGraduationWhenTrained() {
        CharacterSkill persuasaoSkill = CharacterSkillFixture.blank(CharacterSkillFixture.PERSUASAO_1).build();
        persuasaoSkill.increaseGraduation(1);
        CharacterSheet sheet = sheetWithCharismaAndSkill(2, persuasaoSkill);

        InteractionResult result = persuasaoInteraction.applyTo(sheet);

        assertEquals(3, result.getSkillRollBonus());
        assertEquals(CharacterStatus.CLEAN, result.getResultStatus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToAppliesTheUntrainedPenaltyWhenNeverTrained() {
        CharacterSheet sheet = sheetWithCharismaAndSkill(3, null);

        InteractionResult result = persuasaoInteraction.applyTo(sheet);

        assertEquals(1, result.getSkillRollBonus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToReflectsProdigioDifficultyReductionOnceUnlocked() {
        CharacterSkill persuasaoSkill = CharacterSkillFixture.blank(CharacterSkillFixture.PERSUASAO_1).build();
        persuasaoSkill.increaseGraduation(7);
        CharacterSheet sheet = sheetWithCharismaAndSkill(2, persuasaoSkill);

        InteractionResult result = persuasaoInteraction.applyTo(sheet);

        assertEquals(1, result.getDifficultyReduction());
    }

    @Test
    void characterSheetReceiveInteractionDelegatesToApplyTo() {
        CharacterSheet sheet = sheetWithCharismaAndSkill(2, null);

        InteractionResult result = sheet.receiveInteraction(persuasaoInteraction);

        assertEquals(0, result.getSkillRollBonus());
    }

    /**
     * An equipped Capa whose Favor grants "Vantagem em Persuasão" (a flat +2 on the whole
     * Perícia) is summed into the roll — the item-Favor pass {@code
     * AbstractSkillInteraction#sumEquipmentRollBonuses} now makes.
     */
    @Test
    void applyToSumsAnEquippedCapaEsvoacanteFavorPersuasaoVantagem() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .charisma(AttributeValue.builder().domain(AttributeDomain.CHARISMA).base(3).build())
                        .build())
                .equipment(java.util.List.of(org.aventyrs.core.item.CloakItem.CAPA_ESVOACANTE))
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        InteractionResult result = persuasaoInteraction.applyTo(sheet);

        // charisma 3, untrained (-2), + Vantagem (+2) = 3
        assertEquals(3, result.getSkillRollBonus());
    }
}
