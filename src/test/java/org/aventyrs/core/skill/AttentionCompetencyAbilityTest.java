package org.aventyrs.core.skill;

import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AttentionCompetencyAbilityTest {

    @Test
    void everyAbilityBelongsToAttention() {
        for (AttentionCompetencyAbility ability : AttentionCompetencyAbility.values()) {
            assertEquals(SkillType.ATTENTION, ability.getSkillType());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (AttentionCompetencyAbility ability : AttentionCompetencyAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, AttentionCompetencyAbility.values().length);
    }

    @Test
    void onlyPercepcaoDeFoxmGrantsASkillRollBonus() {
        ModifierResolver modifierResolver = new ModifierResolverImpl();
        for (AttentionCompetencyAbility ability : AttentionCompetencyAbility.values()) {
            int expected = ability == AttentionCompetencyAbility.PERCEPCAO_DE_FOXM ? Skill.ADVANTAGE_BONUS : 0;
            assertEquals(expected, modifierResolver.sumModifiers(ability, ModifierType.SKILL_ROLL_BONUS));
        }
    }
}
