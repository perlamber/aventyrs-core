package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.ability.VigorAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.item.ArmorItem;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.ego.InitiativeAdvantage;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.title.santo.Santo;
import org.aventyrs.core.title.santo.SantoAbility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DamageServiceImplTest {

    private final DamageService damageService = new DamageServiceImpl();

    private static class DamageReductionAbility implements AttributeAbility {
        @Override
        public AttributeDomain getAttributeDomain() {
            return AttributeDomain.VIGOR;
        }

        @Override
        public String getDescription() {
            return "Test-only +3 RD source.";
        }

        @Modifier(ModifierType.DAMAGE_REDUCTION)
        public int bonus() {
            return 3;
        }
    }

    private static class AbsoluteDamageReductionAbility implements SkillCompetencyAbility {
        @Override
        public SkillType getSkillType() {
            return SkillType.ATLETISMO;
        }

        @Override
        public String getDescription() {
            return "Test-only +2 RA source.";
        }

        @Modifier(ModifierType.ABSOLUTE_DAMAGE_REDUCTION)
        public int bonus() {
            return 2;
        }
    }


    private static class HalfDamageAbility implements SkillCompetencyAbility {
        @Override
        public SkillType getSkillType() {
            return SkillType.ATLETISMO;
        }

        @Override
        public String getDescription() {
            return "Test-only +2 RA source.";
        }

        @Modifier(ModifierType.HALF_DAMAGE)
        public int bonus() {
            return 2;
        }
    }

    private static class DamageReductionMalusAbility implements SkillCompetencyAbility {
        @Override
        public SkillType getSkillType() {
            return SkillType.ATLETISMO;
        }

        @Override
        public String getDescription() {
            return "Test-only -10 RD malus source.";
        }

        @Modifier(ModifierType.DAMAGE_REDUCTION)
        public int malus() {
            return -10;
        }
    }

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    @Test
    void defaultTotalDamageReductionIsZero() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        assertEquals(0, damageService.getTotalDamageReduction(character));
    }

    @Test
    void defaultTotalAbsoluteDamageReductionIsZero() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        assertEquals(0, damageService.getTotalAbsoluteDamageReduction(character));
    }

    @Test
    void attributeAbilityModifierIsAddedToDamageReduction() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(new DamageReductionAbility())
                .build();
        assertEquals(3, damageService.getTotalDamageReduction(character));
    }

    @Test
    void skillCompetencyAbilityModifierIsAddedToAbsoluteDamageReduction() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skillCompetencyAbility(new AbsoluteDamageReductionAbility())
                .build();
        assertEquals(2, damageService.getTotalAbsoluteDamageReduction(character));
    }

    @Test
    void trainedSkillWhoseExcellencyGrantsAnUnrelatedModifierContributesNothing() {
        CharacterSkill atletismoSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATLETISMO_1).build();
        atletismoSkill.increaseGraduation(10);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.ATLETISMO, atletismoSkill)
                .build();

        // AtletismoExcellency.LENDA grants ACTION_POINTS, not DAMAGE_REDUCTION/
        // ABSOLUTE_DAMAGE_REDUCTION, so a fully-unlocked Atletismo contributes nothing here —
        // this just proves the excellency-scanning loop runs without blowing up on a trained
        // skill that happens to unlock unrelated modifiers. No SkillExcellency grants RD/RA
        // for real yet, so this can't be exercised with a nonzero example today.
        assertEquals(0, damageService.getTotalDamageReduction(character));
        assertEquals(0, damageService.getTotalAbsoluteDamageReduction(character));
    }

    @Test
    void totalDamageReductionNeverGoesBelowZero() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skillCompetencyAbility(new DamageReductionMalusAbility())
                .build();
        assertEquals(0, damageService.getTotalDamageReduction(character));
    }

    @Test
    void calculateFinalDamageSubtractsRdAndRa() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(new DamageReductionAbility())
                .skillCompetencyAbility(new AbsoluteDamageReductionAbility())
                .build();

        assertEquals(5, damageService.calculateFinalDamage(character, 10, false));
    }

    @Test
    void calculateFinalDamageIgnoringDamageReductionSkipsRdButNotRa() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(new DamageReductionAbility())
                .skillCompetencyAbility(new AbsoluteDamageReductionAbility())
                .build();

        assertEquals(8, damageService.calculateFinalDamage(character, 10, true));
    }

    @Test
    void calculateFinalDamageAppliesHalfDamageLastAndRoundsDown() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(new DamageReductionAbility())
                .skillCompetencyAbility(new HalfDamageAbility())
                .build();

        assertEquals(4, damageService.calculateFinalDamage(character, 12, false));
    }

    @Test
    void calculateFinalDamageNeverGoesBelowZero() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(new DamageReductionAbility())
                .skillCompetencyAbility(new AbsoluteDamageReductionAbility())
                .build();

        assertEquals(0, damageService.calculateFinalDamage(character, 2, false));
    }

    @Test
    void applyDamageAppliesTheCalculatedAmountToTheCharacterSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(new DamageReductionAbility())
                .skillCompetencyAbility(new AbsoluteDamageReductionAbility())
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        int totalDamageTaken = damageService.applyDamage(sheet, 10, false);

        assertEquals(5, totalDamageTaken);
        assertEquals(5, sheet.getDamageTaken());
    }
    @Test
    void applyDamageStillAbsorbsShieldPointsFirst() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(new DamageReductionAbility())
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.addShield(4);

        int totalDamageTaken = damageService.applyDamage(sheet, 10, false);

        assertEquals(3, totalDamageTaken);
        assertEquals(3, sheet.getDamageTaken());
    }

    private SceneContext combatContext(final int currentRound, final boolean wonInitiative) {
        return new SceneContext(List.of(), List.of(), Map.of(), null, true, currentRound, wonInitiative);
    }

    private Character characterWithTorreEmMovimento() {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .egoAdvantage(EgoDomain.INICIATIVA, InitiativeAdvantage.TORRE_EM_MOVIMENTO)
                .build();
    }

    @Test
    void getTotalAbsoluteDamageReductionWithSceneContextAddsTheEgoAdvantagesOwnContribution() {
        Character character = characterWithTorreEmMovimento();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        assertEquals(2, damageService.getTotalAbsoluteDamageReduction(sheet, combatContext(1, false)));
    }

    @Test
    void getTotalAbsoluteDamageReductionWithSceneContextCombinesWithTheReflectionBasedSources() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .egoAdvantage(EgoDomain.INICIATIVA, InitiativeAdvantage.TORRE_EM_MOVIMENTO)
                .skillCompetencyAbility(new AbsoluteDamageReductionAbility())
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        assertEquals(4, damageService.getTotalAbsoluteDamageReduction(sheet, combatContext(1, false)));
    }

    @Test
    void getTotalAbsoluteDamageReductionWithSceneContextOmitsItOutsideTheFirstTwoRounds() {
        Character character = characterWithTorreEmMovimento();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        assertEquals(0, damageService.getTotalAbsoluteDamageReduction(sheet, combatContext(3, false)));
    }

    @Test
    void getTotalAbsoluteDamageReductionWithoutASceneContextOmitsTheEgoAdvantagesOwnContribution() {
        Character character = characterWithTorreEmMovimento();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        assertEquals(0, damageService.getTotalAbsoluteDamageReduction(sheet, null));
    }

    @Test
    void calculateFinalDamageWithSceneContextAppliesTheEgoAdvantagesOwnAbsoluteDamageReduction() {
        Character character = characterWithTorreEmMovimento();
        assertEquals(8, damageService.calculateFinalDamage(character, combatContext(1, false), 10, false));
    }

    @Test
    void calculateFinalDamageWithSceneContextAlsoHalvesDamageWhenInitiativeWasWon() {
        Character character = characterWithTorreEmMovimento();
        // RA(2) subtracted from 10 leaves 8, then halved (initiative won) to 4.
        assertEquals(4, damageService.calculateFinalDamage(character, combatContext(1, true), 10, false));
    }

    @Test
    void calculateFinalDamageWithSceneContextDoesNotHalveDamageWhenInitiativeWasNotWon() {
        Character character = characterWithTorreEmMovimento();
        assertEquals(8, damageService.calculateFinalDamage(character, combatContext(1, false), 10, false));
    }

    @Test
    void calculateFinalDamageWithNullSceneContextMatchesTheNoContextOverload() {
        Character character = characterWithTorreEmMovimento();
        assertEquals(damageService.calculateFinalDamage(character, 10, false),
                damageService.calculateFinalDamage(character, null, 10, false));
    }

    @Test
    void applyDamageWithSceneContextAppliesTheCalculatedAmountToTheCharacterSheet() {
        Character character = characterWithTorreEmMovimento();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        int totalDamageTaken = damageService.applyDamage(sheet, combatContext(1, true), 10, false);

        assertEquals(4, totalDamageTaken);
        assertEquals(4, sheet.getDamageTaken());
    }

    private Character characterWithRigidezDaMontanha(final SizeCategory sizeCategory) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(VigorAbility.RIGIDEZ_DA_MONTANHA)
                .sizeCategory(sizeCategory)
                .build();
    }

    @Test
    void getTotalDamageReductionWithDamageTypeAddsTheAttributeAbilitysOwnContribution() {
        Character character = characterWithRigidezDaMontanha(SizeCategory.ZERO);
        CharacterSheet target = CharacterSheet.of(character, new Player());

        assertEquals(1, damageService.getTotalDamageReduction(target, DamageType.FISICO, null));
    }

    @Test
    void getTotalDamageReductionWithDamageTypeGrantsTheEnhancedReductionAgainstASmallerAttacker() {
        Character character = characterWithRigidezDaMontanha(SizeCategory.ZERO);
        CharacterSheet target = CharacterSheet.of(character, new Player());
        Character attacker = CharacterFixture.blank(CharacterFixture.BLANK).sizeCategory(SizeCategory.MINUS_ONE).build();
        CharacterSheet source = CharacterSheet.of(attacker, new Player());

        assertEquals(2, damageService.getTotalDamageReduction(target, DamageType.FISICO, source));
    }

    @Test
    void getTotalDamageReductionWithDamageTypeOmitsItForNonFisicoDamage() {
        Character character = characterWithRigidezDaMontanha(SizeCategory.ZERO);
        CharacterSheet target = CharacterSheet.of(character, new Player());

        assertEquals(0, damageService.getTotalDamageReduction(target, DamageType.MAGICO, null));
    }

    @Test
    void getTotalDamageReductionWithDamageTypeCombinesWithTheReflectionBasedSources() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(VigorAbility.RIGIDEZ_DA_MONTANHA)
                .attributeAbility(new DamageReductionAbility())
                .sizeCategory(SizeCategory.ZERO)
                .build();
        CharacterSheet target = CharacterSheet.of(character, new Player());

        assertEquals(4, damageService.getTotalDamageReduction(target, DamageType.FISICO, null));
    }

    @Test
    void calculateFinalDamageWithDamageTypeAppliesTheAttributeAbilitysOwnReduction() {
        Character character = characterWithRigidezDaMontanha(SizeCategory.ZERO);
        CharacterSheet target = CharacterSheet.of(character, new Player());

        assertEquals(9, damageService.calculateFinalDamage(target, null, DamageType.FISICO, null, 10, false));
    }

    @Test
    void applyDamageWithDamageTypeAppliesTheCalculatedAmountToTheCharacterSheet() {
        Character character = characterWithRigidezDaMontanha(SizeCategory.ZERO);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        int totalDamageTaken = damageService.applyDamage(sheet, null, DamageType.FISICO, null, 10, false);

        assertEquals(9, totalDamageTaken);
        assertEquals(9, sheet.getDamageTaken());
    }

    private Character characterWithBastiaoDosNecessitados() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        character.grantTitle(new Santo(List.of(), List.of(SantoAbility.BASTIAO_DOS_NECESSITADOS)), TitleSlot.PRIMARY);
        return character;
    }

    private CharacterSheet allySheetWithDamageTaken(final int damageTaken) {
        Character allyCharacter = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet allySheet = CharacterSheet.of(allyCharacter, new Player());
        if (damageTaken > 0) {
            allySheet.applyDamage(damageTaken);
        }
        return allySheet;
    }

    @Test
    void selfFacingAbsoluteDamageReductionAppliesWhenAnAdjacentAllyHasLowerCurrentHitPoints() {
        Character holder = characterWithBastiaoDosNecessitados();
        CharacterSheet holderSheet = CharacterSheet.of(holder, new Player());
        CharacterSheet allySheet = allySheetWithDamageTaken(5);
        SceneContext sceneContext = new SceneContext(List.of(allySheet), List.of(), Map.of(allySheet, Range.ADJACENTE));

        assertEquals(DamageService.DEFAULT_DAMAGE_REDUCTION, damageService.getTotalAbsoluteDamageReduction(holderSheet, sceneContext));
    }

    @Test
    void selfFacingAbsoluteDamageReductionDoesNotApplyWhenTheAdjacentAllyHasMoreHitPoints() {
        Character holder = characterWithBastiaoDosNecessitados();
        CharacterSheet holderSheet = CharacterSheet.of(holder, new Player());
        holderSheet.applyDamage(3);
        CharacterSheet allySheet = allySheetWithDamageTaken(0);
        SceneContext sceneContext = new SceneContext(List.of(allySheet), List.of(), Map.of(allySheet, Range.ADJACENTE));

        assertEquals(0, damageService.getTotalAbsoluteDamageReduction(holderSheet, sceneContext));
    }

    @Test
    void selfFacingAbsoluteDamageReductionDoesNotApplyWhenTheLowerPvAllyIsNotAdjacent() {
        Character holder = characterWithBastiaoDosNecessitados();
        CharacterSheet holderSheet = CharacterSheet.of(holder, new Player());
        CharacterSheet allySheet = allySheetWithDamageTaken(5);
        SceneContext sceneContext = new SceneContext(List.of(allySheet), List.of(), Map.of(allySheet, Range.DISTANCIA_CURTA));

        assertEquals(0, damageService.getTotalAbsoluteDamageReduction(holderSheet, sceneContext));
    }

    @Test
    void calculateFinalDamageWithoutATargetDoesNotApplyBastiaoDosNecessitados() {
        Character holder = characterWithBastiaoDosNecessitados();
        CharacterSheet allySheet = allySheetWithDamageTaken(5);
        SceneContext sceneContext = new SceneContext(List.of(allySheet), List.of(), Map.of(allySheet, Range.ADJACENTE));

        // The sheet-less calculateFinalDamage overload has no CharacterSheet in hand — Bastião's
        // own PV comparison can't be resolved at all, so its RA doesn't reduce the raw damage.
        assertEquals(10, damageService.calculateFinalDamage(holder, sceneContext, 10, false));
    }

    @Test
    void selfFacingAbsoluteDamageReductionDoesNotApplyWithoutASceneContext() {
        Character holder = characterWithBastiaoDosNecessitados();
        CharacterSheet holderSheet = CharacterSheet.of(holder, new Player());

        assertEquals(0, damageService.getTotalAbsoluteDamageReduction(holderSheet, null));
    }

    @Test
    void calculateFinalDamageAppliesBastiaoDosNecessitadosSelfFacingAbsoluteDamageReduction() {
        Character holder = characterWithBastiaoDosNecessitados();
        CharacterSheet holderSheet = CharacterSheet.of(holder, new Player());
        CharacterSheet allySheet = allySheetWithDamageTaken(5);
        SceneContext sceneContext = new SceneContext(List.of(allySheet), List.of(), Map.of(allySheet, Range.ADJACENTE));

        int finalDamage = damageService.calculateFinalDamage(holderSheet, sceneContext, null, null, 10, false);

        assertEquals(10 - DamageService.DEFAULT_DAMAGE_REDUCTION, finalDamage);
    }

    @Test
    void bastiaoDosNecessitadosCombinesAdditivelyWithTorreEmMovimento() {
        Character holder = CharacterFixture.blank(CharacterFixture.BLANK)
                .egoAdvantage(EgoDomain.INICIATIVA, InitiativeAdvantage.TORRE_EM_MOVIMENTO)
                .build();
        holder.grantTitle(new Santo(List.of(), List.of(SantoAbility.BASTIAO_DOS_NECESSITADOS)), TitleSlot.PRIMARY);
        CharacterSheet holderSheet = CharacterSheet.of(holder, new Player());
        CharacterSheet allySheet = allySheetWithDamageTaken(5);
        SceneContext sceneContext = new SceneContext(List.of(allySheet), List.of(), Map.of(allySheet, Range.ADJACENTE),
                null, true, 1, false);

        assertEquals(DamageService.DEFAULT_DAMAGE_REDUCTION * 2,
                damageService.getTotalAbsoluteDamageReduction(holderSheet, sceneContext));
    }

    // ---------------------------------------------------------------------------------------
    // Equipped items as the fourth RD source.
    // ---------------------------------------------------------------------------------------

    @Test
    void anEquippedArmorsFavorContributesDamageReductionOnceItsRequisitosAreMet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(3).build())
                        .build())
                .equipment(List.of(ArmorItem.ARMADURA_COMPLETA))
                .build();

        assertEquals(2, damageService.getTotalDamageReduction(character));
        assertEquals(10 - 2, damageService.calculateFinalDamage(character, 10, false));
    }

    @Test
    void anEquippedArmorsFavorContributesNothingWhenItsRequisitosArentMet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(1).build())
                        .build())
                .equipment(List.of(ArmorItem.ARMADURA_COMPLETA))
                .build();

        assertEquals(0, damageService.getTotalDamageReduction(character));
    }

    @Test
    void anEquippedArmorsDamageReductionIsIgnoredByAnIgnoreDamageReductionAttack() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(3).build())
                        .build())
                .equipment(List.of(ArmorItem.ARMADURA_COMPLETA))
                .build();

        assertEquals(10, damageService.calculateFinalDamage(character, 10, true));
    }
}
