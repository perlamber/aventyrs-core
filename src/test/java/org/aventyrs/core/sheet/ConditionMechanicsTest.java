package org.aventyrs.core.sheet;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DiseaseService;
import org.aventyrs.core.character.services.DiseaseServiceImpl;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Condições whose mechanics are more than an Estado (core 0.1.5): Cego's reach table,
 * Feridas Dolorosas' PM/PD half, Envenenado's continuous damage and Doente's spreading.
 */
class ConditionMechanicsTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet sheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID())
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(4).build())
                        .build())
                .build(), new Player());
    }

    // ---------- Cego ----------

    @Test
    void onlyAForcaOrDestrezaRollOwesTheBlindDie() {
        CharacterSheet blind = sheet();
        blind.applyCondition(new Condition(ConditionType.CEGO, 2));

        assertEquals(OptionalInt.of(BlindCheck.Reach.PERSONAL.getFailureFace()),
                blind.getBlindCheckThreshold(SkillType.ATLETISMO, AttributeDomain.STRENGTH, null, null));
        assertEquals(OptionalInt.empty(),
                blind.getBlindCheckThreshold(SkillType.ATTENTION, AttributeDomain.INSTINCT, null, null));
    }

    @Test
    void anAttacksBlindFaceDependsOnItsTargetsDistance() {
        CharacterSheet blind = sheet();
        CharacterSheet target = sheet();
        blind.applyCondition(new Condition(ConditionType.CEGO, 2));
        SceneContext adjacent = new SceneContext(List.of(), List.of(target), Map.of(target, Range.ADJACENTE));
        SceneContext far = new SceneContext(List.of(), List.of(target), Map.of(target, Range.DISTANCIA_MEDIA));

        assertEquals(OptionalInt.of(3), blind.getBlindCheckThreshold(SkillType.ATAQUE_A_DISTANCIA,
                AttributeDomain.DEXTERITY, adjacent, target));
        assertEquals(OptionalInt.of(5), blind.getBlindCheckThreshold(SkillType.ATAQUE_A_DISTANCIA,
                AttributeDomain.DEXTERITY, far, target));
    }

    // ---------- Feridas Dolorosas ----------

    @Test
    void feridasDolorosasStopsPdAndPmRecoveryToo() {
        CharacterSheet wounded = sheet();
        // spend/recover report what is spent after the change, not the change itself.
        assertEquals(2, wounded.spendDeterminationPoints(2));
        assertEquals(2, wounded.spendMagicPoints(2));

        wounded.applyCondition(new Condition(ConditionType.FERIDAS_DOLOROSAS, 2));

        assertEquals(2, wounded.recoverDeterminationPoints(1), "nothing came back");
        assertEquals(2, wounded.recoverMagicPoints(1), "nothing came back");

        wounded.removeCondition(ConditionType.FERIDAS_DOLOROSAS);
        assertEquals(1, wounded.recoverDeterminationPoints(1));
    }

    // ---------- Envenenado ----------

    @Test
    void aPoisoningDealsItsVenenosDamageEachRodadaAndIsFraqueza() {
        CharacterSheet poisoned = sheet();
        poisoned.applyCondition(new Poisoning(2, 2, null, List.of()));

        assertTrue(poisoned.hasCondition(ConditionType.FRAQUEZA, null));
        poisoned.tickTemporaryEffects();
        assertEquals(2, poisoned.getDamageTaken());
        poisoned.tickTemporaryEffects();
        assertEquals(4, poisoned.getDamageTaken());
        poisoned.tickTemporaryEffects();
        assertEquals(4, poisoned.getDamageTaken(), "it lapsed after its 2 Rodadas");
    }

    // ---------- Doente ----------

    private final DiseaseService diseaseService = new DiseaseServiceImpl();

    @Test
    void aSpreadingDiseaseInfectsAnAdjacentCharacterOnAOne() {
        CharacterSheet sick = sheet();
        CharacterSheet lucky = sheet();
        CharacterSheet unlucky = sheet();
        sick.applyCondition(new Disease(5, null, true, List.of()));

        assertEquals(List.of(lucky, unlucky), diseaseService.propagationCandidates(sick, List.of(lucky, unlucky)));
        assertFalse(diseaseService.resolvePropagation(sick, lucky, 2));
        assertTrue(diseaseService.resolvePropagation(sick, unlucky, DiseaseService.INFECTING_FACE));

        assertFalse(lucky.hasCondition(ConditionType.DOENTE, null));
        assertTrue(unlucky.hasCondition(ConditionType.DOENTE, null));
        assertTrue(unlucky.hasCondition(ConditionType.FRAQUEZA, null));
    }

    @Test
    void aDiseaseThatDoesNotSpreadHasNoCandidates() {
        CharacterSheet sick = sheet();
        sick.applyCondition(new Disease(5, null, false, List.of()));

        assertTrue(diseaseService.propagationCandidates(sick, List.of(sheet())).isEmpty());
    }

    /** "Monstros capazes de adoecer seus alvos … são imunes a própria doença." */
    @Test
    void theInfectingCreatureIsImmuneToItsOwnDisease() {
        CharacterSheet carrier = sheet();
        CharacterSheet sick = sheet();
        sick.applyCondition(new Disease(5, carrier, true, List.of()));

        assertTrue(diseaseService.propagationCandidates(sick, List.of(carrier)).isEmpty());
        carrier.applyCondition(new Disease(5, carrier, true, List.of()));
        assertFalse(carrier.hasCondition(ConditionType.DOENTE, null));
    }
}
