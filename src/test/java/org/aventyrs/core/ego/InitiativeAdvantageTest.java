package org.aventyrs.core.ego;

import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.Skill;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class InitiativeAdvantageTest {

    @Test
    void everyAdvantageBelongsToIniciativa() {
        for (InitiativeAdvantage advantage : InitiativeAdvantage.values()) {
            assertEquals(EgoDomain.INICIATIVA, advantage.getEgoDomain());
        }
    }

    @Test
    void everyAdvantageHasADescription() {
        for (InitiativeAdvantage advantage : InitiativeAdvantage.values()) {
            assertFalse(advantage.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheThreeDescribedAdvantages() {
        assertEquals(3, InitiativeAdvantage.values().length);
    }

    private SceneContext combatContext(final int currentRound, final boolean wonInitiative) {
        return new SceneContext(List.of(), List.of(), Map.of(), null, true, currentRound, wonInitiative);
    }

    @Test
    void impetoGrantsAConditionalRollBonusDuringTheFirstTwoRoundsOfACombatScene() {
        assertEquals(Optional.of(Skill.ADVANTAGE_BONUS), InitiativeAdvantage.IMPETO.resolveConditionalRollBonus(combatContext(1, false)));
        assertEquals(Optional.of(Skill.ADVANTAGE_BONUS), InitiativeAdvantage.IMPETO.resolveConditionalRollBonus(combatContext(2, false)));
    }

    @Test
    void impetoGrantsNoConditionalRollBonusPastTheFirstTwoRounds() {
        assertEquals(Optional.empty(), InitiativeAdvantage.IMPETO.resolveConditionalRollBonus(combatContext(3, false)));
    }

    @Test
    void impetoGrantsNoConditionalRollBonusOutsideACombatScene() {
        SceneContext nonCombat = new SceneContext(List.of(), List.of(), Map.of(), null, false, 1, false);
        assertEquals(Optional.empty(), InitiativeAdvantage.IMPETO.resolveConditionalRollBonus(nonCombat));
    }

    @Test
    void impetoGrantsNoConditionalRollBonusWithoutASceneContext() {
        assertEquals(Optional.empty(), InitiativeAdvantage.IMPETO.resolveConditionalRollBonus(null));
    }

    @Test
    void impetoGrantsADamageBonusDuringTheFirstTwoRoundsWhenInitiativeWasWon() {
        Optional<DamageBonus> bonus = InitiativeAdvantage.IMPETO.resolveDamageBonus(combatContext(1, true));

        assertEquals(Skill.ADVANTAGE_BONUS, bonus.orElseThrow().getValue());
        assertEquals(DamageType.FISICO, bonus.orElseThrow().getType());
    }

    @Test
    void impetoGrantsNoDamageBonusWhenInitiativeWasNotWon() {
        assertEquals(Optional.empty(), InitiativeAdvantage.IMPETO.resolveDamageBonus(combatContext(1, false)));
    }

    @Test
    void impetoGrantsNoDamageBonusPastTheFirstTwoRoundsEvenWhenInitiativeWasWon() {
        assertEquals(Optional.empty(), InitiativeAdvantage.IMPETO.resolveDamageBonus(combatContext(3, true)));
    }

    @Test
    void impetoGrantsNoDamageBonusWithoutASceneContext() {
        assertEquals(Optional.empty(), InitiativeAdvantage.IMPETO.resolveDamageBonus(null));
    }

    @Test
    void onlyImpetoEverResolvesAConditionalRollOrDamageBonus() {
        SceneContext context = combatContext(1, true);

        for (InitiativeAdvantage advantage : InitiativeAdvantage.values()) {
            if (advantage != InitiativeAdvantage.IMPETO) {
                assertEquals(Optional.empty(), advantage.resolveConditionalRollBonus(context));
                assertEquals(Optional.empty(), advantage.resolveDamageBonus(context));
            }
        }
    }
}
