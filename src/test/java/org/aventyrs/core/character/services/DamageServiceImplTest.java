package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.ability.VigorAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.ego.InitiativeAdvantage;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;
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

        int totalDamageTaken = damageService.applyDamage(character, sheet, 10, false);

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

        int totalDamageTaken = damageService.applyDamage(character, sheet, 10, false);

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
        assertEquals(2, damageService.getTotalAbsoluteDamageReduction(character, combatContext(1, false)));
    }

    @Test
    void getTotalAbsoluteDamageReductionWithSceneContextCombinesWithTheReflectionBasedSources() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .egoAdvantage(EgoDomain.INICIATIVA, InitiativeAdvantage.TORRE_EM_MOVIMENTO)
                .skillCompetencyAbility(new AbsoluteDamageReductionAbility())
                .build();
        assertEquals(4, damageService.getTotalAbsoluteDamageReduction(character, combatContext(1, false)));
    }

    @Test
    void getTotalAbsoluteDamageReductionWithSceneContextOmitsItOutsideTheFirstTwoRounds() {
        Character character = characterWithTorreEmMovimento();
        assertEquals(0, damageService.getTotalAbsoluteDamageReduction(character, combatContext(3, false)));
    }

    @Test
    void getTotalAbsoluteDamageReductionWithoutASceneContextOmitsTheEgoAdvantagesOwnContribution() {
        Character character = characterWithTorreEmMovimento();
        assertEquals(0, damageService.getTotalAbsoluteDamageReduction(character, null));
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

        int totalDamageTaken = damageService.applyDamage(character, sheet, combatContext(1, true), 10, false);

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

        assertEquals(1, damageService.getTotalDamageReduction(character, target, DamageType.FISICO, null));
    }

    @Test
    void getTotalDamageReductionWithDamageTypeGrantsTheEnhancedReductionAgainstASmallerAttacker() {
        Character character = characterWithRigidezDaMontanha(SizeCategory.ZERO);
        CharacterSheet target = CharacterSheet.of(character, new Player());
        Character attacker = CharacterFixture.blank(CharacterFixture.BLANK).sizeCategory(SizeCategory.MINUS_ONE).build();
        CharacterSheet source = CharacterSheet.of(attacker, new Player());

        assertEquals(2, damageService.getTotalDamageReduction(character, target, DamageType.FISICO, source));
    }

    @Test
    void getTotalDamageReductionWithDamageTypeOmitsItForNonFisicoDamage() {
        Character character = characterWithRigidezDaMontanha(SizeCategory.ZERO);
        CharacterSheet target = CharacterSheet.of(character, new Player());

        assertEquals(0, damageService.getTotalDamageReduction(character, target, DamageType.MAGICO, null));
    }

    @Test
    void getTotalDamageReductionWithDamageTypeCombinesWithTheReflectionBasedSources() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(VigorAbility.RIGIDEZ_DA_MONTANHA)
                .attributeAbility(new DamageReductionAbility())
                .sizeCategory(SizeCategory.ZERO)
                .build();
        CharacterSheet target = CharacterSheet.of(character, new Player());

        assertEquals(4, damageService.getTotalDamageReduction(character, target, DamageType.FISICO, null));
    }

    @Test
    void calculateFinalDamageWithDamageTypeAppliesTheAttributeAbilitysOwnReduction() {
        Character character = characterWithRigidezDaMontanha(SizeCategory.ZERO);
        CharacterSheet target = CharacterSheet.of(character, new Player());

        assertEquals(9, damageService.calculateFinalDamage(character, target, null, DamageType.FISICO, null, 10, false));
    }

    @Test
    void calculateFinalDamageWithNullTargetAndDamageTypeMatchesTheNoContextOverload() {
        Character character = characterWithRigidezDaMontanha(SizeCategory.ZERO);

        assertEquals(damageService.calculateFinalDamage(character, 10, false),
                damageService.calculateFinalDamage(character, null, null, null, null, 10, false));
    }

    @Test
    void applyDamageWithDamageTypeAppliesTheCalculatedAmountToTheCharacterSheet() {
        Character character = characterWithRigidezDaMontanha(SizeCategory.ZERO);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        int totalDamageTaken = damageService.applyDamage(character, sheet, null, DamageType.FISICO, null, 10, false);

        assertEquals(9, totalDamageTaken);
        assertEquals(9, sheet.getDamageTaken());
    }
}
