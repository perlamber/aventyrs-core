package org.aventyrs.core.skill;

import org.aventyrs.core.modifier.ModifierType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillTypeTest {

    @Test
    void onlyTheTwoPericiasDeAtaqueAreAttackSkills() {
        for (SkillType skillType : SkillType.values()) {
            if (skillType == SkillType.ATAQUE_A_DISTANCIA || skillType == SkillType.ATAQUE_CORPO_A_CORPO) {
                assertTrue(skillType.isAttackSkill());
            } else {
                assertFalse(skillType.isAttackSkill());
            }
        }
    }

    @Test
    void everySkillTypeHasItsOwnDistinctRollBonusModifierType() {
        Set<ModifierType> seen = new HashSet<>();
        for (SkillType skillType : SkillType.values()) {
            assertNotNull(skillType.getRollBonusType());
            assertTrue(seen.add(skillType.getRollBonusType()),
                    "Duplicate rollBonusType for " + skillType);
        }
    }

    @Test
    void everySkillTypeHasAFreshSkillInstance() {
        for (SkillType skillType : SkillType.values()) {
            assertNotNull(skillType.newSkillInstance());
        }
    }

    @Test
    void everySkillTypeHasAllThreeConstructorArgumentsDistinctFromEveryOtherSkillType() {
        long distinctExcellencyClasses = Arrays.stream(SkillType.values())
                .map(SkillType::getExcellencyClass)
                .distinct()
                .count();
        assertEquals(SkillType.values().length, distinctExcellencyClasses);
    }
}
