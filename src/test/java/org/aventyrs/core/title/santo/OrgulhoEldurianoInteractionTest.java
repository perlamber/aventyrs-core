package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.scene.ActiveAura;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.title.TitleAbilityActivationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.aventyrs.core.util.TranslatableMessages.ABILITY_ACTIVATION_PREVENTED;
import static org.aventyrs.core.util.TranslatableMessages.INVALID_PD_AMOUNT;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_DETERMINATION_POINTS;
import static org.aventyrs.core.util.TranslatableMessages.REQUIRED_TITLE_TRAIT_NOT_HELD;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_REQUIRES_SCENE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrgulhoEldurianoInteractionTest {

    private final DeterminationPointsService determinationPointsService = new DeterminationPointsServiceImpl();

    private Scene scene;
    private CharacterSheet holder;
    private CharacterSheet foe;
    private Santo santo;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        scene = new Scene();
        holder = newSheet();
        foe = newSheet();
        scene.addParticipant(holder, 20, UUID.randomUUID());
        scene.addParticipant(foe, 10, UUID.randomUUID());
        santo = new Santo(List.of(SantoSpecialization.ABENCOADO_PELA_LUZ),
                List.of(AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO));
    }

    private CharacterSheet newSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        return CharacterSheet.of(character, new Player());
    }

    private int currentPd() {
        return determinationPointsService.getCurrentDeterminationPoints(holder.getCharacter(), holder);
    }

    private static void assertRefused(final String message, final Executable activation) {
        assertEquals(message, assertThrows(IllegalOperationException.class, activation).getMessage());
    }

    @Test
    void theHolderHasEnoughPdForTheseTests() {
        assertTrue(currentPd() >= 2, "fixture must start with at least 2 PD, has " + currentPd());
    }

    @Test
    void activationSpendsTheChosenPdAndLastsThatManyRodadas() {
        int before = currentPd();

        InteractionResult result = santo.activateOrgulhoElduriano(holder, scene, 2, null);

        assertEquals(2, result.getDeterminationPointsSpent());
        assertEquals(before - 2, currentPd());
        assertEquals(1, scene.getActiveAuras().size());
        ActiveAura aura = scene.getActiveAuras().get(0);
        assertEquals(2, aura.getRemainingRounds());
        assertEquals(Range.DISTANCIA_CURTA, aura.getRadius());
        assertSame(AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO, aura.getSource());
    }

    @Test
    void aVariableCostWithNoAmountChosenIsRefused() {
        assertRefused(INVALID_PD_AMOUNT, () -> new OrgulhoEldurianoInteraction().activate(
                TitleAbilityActivationRequest.builder().activator(holder).scene(scene).build()));
        assertEquals(List.of(), scene.getActiveAuras());
    }

    @Test
    void anActivationWithoutASceneIsRefusedAndCostsNothing() {
        int before = currentPd();

        assertRefused(TITLE_ABILITY_REQUIRES_SCENE, () -> new OrgulhoEldurianoInteraction().activate(
                TitleAbilityActivationRequest.builder().activator(holder).determinationPoints(2).build()));

        assertEquals(before, currentPd());
    }

    @Test
    void aSuppliedContextBindsFoesAlreadyInRange() {
        santo.activateOrgulhoElduriano(holder, scene, 2,
                scene.buildContext(holder, Map.of(foe, Range.DISTANCIA_MUITO_CURTA)));

        assertEquals(Optional.of(holder), scene.getForcedAttackTarget(foe));
    }

    @Test
    void spendingBelowTheMinimumIsRefusedAndCostsNothing() {
        int before = currentPd();

        assertRefused(INVALID_PD_AMOUNT, () -> santo.activateOrgulhoElduriano(holder, scene, 0, null));

        assertEquals(before, currentPd());
        assertEquals(List.of(), scene.getActiveAuras());
    }

    @Test
    void spendingMorePdThanTheHolderHasIsRefused() {
        int before = currentPd();

        assertRefused(NOT_ENOUGH_DETERMINATION_POINTS,
                () -> santo.activateOrgulhoElduriano(holder, scene, before + 1, null));

        assertEquals(before, currentPd());
        assertEquals(List.of(), scene.getActiveAuras());
    }

    @Test
    void silencioRefusesTheActivation() {
        holder.applyCondition(new Condition(ConditionType.SILENCIO, 1));

        assertRefused(ABILITY_ACTIVATION_PREVENTED, () -> santo.activateOrgulhoElduriano(holder, scene, 2, null));
        assertEquals(List.of(), scene.getActiveAuras());
    }

    @Test
    void aSantoWithoutTheHabilidadeIsRefused() {
        Santo without = new Santo(List.of(SantoSpecialization.ABENCOADO_PELA_LUZ), List.of());

        assertRefused(REQUIRED_TITLE_TRAIT_NOT_HELD, () -> without.activateOrgulhoElduriano(holder, scene, 2, null));
    }
}
