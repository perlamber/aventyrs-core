package org.aventyrs.core.ego;

import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.skill.Skill;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void posicionamentoEstrategicoGrantsAMovementBlessingThatAppliesToAllies() {
        List<Blessing> blessings = InitiativeAdvantage.POSICIONAMENTO_ESTRATEGICO.resolveInitiativeBlessings();

        assertEquals(1, blessings.size());
        Blessing blessing = blessings.get(0);
        assertEquals(ModifierType.MOVEMENT, blessing.getModifierType());
        assertEquals(2, blessing.getValue());
        assertEquals(2, blessing.getRounds());
        assertEquals(TargetScope.SELF_AND_ALLIES, blessing.getScope());
        assertEquals("POSICIONAMENTO_ESTRATEGICO", blessing.getSource());
    }

    @Test
    void onlyPosicionamentoEstrategicoEverResolvesAnInitiativeBlessing() {
        for (InitiativeAdvantage advantage : InitiativeAdvantage.values()) {
            if (advantage != InitiativeAdvantage.POSICIONAMENTO_ESTRATEGICO) {
                assertEquals(List.of(), advantage.resolveInitiativeBlessings());
            }
        }
    }

    @Test
    void torreEmMovimentoGrantsAbsoluteDamageReductionDuringTheFirstTwoRoundsOfACombatScene() {
        assertEquals(2, InitiativeAdvantage.TORRE_EM_MOVIMENTO.resolveAbsoluteDamageReduction(combatContext(1, false)));
        assertEquals(2, InitiativeAdvantage.TORRE_EM_MOVIMENTO.resolveAbsoluteDamageReduction(combatContext(2, false)));
    }

    @Test
    void torreEmMovimentoGrantsNoAbsoluteDamageReductionPastTheFirstTwoRounds() {
        assertEquals(0, InitiativeAdvantage.TORRE_EM_MOVIMENTO.resolveAbsoluteDamageReduction(combatContext(3, true)));
    }

    @Test
    void torreEmMovimentoGrantsNoAbsoluteDamageReductionOutsideACombatScene() {
        SceneContext nonCombat = new SceneContext(List.of(), List.of(), Map.of(), null, false, 1, true);
        assertEquals(0, InitiativeAdvantage.TORRE_EM_MOVIMENTO.resolveAbsoluteDamageReduction(nonCombat));
    }

    @Test
    void torreEmMovimentoGrantsNoAbsoluteDamageReductionWithoutASceneContext() {
        assertEquals(0, InitiativeAdvantage.TORRE_EM_MOVIMENTO.resolveAbsoluteDamageReduction(null));
    }

    @Test
    void torreEmMovimentoHalvesDamageDuringTheFirstTwoRoundsWhenInitiativeWasWon() {
        assertTrue(InitiativeAdvantage.TORRE_EM_MOVIMENTO.resolveHalfDamage(combatContext(1, true)));
        assertTrue(InitiativeAdvantage.TORRE_EM_MOVIMENTO.resolveHalfDamage(combatContext(2, true)));
    }

    @Test
    void torreEmMovimentoDoesNotHalveDamageWhenInitiativeWasNotWon() {
        assertFalse(InitiativeAdvantage.TORRE_EM_MOVIMENTO.resolveHalfDamage(combatContext(1, false)));
    }

    @Test
    void torreEmMovimentoDoesNotHalveDamagePastTheFirstTwoRoundsEvenWhenInitiativeWasWon() {
        assertFalse(InitiativeAdvantage.TORRE_EM_MOVIMENTO.resolveHalfDamage(combatContext(3, true)));
    }

    @Test
    void torreEmMovimentoDoesNotHalveDamageWithoutASceneContext() {
        assertFalse(InitiativeAdvantage.TORRE_EM_MOVIMENTO.resolveHalfDamage(null));
    }

    @Test
    void onlyTorreEmMovimentoEverResolvesAbsoluteDamageReductionOrHalfDamage() {
        SceneContext context = combatContext(1, true);

        for (InitiativeAdvantage advantage : InitiativeAdvantage.values()) {
            if (advantage != InitiativeAdvantage.TORRE_EM_MOVIMENTO) {
                assertEquals(0, advantage.resolveAbsoluteDamageReduction(context));
                assertFalse(advantage.resolveHalfDamage(context));
            }
        }
    }
}
