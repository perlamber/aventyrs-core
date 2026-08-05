package org.aventyrs.core.skill;

import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.artes.ArtesInteraction;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void everySkillTypeHasAFreshInteractionInstance() {
        for (SkillType skillType : SkillType.values()) {
            assertNotNull(skillType.newInteraction());
        }
    }

    @Test
    void newInteractionReturnsADistinctInstanceEachCall() {
        AbstractSkillInteraction first = SkillType.ARTES.newInteraction();
        AbstractSkillInteraction second = SkillType.ARTES.newInteraction();

        assertNotSame(first, second);
    }

    @Test
    void artesSkillTypeReturnsAnArtesInteraction() {
        assertTrue(SkillType.ARTES.newInteraction() instanceof ArtesInteraction);
    }

    @Test
    void everySkillTypeHasAllThreeConstructorArgumentsDistinctFromEveryOtherSkillType() {
        long distinctExcellencyClasses = Arrays.stream(SkillType.values())
                .map(SkillType::getExcellencyClass)
                .distinct()
                .count();
        assertEquals(SkillType.values().length, distinctExcellencyClasses);
    }

    @Test
    void fromStringResolvesEverySkillTypeByItsExactName() {
        for (SkillType skillType : SkillType.values()) {
            assertEquals(skillType, SkillType.fromString(skillType.name()));
        }
    }

    @Test
    void fromStringIsCaseInsensitive() {
        assertEquals(SkillType.ARTES, SkillType.fromString("artes"));
        assertEquals(SkillType.ATAQUE_A_DISTANCIA, SkillType.fromString("Ataque_A_Distancia"));
    }

    @Test
    void fromStringThrowsForAnUnknownName() {
        assertThrows(IllegalOperationException.class, () -> SkillType.fromString("NOT_A_SKILL"));
    }
}
