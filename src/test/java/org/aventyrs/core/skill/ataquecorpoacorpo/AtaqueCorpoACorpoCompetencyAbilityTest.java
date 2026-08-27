package org.aventyrs.core.skill.ataquecorpoacorpo;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtaqueCorpoACorpoCompetencyAbilityTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    @Test
    void everyAbilityBelongsToAtaqueCorpoACorpo() {
        for (AtaqueCorpoACorpoCompetencyAbility ability : AtaqueCorpoACorpoCompetencyAbility.values()) {
            assertEquals(SkillType.ATAQUE_CORPO_A_CORPO, ability.getSkillType());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (AtaqueCorpoACorpoCompetencyAbility ability : AtaqueCorpoACorpoCompetencyAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, AtaqueCorpoACorpoCompetencyAbility.values().length);
    }

    @Test
    void noAbilityReducesDifficultyOrGrantsASkillRollBonusYet() {
        ModifierResolver modifierResolver = new ModifierResolverImpl();
        for (AtaqueCorpoACorpoCompetencyAbility ability : AtaqueCorpoACorpoCompetencyAbility.values()) {
            assertEquals(0, ability.getDifficultyReduction());
            assertEquals(0, modifierResolver.sumModifiers(ability, ModifierType.SKILL_ROLL_BONUS));
        }
    }

    private Character meleeCharacterAt(int graduation) {
        CharacterSkill skill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        skill.increaseGraduation(graduation);
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, skill)
                .build();
    }

    private Optional<DamageBonus> brutalidadeDamageBonusAt(int graduation) {
        return AtaqueCorpoACorpoCompetencyAbility.BRUTALIDADE.resolveDamageBonus(
                SkillType.ATAQUE_CORPO_A_CORPO, null, null, meleeCharacterAt(graduation));
    }

    private int brutalidadeDamageBaseIncreaseAt(int graduation) {
        return AtaqueCorpoACorpoCompetencyAbility.BRUTALIDADE.resolveDamageBaseIncrease(
                SkillType.ATAQUE_CORPO_A_CORPO, meleeCharacterAt(graduation));
    }

    @Test
    void brutalidadeGrantsAFlatDamageBonusBelowFiveGraduacoes() {
        for (int graduation = 0; graduation < 5; graduation++) {
            Optional<DamageBonus> bonus = brutalidadeDamageBonusAt(graduation);
            assertTrue(bonus.isPresent());
            assertEquals(1, bonus.get().getValue());
            assertEquals(DamageType.FISICO, bonus.get().getType());
            assertEquals(0, brutalidadeDamageBaseIncreaseAt(graduation));
        }
    }

    /** "Convertido" is exclusive: the flat bonus stops the moment the Dano Base increase starts. */
    @Test
    void brutalidadeConvertsIntoOneDamageBaseAtFiveGraduacoes() {
        for (int graduation = 5; graduation < 10; graduation++) {
            assertEquals(Optional.empty(), brutalidadeDamageBonusAt(graduation));
            assertEquals(1, brutalidadeDamageBaseIncreaseAt(graduation));
        }
    }

    @Test
    void brutalidadeDamageBaseIncreaseBecomesTwoAtTenGraduacoes() {
        assertEquals(Optional.empty(), brutalidadeDamageBonusAt(10));
        assertEquals(2, brutalidadeDamageBaseIncreaseAt(10));
        assertEquals(2, brutalidadeDamageBaseIncreaseAt(14));
    }

    @Test
    void brutalidadeGrantsNothingToARangedAttack() {
        Character character = meleeCharacterAt(10);

        assertEquals(0, AtaqueCorpoACorpoCompetencyAbility.BRUTALIDADE
                .resolveDamageBaseIncrease(SkillType.ATAQUE_A_DISTANCIA, character));
        assertEquals(Optional.empty(), AtaqueCorpoACorpoCompetencyAbility.BRUTALIDADE
                .resolveDamageBonus(SkillType.ATAQUE_A_DISTANCIA, null, null, meleeCharacterAt(2)));
    }

    /**
     * Reached through the 2-arg overload there is no actor to read a Graduação from, so which
     * tier applies is unknowable — "condition not met", never a guessed default.
     */
    @Test
    void brutalidadeGrantsNothingWithoutAnActor() {
        assertEquals(Optional.empty(),
                AtaqueCorpoACorpoCompetencyAbility.BRUTALIDADE.resolveDamageBonus(null, null));
    }

    @Test
    void onlyBrutalidadeEverResolvesADamageBaseIncrease() {
        Character character = meleeCharacterAt(10);
        for (AtaqueCorpoACorpoCompetencyAbility ability : AtaqueCorpoACorpoCompetencyAbility.values()) {
            if (ability != AtaqueCorpoACorpoCompetencyAbility.BRUTALIDADE) {
                assertEquals(0, ability.resolveDamageBaseIncrease(SkillType.ATAQUE_CORPO_A_CORPO, character));
            }
        }
    }

    @Test
    void onlyAcuidadeAndSagacidadeArcanaSubstituteTheBaseAttribute() {
        Map<AtaqueCorpoACorpoCompetencyAbility, AttributeDomain> substitutions = Map.of(
                AtaqueCorpoACorpoCompetencyAbility.ACUIDADE, AttributeDomain.DEXTERITY,
                AtaqueCorpoACorpoCompetencyAbility.SAGACIDADE_ARCANA, AttributeDomain.FOCUS);
        for (AtaqueCorpoACorpoCompetencyAbility ability : AtaqueCorpoACorpoCompetencyAbility.values()) {
            assertEquals(Optional.ofNullable(substitutions.get(ability)), ability.getSubstituteAttributeDomain());
        }
    }
}
