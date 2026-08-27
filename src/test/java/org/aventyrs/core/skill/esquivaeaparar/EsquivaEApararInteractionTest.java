package org.aventyrs.core.skill.esquivaeaparar;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.item.ArmorItem;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    // ---------------------------------------------------------------------------------------
    // The 4-arg overload: DF/DM, and the armor-Categoria Destreza penalty.
    // ---------------------------------------------------------------------------------------

    @Test
    void theFourArgOverloadWithANullDefenseTypeReproducesTheThreeArgResult() {
        CharacterSheet sheet = sheetWithDexterityAndSkill(3, null);

        InteractionResult threeArg = esquivaEApararInteraction.applyTo(sheet, null, null);
        InteractionResult fourArg = esquivaEApararInteraction.applyTo(sheet, null, null, (DefenseType) null);

        assertEquals(threeArg.getSkillRollBonus(), fourArg.getSkillRollBonus());
        assertEquals(threeArg.getDifficultyReduction(), fourArg.getDifficultyReduction());
    }

    @Test
    void theFourArgOverloadAddsThePhysicalDefenseOfEquippedArmor() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(0).build())
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(3).build())
                        .build())
                .equipment(List.of(ArmorItem.ARMADURA_DE_GLADIADOR))
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        InteractionResult physical = esquivaEApararInteraction.applyTo(sheet, null, null, DefenseType.PHYSICAL);
        InteractionResult magic = esquivaEApararInteraction.applyTo(sheet, null, null, DefenseType.MAGIC);

        // Leve armor, so no Destreza penalty; only the DF/DM columns differ between the two.
        assertEquals(Skill.UNTRAINED_PENALTY + ArmorItem.ARMADURA_DE_GLADIADOR.getPhysicalDefenseBonus(),
                physical.getSkillRollBonus());
        assertEquals(Skill.UNTRAINED_PENALTY + ArmorItem.ARMADURA_DE_GLADIADOR.getMagicDefenseBonus(),
                magic.getSkillRollBonus());
    }

    @Test
    void theFourArgOverloadPicksUpADefesasBlessingOnTheSheet() {
        CharacterSheet sheet = sheetWithDexterityAndSkill(2, null);
        sheet.grantTemporaryBonus(ModifierType.DEFESAS, 2, 2);

        InteractionResult result = esquivaEApararInteraction.applyTo(sheet, null, null, DefenseType.PHYSICAL);

        assertEquals(2 + Skill.UNTRAINED_PENALTY + 2, result.getSkillRollBonus());
    }

    /**
     * super.applyTo resolves reachedDifficultyLevel before this class adds the Defesa, so the
     * override has to recompute it — otherwise exactly the rolls this class exists for report a
     * stale tier.
     */
    @Test
    void reachedDifficultyLevelReflectsThePostDefenseTotal()  {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(0).build())
                        .build())
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.grantTemporaryBonus(ModifierType.PHYSICAL_DEFENSE, 7, 2);
        SkillRoll roll = new SkillRoll(List.of(3, 3, 3));

        InteractionResult withoutDefense = esquivaEApararInteraction.applyTo(sheet, null, roll, (DefenseType) null);
        InteractionResult withDefense = esquivaEApararInteraction.applyTo(sheet, null, roll, DefenseType.PHYSICAL);

        // Untrained (-2) + 9 dice = 7, short of even VERY_EASY (12); +7 DF lifts it to 14 (EASY).
        assertEquals(null, withoutDefense.getReachedDifficultyLevel());
        assertEquals(DifficultyLevel.EASY, withDefense.getReachedDifficultyLevel());
    }

    private CharacterSheet sheetWearing(final ArmorItem armor, final EsquivaEApararCompetencyAbility ability) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(4).build())
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(4).build())
                        .build())
                .equipment(List.of(armor));
        if (ability != null) {
            builder.skillCompetencyAbility(ability);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    @Test
    void leveArmorCostsNoneOfTheCharactersDestreza() {
        // ARMADURA_DE_GLADIADOR is LIGHT: full Destreza, plus its own DF column.
        InteractionResult result = esquivaEApararInteraction.applyTo(
                sheetWearing(ArmorItem.ARMADURA_DE_GLADIADOR, null), null, null, DefenseType.PHYSICAL);

        assertEquals(4 + Skill.UNTRAINED_PENALTY + ArmorItem.ARMADURA_DE_GLADIADOR.getPhysicalDefenseBonus(),
                result.getSkillRollBonus());
    }

    @Test
    void mediaArmorCostsHalfTheCharactersDestreza() {
        // COURACA is MEDIUM: half of 4 Destreza is subtracted.
        InteractionResult result = esquivaEApararInteraction.applyTo(
                sheetWearing(ArmorItem.COURACA, null), null, null, DefenseType.PHYSICAL);

        assertEquals(4 + Skill.UNTRAINED_PENALTY + ArmorItem.COURACA.getPhysicalDefenseBonus() - 2,
                result.getSkillRollBonus());
    }

    @Test
    void pesadaArmorCostsAllOfTheCharactersDestreza() {
        // ARMADURA_COMPLETA is HEAVY: all 4 Destreza is subtracted.
        InteractionResult result = esquivaEApararInteraction.applyTo(
                sheetWearing(ArmorItem.ARMADURA_COMPLETA, null), null, null, DefenseType.PHYSICAL);

        assertEquals(4 + Skill.UNTRAINED_PENALTY + ArmorItem.ARMADURA_COMPLETA.getPhysicalDefenseBonus() - 4,
                result.getSkillRollBonus());
    }

    @Test
    void encouracadoEVelozShiftsTheArmorPenaltyOneBracketLighter() {
        InteractionResult media = esquivaEApararInteraction.applyTo(
                sheetWearing(ArmorItem.COURACA, EsquivaEApararCompetencyAbility.ENCOURACADO_E_VELOZ),
                null, null, DefenseType.PHYSICAL);
        InteractionResult pesada = esquivaEApararInteraction.applyTo(
                sheetWearing(ArmorItem.ARMADURA_COMPLETA, EsquivaEApararCompetencyAbility.ENCOURACADO_E_VELOZ),
                null, null, DefenseType.PHYSICAL);

        // Média now costs nothing, and Pesada costs only half.
        assertEquals(4 + Skill.UNTRAINED_PENALTY + ArmorItem.COURACA.getPhysicalDefenseBonus(),
                media.getSkillRollBonus());
        assertEquals(4 + Skill.UNTRAINED_PENALTY + ArmorItem.ARMADURA_COMPLETA.getPhysicalDefenseBonus() - 2,
                pesada.getSkillRollBonus());
    }

    @Test
    void theHeaviestEquippedArmorSetsThePenaltyBracket() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(4).build())
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(4).build())
                        .build())
                .equipment(List.of(ArmorItem.ARMADURA_DE_GLADIADOR, ArmorItem.ARMADURA_COMPLETA))
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        InteractionResult result = esquivaEApararInteraction.applyTo(sheet, null, null, DefenseType.PHYSICAL);

        // Wearing something Leve doesn't excuse the Pesada piece: all 4 Destreza is still lost.
        int bothDefenseColumns = ArmorItem.ARMADURA_DE_GLADIADOR.getPhysicalDefenseBonus()
                + ArmorItem.ARMADURA_COMPLETA.getPhysicalDefenseBonus();
        assertEquals(4 + Skill.UNTRAINED_PENALTY + bothDefenseColumns - 4, result.getSkillRollBonus());
    }

    @Test
    void theArmorPenaltyAppliesEvenWithoutADefenseType() {
        // The penalty is a property of what's worn, not of what's being resisted.
        InteractionResult result = esquivaEApararInteraction.applyTo(
                sheetWearing(ArmorItem.ARMADURA_COMPLETA, null), null, null, (DefenseType) null);

        assertEquals(4 + Skill.UNTRAINED_PENALTY - 4, result.getSkillRollBonus());
    }
}
