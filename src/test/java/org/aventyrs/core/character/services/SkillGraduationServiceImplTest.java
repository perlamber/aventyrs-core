package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.PeritoTeoricoAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.DlcRuleset;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.ataqueadistancia.AtaqueADistanciaCompetencyAbility;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoCompetencyAbility;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkillGraduationServiceImplTest {

    private final SkillGraduationService skillGraduationService = new SkillGraduationServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private Character characterWithForcaBase(final int forcaBase, final int graduationValue) {
        CharacterSkill ataqueCorpoACorpoSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        ataqueCorpoACorpoSkill.increaseGraduation(graduationValue);
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(forcaBase).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, ataqueCorpoACorpoSkill)
                .build();
    }

    private CharacterSheet sheetWithExperience(final Character character, final BigDecimal experience) {
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(experience);
        return sheet;
    }

    @Test
    void getMaxGraduationIsTwiceTheGoverningAttributesBase() {
        Character character = characterWithForcaBase(3, 0);

        assertEquals(6, skillGraduationService.getMaxGraduation(character, SkillType.ATAQUE_CORPO_A_CORPO));
    }

    @Test
    void getMaxGraduationIgnoresRacialBonusAndVariableOnlyBaseCounts() {
        CharacterSkill ataqueCorpoACorpoSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(2).racialBonus(10).variable(10).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, ataqueCorpoACorpoSkill)
                .build();

        assertEquals(4, skillGraduationService.getMaxGraduation(character, SkillType.ATAQUE_CORPO_A_CORPO));
    }

    @Test
    void getMaxGraduationUsesTheSubstitutedAttributeWhenPresent() {
        CharacterSkill ataqueCorpoACorpoSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(3).build())
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(5).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, ataqueCorpoACorpoSkill)
                .skillCompetencyAbility(AtaqueCorpoACorpoCompetencyAbility.ACUIDADE)
                .build();

        // ACUIDADE substitutes Destreza(5) for Força(3), so the cap is 2*5=10, not 2*3=6.
        assertEquals(10, skillGraduationService.getMaxGraduation(character, SkillType.ATAQUE_CORPO_A_CORPO));
    }

    /**
     * The deliberate counterpart to the test above. A <em>delivery-scoped</em> substitution
     * doesn't widen the cap: the cap asks which Attribute currently <em>governs</em> the Perícia,
     * and ARREMESSO_PODEROSO governs only the rolls made with a thrown weapon or a Magia. This is
     * why {@code SkillGraduationServiceImpl} keeps calling the 3-arg {@code
     * resolveAttributeDomain}, which passes no {@code AttackSource}.
     */
    @Test
    void getMaxGraduationIgnoresADeliveryScopedSubstitution() {
        CharacterSkill ataqueADistanciaSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_A_DISTANCIA_1).build();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(5).build())
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(3).build())
                        .build())
                .skill(SkillType.ATAQUE_A_DISTANCIA, ataqueADistanciaSkill)
                .skillCompetencyAbility(AtaqueADistanciaCompetencyAbility.ARREMESSO_PODEROSO)
                .build();

        // Destreza(3) governs, so the cap stays 2*3=6 — not the 2*5=10 Força would give.
        assertEquals(6, skillGraduationService.getMaxGraduation(character, SkillType.ATAQUE_A_DISTANCIA));
    }

    private static class SubstitutingRacialAbility implements SkillCompetencyAbility {
        @Override
        public SkillType getSkillType() {
            return SkillType.ATAQUE_CORPO_A_CORPO;
        }

        @Override
        public String getDescription() {
            return "Test-only racial substitution of Destreza for Força on Ataque Corpo-a-Corpo.";
        }

        @Override
        public Optional<AttributeDomain> getSubstituteAttributeDomain() {
            return Optional.of(AttributeDomain.DEXTERITY);
        }
    }

    private static class RaceWithSubstitutingRacialAbility implements Race {
        @Override
        public CreatureType getCreatureType() {
            return CreatureType.HUMANOIDE;
        }

        @Override
        public List<SkillCompetencyAbility> getRacialAbilities() {
            return List.of(new SubstitutingRacialAbility());
        }

        @Override
        public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
            throw new UnsupportedOperationException("not needed by this test");
        }
    }

    @Test
    void getMaxGraduationUsesTheSubstitutedAttributeFromARacialAbilityToo() {
        CharacterSkill ataqueCorpoACorpoSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(3).build())
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(5).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, ataqueCorpoACorpoSkill)
                .race(new RaceWithSubstitutingRacialAbility())
                .build();

        // The racial ability substitutes Destreza(5) for Força(3), so the cap is 2*5=10, not 2*3=6.
        assertEquals(10, skillGraduationService.getMaxGraduation(character, SkillType.ATAQUE_CORPO_A_CORPO));
    }

    @Test
    void getMaxGraduationUsesTheSubstitutedAttributeFromPeritoTeoricoWhenPresent() {
        CharacterSkill furtividadeSkill = CharacterSkillFixture.blank(CharacterSkillFixture.FURTIVIDADE_1).build();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(2).build())
                        .gnose(AttributeValue.builder().domain(AttributeDomain.GNOSE).base(5).build())
                        .build())
                .skill(SkillType.FURTIVIDADE, furtividadeSkill)
                .attributeAbility(PeritoTeoricoAbility.FURTIVIDADE)
                .build();

        // PeritoTeoricoAbility.FURTIVIDADE substitutes Gnose(5) for Destreza(2), so the cap is 2*5=10, not 2*2=4.
        assertEquals(10, skillGraduationService.getMaxGraduation(character, SkillType.FURTIVIDADE));
    }

    @Test
    void getMaxGraduationIsUnaffectedByPeritoTeoricoForAnUnchosenSkillType() {
        CharacterSkill furtividadeSkill = CharacterSkillFixture.blank(CharacterSkillFixture.FURTIVIDADE_1).build();
        CharacterSkill persuasaoSkill = CharacterSkillFixture.blank(CharacterSkillFixture.PERSUASAO_1).build();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .gnose(AttributeValue.builder().domain(AttributeDomain.GNOSE).base(5).build())
                        .charisma(AttributeValue.builder().domain(AttributeDomain.CHARISMA).base(2).build())
                        .build())
                .skill(SkillType.FURTIVIDADE, furtividadeSkill)
                .skill(SkillType.PERSUASAO, persuasaoSkill)
                .attributeAbility(PeritoTeoricoAbility.FURTIVIDADE)
                .build();

        // The character only holds FURTIVIDADE's constant, so Persuasão's cap still uses Carisma(2)*2=4, not Gnose(5)*2=10.
        assertEquals(4, skillGraduationService.getMaxGraduation(character, SkillType.PERSUASAO));
    }

    @Test
    void upgradeCostIsHalfTheTargetGraduation() {
        Character character = characterWithForcaBase(10, 6);
        CharacterSkill ataqueCorpoACorpoSkill = character.getSkills().get(SkillType.ATAQUE_CORPO_A_CORPO);

        // 6 -> 7: cost is target(7)/2 = 3.5.
        assertEquals(BigDecimal.valueOf(3.5), skillGraduationService.getUpgradeCost(ataqueCorpoACorpoSkill));
    }

    @Test
    void upgradeGraduationIncreasesGraduationByOneWithinTheCap() throws IllegalOperationException {
        Character character = characterWithForcaBase(3, 4);
        CharacterSheet sheet = sheetWithExperience(character, BigDecimal.TEN);

        CharacterSkill upgraded = skillGraduationService.upgradeGraduation(character, sheet, SkillType.ATAQUE_CORPO_A_CORPO);

        assertEquals(5, upgraded.getGraduation().getGraduationValue());
    }

    @Test
    void upgradeGraduationSpendsHalfTheTargetGraduationAsExperience() throws IllegalOperationException {
        Character character = characterWithForcaBase(3, 4);
        CharacterSheet sheet = sheetWithExperience(character, BigDecimal.TEN);

        skillGraduationService.upgradeGraduation(character, sheet, SkillType.ATAQUE_CORPO_A_CORPO);

        // 4 -> 5: cost is target(5)/2 = 2.5; 10 - 2.5 = 7.5.
        assertEquals(BigDecimal.valueOf(7.5), sheet.getUnUsedExperience());
    }

    @Test
    void upgradeGraduationRejectsExceedingTheCapAndDoesNotSpendAnyway() {
        Character character = characterWithForcaBase(3, 6);
        CharacterSheet sheet = sheetWithExperience(character, BigDecimal.TEN);
        CharacterSkill ataqueCorpoACorpoSkill = character.getSkills().get(SkillType.ATAQUE_CORPO_A_CORPO);

        assertThrows(IllegalOperationException.class,
                () -> skillGraduationService.upgradeGraduation(character, sheet, SkillType.ATAQUE_CORPO_A_CORPO));
        assertEquals(6, ataqueCorpoACorpoSkill.getGraduation().getGraduationValue());
        assertEquals(BigDecimal.TEN, sheet.getUnUsedExperience());
    }

    @Test
    void upgradeGraduationRejectsWhenNotEnoughExperienceAndDoesNotChangeGraduation() {
        Character character = characterWithForcaBase(3, 4);
        CharacterSheet sheet = sheetWithExperience(character, BigDecimal.valueOf(2));
        CharacterSkill ataqueCorpoACorpoSkill = character.getSkills().get(SkillType.ATAQUE_CORPO_A_CORPO);

        // 4 -> 5 costs 2.5, but the sheet only has 2.
        assertThrows(IllegalOperationException.class,
                () -> skillGraduationService.upgradeGraduation(character, sheet, SkillType.ATAQUE_CORPO_A_CORPO));
        assertEquals(4, ataqueCorpoACorpoSkill.getGraduation().getGraduationValue());
        assertEquals(BigDecimal.valueOf(2), sheet.getUnUsedExperience());
    }
}
