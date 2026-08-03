package org.aventyrs.core.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
