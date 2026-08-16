package org.aventyrs.core.ego;

import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SorteAdvantageTest {

    @Test
    void everyAdvantageBelongsToSorte() {
        for (SorteAdvantage advantage : SorteAdvantage.values()) {
            assertEquals(EgoDomain.SORTE, advantage.getEgoDomain());
        }
    }

    @Test
    void everyAdvantageHasADescription() {
        for (SorteAdvantage advantage : SorteAdvantage.values()) {
            assertFalse(advantage.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheThreeDescribedAdvantages() {
        assertEquals(3, SorteAdvantage.values().length);
    }

    private SceneContext combatContext() {
        return new SceneContext(List.of(), List.of(), Map.of(), null, true, 1, false);
    }

    private SceneContext nonCombatContext() {
        return new SceneContext(List.of(), List.of(), Map.of(), null, false, 0, false);
    }

    @Test
    void aceGrantsPlusOneMarginForAnAttackSkillDuringACombatScene() {
        assertEquals(1, SorteAdvantage.ACE.resolveCriticalMarginIncrease(SkillType.ATAQUE_CORPO_A_CORPO, combatContext()));
        assertEquals(1, SorteAdvantage.ACE.resolveCriticalMarginIncrease(SkillType.ATAQUE_A_DISTANCIA, combatContext()));
    }

    @Test
    void aceGrantsNoMarginForANonAttackSkillDuringACombatScene() {
        assertEquals(0, SorteAdvantage.ACE.resolveCriticalMarginIncrease(SkillType.ARTES, combatContext()));
    }

    @Test
    void aceGrantsPlusThreeMarginForANonAttackSkillOutsideACombatScene() {
        assertEquals(3, SorteAdvantage.ACE.resolveCriticalMarginIncrease(SkillType.ARTES, nonCombatContext()));
    }

    @Test
    void aceGrantsNoMarginForAnAttackSkillOutsideACombatScene() {
        assertEquals(0, SorteAdvantage.ACE.resolveCriticalMarginIncrease(SkillType.ATAQUE_CORPO_A_CORPO, nonCombatContext()));
    }

    @Test
    void aceGrantsNoMarginWithoutASceneContext() {
        assertEquals(0, SorteAdvantage.ACE.resolveCriticalMarginIncrease(SkillType.ATAQUE_CORPO_A_CORPO, null));
        assertEquals(0, SorteAdvantage.ACE.resolveCriticalMarginIncrease(SkillType.ARTES, null));
    }

    @Test
    void onlyAceEverResolvesACriticalMarginIncrease() {
        for (SorteAdvantage advantage : SorteAdvantage.values()) {
            if (advantage != SorteAdvantage.ACE) {
                assertEquals(0, advantage.resolveCriticalMarginIncrease(SkillType.ATAQUE_CORPO_A_CORPO, combatContext()));
                assertEquals(0, advantage.resolveCriticalMarginIncrease(SkillType.ARTES, nonCombatContext()));
            }
        }
    }
}
