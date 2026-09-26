package org.aventyrs.core.monster;

import org.aventyrs.core.ability.DexterityAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.feat.MonstruosoFeat;
import org.aventyrs.core.feat.ArtesMarciaisFeat;
import org.aventyrs.core.monster.MonsterViolation.Code;
import org.aventyrs.core.monster.model.MonsterModel;
import org.aventyrs.core.monster.model.almaelemental.AlmaElementalAbility;
import org.aventyrs.core.monster.model.aspectohumanoide.AspectoHumanoideAbility;
import org.aventyrs.core.monster.model.cireneia.CireneiaAbility;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonsterRulesTest {

    private static MonsterBlueprint.MonsterBlueprintBuilder goblin() {
        return SampleMonster.GOBLIN_SELVAGEM.get().toBuilder();
    }

    private static MonsterBlueprint.MonsterBlueprintBuilder pantera() {
        return SampleMonster.PANTERA_DE_CIRENEIA.get().toBuilder();
    }

    private static Set<Code> codes(final MonsterBlueprint blueprint) {
        return MonsterRules.validate(blueprint).stream().map(MonsterViolation::code).collect(Collectors.toSet());
    }

    // ---- Samples -------------------------------------------------------------------------------

    @Test
    void everySampleIsLegal() {
        for (SampleMonster sample : SampleMonster.values()) {
            assertEquals(List.of(), MonsterRules.validate(sample.get()), sample.name());
        }
    }

    // ---- Budgets -------------------------------------------------------------------------------

    @Test
    void budgetsGrowWithTheGrauDePoder() {
        MonsterBlueprint pantera = pantera().build();
        assertEquals(1 + 10, MonsterRules.abilityBudget(pantera));   // one Modelo + GP30/3
        assertEquals(7, MonsterRules.progressionUpgradeBudget(pantera));
        assertEquals(6, MonsterRules.featBudget(pantera));
        assertEquals(3 + 3, MonsterRules.egoPointBudget(pantera));   // Predador + GP30/10
        assertEquals(2, MonsterRules.gnoseUpgradeBudget(pantera));
    }

    @Test
    void anExemplarTakesTwoExtraTalentosAndOneMorePerSixGp() {
        MonsterBlueprint exemplar = goblin().powerDegree(12).kind(MonsterKind.EXEMPLAR).build();
        assertEquals(12 / 5 + 2 + 12 / 6, MonsterRules.featBudget(exemplar));
    }

    @Test
    void theGnoseBudgetCountsBonusRacial() {
        MonsterBlueprint blueprint = goblin().attributeBase(AttributeDomain.GNOSE, 4).build();
        assertEquals(4, MonsterRules.gnoseUpgradeBudget(blueprint));
    }

    // ---- Violations ----------------------------------------------------------------------------

    @Test
    void aBlankNameAndANegativeGpAreRefused() {
        Set<Code> codes = codes(goblin().name(" ").powerDegree(-1).build());
        assertTrue(codes.contains(Code.NAME_BLANK));
        assertTrue(codes.contains(Code.NEGATIVE_POWER_DEGREE));
    }

    @Test
    void anAttributeBaseAboveFiveIsRefused() {
        assertTrue(codes(goblin().attributeBase(AttributeDomain.STRENGTH, 6).build())
                .contains(Code.ATTRIBUTE_BASE_OUT_OF_RANGE));
    }

    @Test
    void anAttributeBaseBelowOneIsRefused() {
        assertTrue(codes(goblin().attributeBase(AttributeDomain.CHARISMA, 0).build())
                .contains(Code.ATTRIBUTE_BASE_OUT_OF_RANGE));
    }

    @Test
    void spendingMoreThanTenAttributePointsIsRefused() {
        // the Goblin spends all ten; one more point anywhere is over
        MonsterViolation violation = MonsterRules.validate(goblin().attributeBase(AttributeDomain.FOCUS, 2).build()).get(0);
        assertEquals(Code.ATTRIBUTE_POINTS_EXCEEDED, violation.code());
        assertEquals(11, violation.actual());
        assertEquals(10, violation.limit());
    }

    @Test
    void morModelosThanTheCategoriaAllowsAreRefused() {
        MonsterBlueprint blueprint = goblin()
                .model(MonsterModel.ALMA_ELEMENTAL).ability(MonstrousAbilitySelection.of(AlmaElementalAbility.SANGUE_ELEMENTAL))
                .model(MonsterModel.BESTA_ALADA)
                .model(MonsterModel.MUTANTE_MONSTRUOSO)
                .build();
        assertTrue(codes(blueprint).contains(Code.TOO_MANY_MODELS));
    }

    @Test
    void theSameModeloTwiceIsRefused() {
        assertTrue(codes(goblin().model(MonsterModel.ASPECTO_HUMANOIDE).build()).contains(Code.DUPLICATE_MODEL));
    }

    @Test
    void aModeloWithoutAPresaHabilidadeIsRefused() {
        MonsterBlueprint blueprint = goblin().model(MonsterModel.ALMA_ELEMENTAL).build();
        List<MonsterViolation> violations = MonsterRules.validate(blueprint);
        assertTrue(violations.contains(MonsterViolation.of(Code.MODEL_WITHOUT_STARTING_ABILITY, "ALMA_ELEMENTAL")));
    }

    @Test
    void aHabilidadeFromAModeloNotHeldIsRefused() {
        MonsterBlueprint blueprint = goblin().clearAbilities()
                .ability(MonstrousAbilitySelection.of(AspectoHumanoideAbility.CORPO_HUMANOIDE))
                .ability(MonstrousAbilitySelection.of(CireneiaAbility.MOVIMENTO_APRIMORADO))
                .build();
        assertTrue(codes(blueprint).contains(Code.ABILITY_MODEL_NOT_HELD));
    }

    @Test
    void aHabilidadeAboveTheCategoriaIsRefused() {
        MonsterBlueprint blueprint = pantera().powerDegree(11)
                .clearAbilities()
                .clearProgressionUpgrades()
                .clearEgoAllocation()
                .ability(MonstrousAbilitySelection.of(CireneiaAbility.ATRIBUTOS_APRIMORADOS))
                .ability(MonstrousAbilitySelection.of(CireneiaAbility.CELERIDADE))
                .build();
        assertEquals(List.of(MonsterViolation.of(Code.ABILITY_TIER_TOO_HIGH, "CELERIDADE")), MonsterRules.validate(blueprint));
    }

    @Test
    void theSameHabilidadeTwiceIsRefused() {
        MonsterBlueprint blueprint = goblin().ability(MonstrousAbilitySelection.of(AspectoHumanoideAbility.CORPO_HUMANOIDE)).build();
        assertTrue(codes(blueprint).contains(Code.DUPLICATE_ABILITY));
    }

    @Test
    void moreHabilidadesThanOnePerModeloAndOnePerThreeGpAreRefused() {
        // GP 2: one Modelo + 0 → 1 allowed, the Goblin holds 2
        MonsterBlueprint blueprint = goblin().powerDegree(2).clearProgressionUpgrades().build();
        assertEquals(List.of(MonsterViolation.of(Code.TOO_MANY_ABILITIES, null, 2, 1)), MonsterRules.validate(blueprint));
    }

    @Test
    void aChoiceHabilidadeNeedsAChoiceItOffers() {
        MonsterBlueprint missing = pantera().clearAbilities()
                .ability(MonstrousAbilitySelection.of(CireneiaAbility.ATRIBUTOS_APRIMORADOS))
                .ability(MonstrousAbilitySelection.of(CireneiaAbility.RELAMPEJANTE))
                .build();
        MonsterBlueprint wrong = pantera().clearAbilities()
                .ability(MonstrousAbilitySelection.of(CireneiaAbility.ATRIBUTOS_APRIMORADOS))
                .ability(MonstrousAbilitySelection.of(CireneiaAbility.RELAMPEJANTE, "FIRE"))
                .build();
        MonsterBlueprint unasked = pantera().clearAbilities()
                .ability(MonstrousAbilitySelection.of(CireneiaAbility.ATRIBUTOS_APRIMORADOS, "REACTIONS"))
                .build();
        assertTrue(codes(missing).contains(Code.INVALID_ABILITY_CHOICE));
        assertTrue(codes(wrong).contains(Code.INVALID_ABILITY_CHOICE));
        assertTrue(codes(unasked).contains(Code.INVALID_ABILITY_CHOICE));
    }

    @Test
    void anUpgradeOnAnUntrainedPericiaIsRefused() {
        assertTrue(codes(goblin().clearGnoseUpgrades().gnoseUpgrade(SkillType.PERSUASAO).build())
                .contains(Code.UPGRADE_ON_UNTRAINED_SKILL));
        assertTrue(codes(goblin().clearProgressionUpgrades().progressionUpgrade(SkillType.PERSUASAO, 1).build())
                .contains(Code.UPGRADE_ON_UNTRAINED_SKILL));
    }

    @Test
    void moreGnoseUpgradesThanGnoseAreRefused() {
        assertTrue(codes(goblin().gnoseUpgrade(SkillType.ATTENTION).build()).contains(Code.GNOSE_UPGRADES_EXCEEDED));
    }

    @Test
    void moreGpUpgradesThanOnePerFourGpAreRefused() {
        assertTrue(codes(goblin().progressionUpgrade(SkillType.ATTENTION, 1).build())
                .contains(Code.PROGRESSION_UPGRADES_EXCEEDED));
    }

    @Test
    void aNegativeUpgradeOrEgoAllocationIsRefused() {
        assertTrue(codes(goblin().progressionUpgrade(SkillType.ATTENTION, -1).build()).contains(Code.NEGATIVE_AMOUNT));
        assertTrue(codes(goblin().egoPoint(EgoDomain.SORTE, -1).build()).contains(Code.NEGATIVE_AMOUNT));
    }

    @Test
    void talentosAreCappedAndMustBeGeralOrMonstruoso() {
        MonsterBlueprint tooMany = goblin().powerDegree(4).clearProgressionUpgrades()
                .feat(MonstruosoFeat.values()[0]).build();
        assertTrue(codes(tooMany).contains(Code.TOO_MANY_FEATS));

        MonsterBlueprint monstruoso = goblin().feat(MonstruosoFeat.values()[0]).build();
        assertEquals(List.of(), MonsterRules.validate(monstruoso));

        MonsterBlueprint geral = goblin().feat(ArtesMarciaisFeat.values()[0]).build();
        assertFalse(codes(geral).contains(Code.FEAT_NOT_ALLOWED));
    }

    @Test
    void egoPointsPastTheBudgetAreRefused() {
        assertTrue(codes(pantera().egoPoint(EgoDomain.AUTOCONTROLE, 1).build()).contains(Code.EGO_POINTS_EXCEEDED));
    }

    @Test
    void aRegularMayNotHoldExemplarTraits() {
        MonsterBlueprint regular = goblin().attributeAbility(DexterityAbility.values()[0]).famaPositiva(1).build();
        List<MonsterViolation> violations = MonsterRules.validate(regular);
        assertTrue(violations.contains(MonsterViolation.of(Code.EXEMPLAR_ONLY_TRAIT, "ATTRIBUTE_ABILITIES")));
        assertTrue(violations.contains(MonsterViolation.of(Code.EXEMPLAR_ONLY_TRAIT, "FAMA")));

        MonsterBlueprint exemplar = regular.toBuilder().kind(MonsterKind.EXEMPLAR).build();
        assertFalse(codes(exemplar).contains(Code.EXEMPLAR_ONLY_TRAIT));
    }

    @Test
    void theMestresAdjustmentsAreNeverValidated() {
        MonsterBlueprint blueprint = goblin().adjustments(MonsterAdjustments.builder()
                .physicalDefense(40).actionPoints(-3).skillLevelShift(SkillType.ATTENTION, 7).build()).build();
        assertEquals(List.of(), MonsterRules.validate(blueprint));
    }

    // ---- Derivation ----------------------------------------------------------------------------

    @Test
    void theGoblinsGdsAreBuiltFromItsUpgradesAndHalfItsAttributes() {
        MonsterBlueprint goblin = goblin().build();
        assertEquals(SkillDifficulty.of(DifficultyLevel.EASY, 1), MonsterRules.skillDifficulty(goblin, SkillType.ATAQUE_CORPO_A_CORPO));
        assertEquals(SkillDifficulty.of(DifficultyLevel.EASY, 2), MonsterRules.skillDifficulty(goblin, SkillType.ESQUIVA_E_APARAR));
        assertEquals(SkillDifficulty.of(DifficultyLevel.EASY, 2), MonsterRules.skillDifficulty(goblin, SkillType.FURTIVIDADE));
        assertEquals(SkillDifficulty.of(DifficultyLevel.VERY_EASY, 1), MonsterRules.skillDifficulty(goblin, SkillType.ATTENTION));
    }

    @Test
    void anUntrainedPericiaPresentsTheBaseGdPlusHalfItsAttribute() {
        MonsterBlueprint goblin = goblin().build();
        assertEquals(SkillDifficulty.of(DifficultyLevel.VERY_EASY, 0), MonsterRules.skillDifficulty(goblin, SkillType.PERSUASAO));
    }

    @Test
    void anExemplarStartsOneStepHigher() {
        MonsterBlueprint exemplar = goblin().kind(MonsterKind.EXEMPLAR).build();
        assertEquals(DifficultyLevel.EASY, MonsterRules.skillDifficulty(exemplar, SkillType.PERSUASAO).level());
        assertEquals(DifficultyLevel.MEDIUM, MonsterRules.skillDifficulty(exemplar, SkillType.ATAQUE_CORPO_A_CORPO).level());
    }

    @Test
    void upgradesStopAtTheCategoriasMaximumGd() {
        // Deviante, GP 24: Gnose + 6 GP upgrades is 7 steps past Muito Fácil — clamped to Muito Difícil
        MonsterBlueprint blueprint = goblin().powerDegree(24)
                .clearProgressionUpgrades().progressionUpgrade(SkillType.ATAQUE_CORPO_A_CORPO, 6).build();
        assertEquals(DifficultyLevel.VERY_HARD, MonsterRules.skillDifficulty(blueprint, SkillType.ATAQUE_CORPO_A_CORPO).level());

        MonsterBlueprint exemplar = blueprint.toBuilder().kind(MonsterKind.EXEMPLAR).build();
        assertEquals(DifficultyLevel.UNLIKELY, MonsterRules.skillDifficulty(exemplar, SkillType.ATAQUE_CORPO_A_CORPO).level());
    }

    @Test
    void habilidadeStepsAndTheMestresStepsGoPastTheCeiling() {
        // Pantera's Esquiva e Aparar: 3 steps → Difícil, + Liberdade Selvagem → Muito Difícil;
        // the Mestre's +2 then lands on Inimaginável, past the Predador's Improvável
        MonsterBlueprint blueprint = pantera().adjustments(MonsterAdjustments.builder()
                .skillLevelShift(SkillType.ESQUIVA_E_APARAR, 2).skillBonus(SkillType.ESQUIVA_E_APARAR, -1).build()).build();
        assertEquals(SkillDifficulty.of(DifficultyLevel.UNIMAGINABLE, 3),
                MonsterRules.skillDifficulty(blueprint, SkillType.ESQUIVA_E_APARAR));
    }

    @Test
    void thePanterasGdsIncludeItsHabilidades() {
        MonsterBlueprint pantera = pantera().build();
        assertEquals(SkillDifficulty.of(DifficultyLevel.VERY_HARD, 1), MonsterRules.skillDifficulty(pantera, SkillType.ATAQUE_CORPO_A_CORPO));
        assertEquals(SkillDifficulty.of(DifficultyLevel.VERY_HARD, 4), MonsterRules.skillDifficulty(pantera, SkillType.ESQUIVA_E_APARAR));
        assertEquals(SkillDifficulty.of(DifficultyLevel.HARD, 4), MonsterRules.skillDifficulty(pantera, SkillType.FURTIVIDADE));
        assertEquals(SkillDifficulty.of(DifficultyLevel.VERY_EASY, 1), MonsterRules.skillDifficulty(pantera, SkillType.ATTENTION));
        // untrained, but Destreza-governed: Liberdade Selvagem still steps it
        assertEquals(SkillDifficulty.of(DifficultyLevel.EASY, 4), MonsterRules.skillDifficulty(pantera, SkillType.ATAQUE_A_DISTANCIA));
    }

    @Test
    void bonusRacialIsTrimmedToTheCategoriasAttributeCeiling() {
        MonsterBlueprint pantera = pantera().build();
        assertEquals(3, MonsterRules.racialBonus(pantera, AttributeDomain.DEXTERITY));   // 5 + 4, capped at 8
        assertEquals(8, MonsterRules.attributeTotal(pantera, AttributeDomain.DEXTERITY));

        MonsterBlueprint exemplar = pantera.toBuilder().kind(MonsterKind.EXEMPLAR).build();
        assertEquals(9, MonsterRules.attributeTotal(exemplar, AttributeDomain.DEXTERITY));
    }

    @Test
    void theFixedNumbersComeFromTheCategoria() {
        MonsterBlueprint pantera = pantera().build();
        assertEquals(4, MonsterRules.fixedActionPoints(pantera));
        assertEquals(8, MonsterRules.lifeMultiplier(pantera));
        assertEquals(3, MonsterRules.determinationMultiplier(pantera));
        assertEquals(2, MonsterRules.manaMultiplier(pantera));

        MonsterBlueprint adjusted = pantera.toBuilder().adjustments(MonsterAdjustments.builder()
                .actionPoints(-1).lifeMultiplier(2).build()).build();
        assertEquals(3, MonsterRules.fixedActionPoints(adjusted));
        assertEquals(10, MonsterRules.lifeMultiplier(adjusted));
    }
}
