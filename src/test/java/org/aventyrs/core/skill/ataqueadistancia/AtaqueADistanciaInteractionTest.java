package org.aventyrs.core.skill.ataqueadistancia;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.TestSpell;
import org.aventyrs.core.race.Anao;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(dexterityBase).build())
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

    private CharacterSheet sheetWithFrieza() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skillCompetencyAbility(AtaqueADistanciaCompetencyAbility.FRIEZA)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private CharacterSheet plainEnemySheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }

    @Test
    void applyToGrantsFriezaDamageBonusWhenTheAttackTargetIsWithinDistanciaCurta() {
        CharacterSheet sheet = sheetWithFrieza();
        CharacterSheet attackTarget = plainEnemySheet();
        SceneContext sceneContext = new SceneContext(List.of(), List.of(attackTarget), Map.of(attackTarget, Range.DISTANCIA_CURTA));

        InteractionResult result = ataqueADistanciaInteraction.applyTo(sheet, sceneContext, null, attackTarget);

        assertEquals(Skill.ADVANTAGE_BONUS, result.getDamageBonus().getValue());
        assertEquals(DamageType.FISICO, result.getDamageBonus().getType());
    }

    @Test
    void applyToGrantsNoFriezaDamageBonusWhenTheAttackTargetIsFar() {
        CharacterSheet sheet = sheetWithFrieza();
        CharacterSheet attackTarget = plainEnemySheet();
        SceneContext sceneContext = new SceneContext(List.of(), List.of(attackTarget), Map.of(attackTarget, Range.DISTANCIA_LONGA));

        InteractionResult result = ataqueADistanciaInteraction.applyTo(sheet, sceneContext, null, attackTarget);

        assertNull(result.getDamageBonus());
    }

    @Test
    void applyToGrantsNoFriezaDamageBonusWhenAnotherEnemyIsCloseButNotTheAttackTarget() {
        CharacterSheet sheet = sheetWithFrieza();
        CharacterSheet nearbyEnemy = plainEnemySheet();
        CharacterSheet attackTarget = plainEnemySheet();
        SceneContext sceneContext = new SceneContext(List.of(), List.of(nearbyEnemy, attackTarget), Map.of(nearbyEnemy, Range.DISTANCIA_CURTA));

        InteractionResult result = ataqueADistanciaInteraction.applyTo(sheet, sceneContext, null, attackTarget);

        assertNull(result.getDamageBonus());
    }

    @Test
    void applyToGrantsNoFriezaDamageBonusWithoutTheAbility() {
        CharacterSheet sheet = sheetWithDexterityAndSkill(2, null);
        CharacterSheet attackTarget = plainEnemySheet();
        SceneContext sceneContext = new SceneContext(List.of(), List.of(attackTarget), Map.of(attackTarget, Range.DISTANCIA_CURTA));

        InteractionResult result = ataqueADistanciaInteraction.applyTo(sheet, sceneContext, null, attackTarget);

        assertNull(result.getDamageBonus());
    }

    @Test
    void applyToGrantsNoFriezaDamageBonusWithoutASceneContext() {
        CharacterSheet sheet = sheetWithFrieza();
        CharacterSheet attackTarget = plainEnemySheet();

        InteractionResult result = ataqueADistanciaInteraction.applyTo(sheet, null, null, attackTarget);

        assertNull(result.getDamageBonus());
    }

    @Test
    void applyToGrantsNoFriezaDamageBonusWithoutAnAttackTarget() {
        CharacterSheet sheet = sheetWithFrieza();
        SceneContext sceneContext = new SceneContext(List.of(), List.of(), Map.of());

        InteractionResult result = ataqueADistanciaInteraction.applyTo(sheet, sceneContext, null, null);

        assertNull(result.getDamageBonus());
    }

    private CharacterSheet sheetWithRace(Race race) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .race(race)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private CharacterSheet enemySheetWithSizeCategory(SizeCategory sizeCategory) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .sizeCategory(sizeCategory)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void applyToGrantsAbatedoresDeGigantesBonusAgainstATargetTwoOrMoreSizeCategoriesLarger() {
        CharacterSheet sheet = sheetWithRace(new Anao());
        CharacterSheet attackTarget = enemySheetWithSizeCategory(SizeCategory.PLUS_TWO);
        int baseline = ataqueADistanciaInteraction.applyTo(sheet).getSkillRollBonus();

        InteractionResult result = ataqueADistanciaInteraction.applyTo(sheet, null, null, attackTarget);

        assertEquals(baseline + Skill.ADVANTAGE_BONUS, result.getSkillRollBonus());
    }

    @Test
    void applyToGrantsNoAbatedoresDeGigantesBonusWhenTheSizeDifferenceIsOnlyOne() {
        CharacterSheet sheet = sheetWithRace(new Anao());
        CharacterSheet attackTarget = enemySheetWithSizeCategory(SizeCategory.PLUS_ONE);
        int baseline = ataqueADistanciaInteraction.applyTo(sheet).getSkillRollBonus();

        InteractionResult result = ataqueADistanciaInteraction.applyTo(sheet, null, null, attackTarget);

        assertEquals(baseline, result.getSkillRollBonus());
    }

    @Test
    void applyToGrantsNoAbatedoresDeGigantesBonusForANonAnaoRace() {
        CharacterSheet sheet = sheetWithRace(new Human());
        CharacterSheet attackTarget = enemySheetWithSizeCategory(SizeCategory.PLUS_TWO);
        int baseline = ataqueADistanciaInteraction.applyTo(sheet).getSkillRollBonus();

        InteractionResult result = ataqueADistanciaInteraction.applyTo(sheet, null, null, attackTarget);

        assertEquals(baseline, result.getSkillRollBonus());
    }

    @Test
    void applyToGrantsNoAbatedoresDeGigantesBonusWithoutAnAttackTarget() {
        CharacterSheet sheet = sheetWithRace(new Anao());
        int baseline = ataqueADistanciaInteraction.applyTo(sheet).getSkillRollBonus();

        InteractionResult result = ataqueADistanciaInteraction.applyTo(sheet, null, null, null);

        assertEquals(baseline, result.getSkillRollBonus());
    }

    @Test
    void applyToAppliesTheAttackAndDamageSizeCategoryModifier() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(2).build())
                        .build())
                .sizeCategory(SizeCategory.MINUS_TWO)
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        InteractionResult result = ataqueADistanciaInteraction.applyTo(sheet);

        // 2 dexterity + untrained penalty + SizeCategory.MINUS_TWO's attack/damage modifier (-1).
        assertEquals(2 + Skill.UNTRAINED_PENALTY + SizeCategory.MINUS_TWO.getAttackAndDamageModifier(), result.getSkillRollBonus());
    }

    private static final Weapon ADAGA_DE_ARREMESSO = AbstractWeapon.builder()
            .name("Adaga de Arremesso")
            .category(ItemCategory.THROWABLE)
            .damageBase(DamageBase.of(1, 2))
            .skillType(SkillType.ATAQUE_A_DISTANCIA)
            .build();

    private static final Weapon ARCO_LONGO = AbstractWeapon.builder()
            .name("Arco Longo")
            .category(ItemCategory.BOW)
            .damageBase(DamageBase.of(2, 0))
            .skillType(SkillType.ATAQUE_A_DISTANCIA)
            .build();

    /**
     * Força 9 against Destreza 2, so which Attribute governed the roll is unmistakable in
     * skillRollBonus. The excess sits in variable rather than base, per CLAUDE.md's fixture rule
     * that only base is capped at 5.
     */
    private CharacterSheet arremessoPoderosoSheet(final boolean holdsTheAbility) {
        CharacterSkill ataqueADistanciaSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_A_DISTANCIA_1).build();
        ataqueADistanciaSkill.increaseGraduation(1);
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(5).variable(4).build())
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(2).build())
                        .build())
                .skill(SkillType.ATAQUE_A_DISTANCIA, ataqueADistanciaSkill);
        if (holdsTheAbility) {
            builder.skillCompetencyAbility(AtaqueADistanciaCompetencyAbility.ARREMESSO_PODEROSO);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    /** Destreza(2) + Graduação(1). */
    private static final int DEXTERITY_TOTAL = 3;

    /** Força(5+4) + Graduação(1). */
    private static final int STRENGTH_TOTAL = 10;

    private int bonusWith(final CharacterSheet sheet, final AttackSource attackSource) {
        return ataqueADistanciaInteraction.applyTo(sheet, null, null, null, attackSource).getSkillRollBonus();
    }

    @Test
    void arremessoPoderosoSubstitutesForcaWhenTheAttackIsThrown() {
        assertEquals(STRENGTH_TOTAL, bonusWith(arremessoPoderosoSheet(true), ADAGA_DE_ARREMESSO));
    }

    @Test
    void arremessoPoderosoSubstitutesForcaWhenTheAttackDeliversAMagia() {
        assertEquals(STRENGTH_TOTAL, bonusWith(arremessoPoderosoSheet(true), new TestSpell()));
    }

    @Test
    void arremessoPoderosoLeavesAFiredWeaponOnDestreza() {
        assertEquals(DEXTERITY_TOTAL, bonusWith(arremessoPoderosoSheet(true), ARCO_LONGO));
    }

    /**
     * The shorter overloads pass a {@code null} attackSource, so a plain roll by a holder is
     * unaffected — this is the "scoped, not unconditional" guarantee that separates
     * ARREMESSO_PODEROSO from DISPARO_ARCANO.
     */
    @Test
    void arremessoPoderosoDoesNotApplyToAPlainRoll() {
        CharacterSheet sheet = arremessoPoderosoSheet(true);

        assertEquals(DEXTERITY_TOTAL, ataqueADistanciaInteraction.applyTo(sheet).getSkillRollBonus());
        assertEquals(DEXTERITY_TOTAL, bonusWith(sheet, null));
    }

    /** The control: a thrown source alone substitutes nothing without the ability. */
    @Test
    void aThrownAttackSourceAloneSubstitutesNothing() {
        assertEquals(DEXTERITY_TOTAL, bonusWith(arremessoPoderosoSheet(false), ADAGA_DE_ARREMESSO));
    }
}
