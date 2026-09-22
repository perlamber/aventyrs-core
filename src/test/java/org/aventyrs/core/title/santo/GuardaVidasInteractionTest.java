package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.title.TitleAbilityActivationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_DETERMINATION_POINTS;
import static org.aventyrs.core.util.TranslatableMessages.TELEPORT_TARGET_OUT_OF_RANGE;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_REQUIRES_SCENE;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_REQUIRES_TARGET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GuardaVidasInteractionTest {

    private final DeterminationPointsService determinationPointsService = new DeterminationPointsServiceImpl();
    private final GuardaVidasInteraction interaction = new GuardaVidasInteraction();

    private CharacterSheet santo;
    private CharacterSheet ally;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        santo = newSheet();
        ally = newSheet();
    }

    private CharacterSheet newSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        return CharacterSheet.of(character, new Player());
    }

    private int currentPd() {
        return determinationPointsService.getCurrentDeterminationPoints(santo.getCharacter(), santo);
    }

    private TitleAbilityActivationRequest interceptingFor(final CombatantSheet target, final Range distance) {
        return TitleAbilityActivationRequest.builder()
                .activator(santo)
                .target(target)
                .sceneContext(new SceneContext(List.of(ally), List.of(), Map.of(ally, distance)))
                .build();
    }

    @Test
    void activatingMakesTheSantoTheAttacksTargetInstead() {
        InteractionResult result = interaction.activate(interceptingFor(ally, Range.DISTANCIA_CURTA));

        assertSame(santo, result.getRedirectedAttackTarget());
    }

    @Test
    void activatingSpendsExactlyThreeDeterminationPoints() {
        int before = currentPd();

        InteractionResult result = interaction.activate(interceptingFor(ally, Range.DISTANCIA_CURTA));

        assertEquals(3, result.getDeterminationPointsSpent());
        assertEquals(before - 3, currentPd());
    }

    @Test
    void anActivationWithoutASceneContextIsRefused() {
        IllegalOperationException thrown = assertThrows(IllegalOperationException.class,
                () -> interaction.activate(TitleAbilityActivationRequest.builder()
                        .activator(santo)
                        .target(ally)
                        .build()));

        assertEquals(TITLE_ABILITY_REQUIRES_SCENE, thrown.getMessage());
    }

    // "um aliado" is somebody else — an omitted target must refuse rather than quietly default to
    // the activator, which getEffectiveTarget() would have done.
    @Test
    void anActivationNamingNoAllyIsRefused() {
        IllegalOperationException thrown = assertThrows(IllegalOperationException.class,
                () -> interaction.activate(interceptingFor(null, Range.DISTANCIA_CURTA)));

        assertEquals(TITLE_ABILITY_REQUIRES_TARGET, thrown.getMessage());
    }

    @Test
    void anActivationNamingTheSantoThemselvesIsRefused() {
        IllegalOperationException thrown = assertThrows(IllegalOperationException.class,
                () -> interaction.activate(interceptingFor(santo, Range.DISTANCIA_CURTA)));

        assertEquals(TITLE_ABILITY_REQUIRES_TARGET, thrown.getMessage());
    }

    @Test
    void anActivationNamingSomebodyWhoIsNotAnAllyIsRefused() {
        CombatantSheet foe = newSheet();

        IllegalOperationException thrown = assertThrows(IllegalOperationException.class,
                () -> interaction.activate(TitleAbilityActivationRequest.builder()
                        .activator(santo)
                        .target(foe)
                        .sceneContext(new SceneContext(List.of(ally), List.of(foe), Map.of(foe, Range.ADJACENTE)))
                        .build()));

        assertEquals(TITLE_ABILITY_REQUIRES_TARGET, thrown.getMessage());
    }

    @Test
    void anAllyBeyondDistanciaCurtaIsRefused() {
        IllegalOperationException thrown = assertThrows(IllegalOperationException.class,
                () -> interaction.activate(interceptingFor(ally, Range.DISTANCIA_MEDIA)));

        assertEquals(TELEPORT_TARGET_OUT_OF_RANGE, thrown.getMessage());
    }

    // The base class's pay-nothing-on-refusal contract: every gate runs before a single PD moves,
    // so a Santo who tried to save an ally out of reach has not paid for the attempt.
    @Test
    void aRefusedActivationSpendsNothing() {
        int before = currentPd();

        assertThrows(IllegalOperationException.class,
                () -> interaction.activate(interceptingFor(ally, Range.DISTANCIA_MEDIA)));
        assertThrows(IllegalOperationException.class,
                () -> interaction.activate(interceptingFor(null, Range.DISTANCIA_CURTA)));
        assertThrows(IllegalOperationException.class,
                () -> interaction.activate(TitleAbilityActivationRequest.builder()
                        .activator(santo)
                        .target(ally)
                        .build()));

        assertEquals(before, currentPd());
    }

    @Test
    void aSantoWhoCannotPayTheTwoPdIsRefused() {
        santo.spendDeterminationPoints(currentPd() - 1);

        IllegalOperationException thrown = assertThrows(IllegalOperationException.class,
                () -> interaction.activate(interceptingFor(ally, Range.DISTANCIA_CURTA)));

        assertEquals(NOT_ENOUGH_DETERMINATION_POINTS, thrown.getMessage());
    }

    @Test
    void anAdjacentAllyIsWithinReachToo() {
        InteractionResult result = interaction.activate(interceptingFor(ally, Range.ADJACENTE));

        assertSame(santo, result.getRedirectedAttackTarget());
    }
}
