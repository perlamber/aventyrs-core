package org.aventyrs.core.skill.profissao;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProfissaoCompetencyAbilityTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private static Character withProfissaoGraduation(final int graduation) {
        CharacterSkill profissao = CharacterSkillFixture.blank(CharacterSkillFixture.PROFISSAO_1).build();
        profissao.increaseGraduation(graduation);
        return CharacterFixture.blank(CharacterFixture.BLANK).skill(SkillType.PROFISSAO, profissao).build();
    }

    @Test
    void everyAbilityBelongsToProfissao() {
        for (ProfissaoCompetencyAbility ability : ProfissaoCompetencyAbility.values()) {
            assertEquals(SkillType.PROFISSAO, ability.getSkillType());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (ProfissaoCompetencyAbility ability : ProfissaoCompetencyAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, ProfissaoCompetencyAbility.values().length);
    }

    @Test
    void noAbilityReducesDifficultyOrGrantsASkillRollBonusYet() {
        ModifierResolver modifierResolver = new ModifierResolverImpl();
        for (ProfissaoCompetencyAbility ability : ProfissaoCompetencyAbility.values()) {
            assertEquals(0, ability.getDifficultyReduction());
            assertEquals(0, modifierResolver.sumModifiers(ability, ModifierType.SKILL_ROLL_BONUS));
        }
    }

    @Test
    void onlyConstrutorEficienteScalesProductionTime() {
        for (ProfissaoCompetencyAbility ability : ProfissaoCompetencyAbility.values()) {
            double expected = ability == ProfissaoCompetencyAbility.CONSTRUTOR_EFICIENTE ? 0.8 : 1.0;
            assertEquals(expected, ability.resolveProductionTimeMultiplier());
        }
    }

    @Test
    void onlyAumentarADurezaScalesProducedHardness() {
        for (ProfissaoCompetencyAbility ability : ProfissaoCompetencyAbility.values()) {
            double expected = ability == ProfissaoCompetencyAbility.AUMENTAR_A_DUREZA ? 1.5 : 1.0;
            assertEquals(expected, ability.resolveProducedHardnessMultiplier());
        }
    }

    @Test
    void reparoMelhoradoIsTheOnlyRepairBonusAndClimbsToFiveAtTenGraduacoes() {
        for (ProfissaoCompetencyAbility ability : ProfissaoCompetencyAbility.values()) {
            if (ability == ProfissaoCompetencyAbility.REPARO_MELHORADO) {
                continue;
            }
            assertEquals(0, ability.resolveRepairHardnessBonus(withProfissaoGraduation(10)));
        }
        assertEquals(2, ProfissaoCompetencyAbility.REPARO_MELHORADO.resolveRepairHardnessBonus(withProfissaoGraduation(9)));
        assertEquals(2, ProfissaoCompetencyAbility.REPARO_MELHORADO.resolveRepairHardnessBonus(null));
        assertEquals(5, ProfissaoCompetencyAbility.REPARO_MELHORADO.resolveRepairHardnessBonus(withProfissaoGraduation(10)));
    }
}
