package org.aventyrs.core.ability;

import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AcquiredChoiceTest {

    @Test
    void ofStoresTheAbilityAndTheChosenValue() {
        AcquiredChoice<SkillType> choice = AcquiredChoice.of(GnoseAbility.PERITO_TEORICO, SkillType.ATLETISMO);

        assertSame(GnoseAbility.PERITO_TEORICO, choice.getAbility());
        assertEquals(SkillType.ATLETISMO, choice.getValue());
    }
}
