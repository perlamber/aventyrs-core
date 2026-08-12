package org.aventyrs.core.skill.esquivaeaparar;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EsquivaEApararInteractionTest {

    private final EsquivaEApararInteraction esquivaEApararInteraction = new EsquivaEApararInteraction();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private CharacterSheet sheetWithDexterityAndSkill(int dexterityBase, CharacterSkill characterSkill) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(dexterityBase).build())
                        .build());
        if (characterSkill != null) {
            builder.skill(SkillType.ESQUIVA_E_APARAR, characterSkill);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    @Test
    void applyToReturnsAttributeTotalPlusGraduationWhenTrained() {
        CharacterSkill esquivaEApararSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ESQUIVA_E_APARAR_1).build();
        esquivaEApararSkill.increaseGraduation(1);
        CharacterSheet sheet = sheetWithDexterityAndSkill(2, esquivaEApararSkill);

        InteractionResult result = esquivaEApararInteraction.applyTo(sheet);

        assertEquals(3, result.getSkillRollBonus());
        assertEquals(CharacterStatus.CLEAN, result.getResultStatus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToAppliesTheUntrainedPenaltyWhenNeverTrained() {
        CharacterSheet sheet = sheetWithDexterityAndSkill(3, null);

        InteractionResult result = esquivaEApararInteraction.applyTo(sheet);

        assertEquals(1, result.getSkillRollBonus());
        assertEquals(0, result.getDifficultyReduction());
    }

    @Test
    void applyToReflectsProdigioDifficultyReductionOnceUnlocked() {
        CharacterSkill esquivaEApararSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ESQUIVA_E_APARAR_1).build();
        esquivaEApararSkill.increaseGraduation(7);
        CharacterSheet sheet = sheetWithDexterityAndSkill(2, esquivaEApararSkill);

        InteractionResult result = esquivaEApararInteraction.applyTo(sheet);

        assertEquals(1, result.getDifficultyReduction());
    }

    @Test
    void applyToAddsTheFocadoBonusOnceUnlocked() {
        CharacterSkill esquivaEApararSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ESQUIVA_E_APARAR_1).build();
        esquivaEApararSkill.increaseGraduation(3);
        CharacterSheet sheet = sheetWithDexterityAndSkill(2, esquivaEApararSkill);

        InteractionResult result = esquivaEApararInteraction.applyTo(sheet);

        assertEquals(2 + 3 + 1, result.getSkillRollBonus());
    }

    @Test
    void applyToAddsBothFocadoAndLendaBonusesOnceBothUnlocked() {
        CharacterSkill esquivaEApararSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ESQUIVA_E_APARAR_1).build();
        esquivaEApararSkill.increaseGraduation(10);
        CharacterSheet sheet = sheetWithDexterityAndSkill(2, esquivaEApararSkill);

        InteractionResult result = esquivaEApararInteraction.applyTo(sheet);

        assertEquals(2 + 10 + 1 + 2, result.getSkillRollBonus());
    }

    @Test
    void characterSheetReceiveInteractionDelegatesToApplyTo() {
        CharacterSheet sheet = sheetWithDexterityAndSkill(2, null);

        InteractionResult result = sheet.receiveInteraction(esquivaEApararInteraction);

        assertEquals(0, result.getSkillRollBonus());
    }

    @Test
    void applyToAddsTheMovimentoDefensivoBonus() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(2).build())
                        .build())
                .skillCompetencyAbility(EsquivaEApararCompetencyAbility.MOVIMENTO_DEFENSIVO)
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        InteractionResult result = esquivaEApararInteraction.applyTo(sheet);

        assertEquals(3, result.getSkillRollBonus());
    }

    @Test
    void applyToAppliesTheDefenseSizeCategoryModifier() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(2).build())
                        .build())
                .sizeCategory(SizeCategory.MINUS_TWO)
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        InteractionResult result = esquivaEApararInteraction.applyTo(sheet);

        // 2 dexterity + untrained penalty + SizeCategory.MINUS_TWO's defense modifier (+1).
        assertEquals(2 + Skill.UNTRAINED_PENALTY + SizeCategory.MINUS_TWO.getDefenseModifier(), result.getSkillRollBonus());
    }
}
