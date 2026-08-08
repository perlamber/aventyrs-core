package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
