package org.aventyrs.core.character.services;

import org.aventyrs.core.action.ReactionContext;
import org.aventyrs.core.action.ReactionOption;
import org.aventyrs.core.action.ReactionTrigger;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.Teleportation;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.title.PDCost;
import org.aventyrs.core.title.santo.Santo;
import org.aventyrs.core.title.santo.SantoAbility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code GUARDA_VIDAS} is the only Reação in the catalog, so it is what every case here is built
 * around — the service is exercised through a really-granted {@link Santo}, not by calling the
 * ability's own hooks directly.
 */
class ReactionOptionsServiceTest {

    private final ReactionOptionsService reactionOptionsService = new ReactionOptionsServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void aSantoIsOfferedGuardaVidasWhenAnAllyInRangeIsAttacked() {
        CombatantSheet santo = santoHolding(SantoAbility.GUARDA_VIDAS);
        CombatantSheet ally = plainSheet();

        List<ReactionOption> options = reactionOptionsService.getAvailableReactions(
                allyAttacked(santo, ally, Range.DISTANCIA_CURTA));

        assertEquals(1, options.size());
        ReactionOption option = options.get(0);
        assertEquals(SantoAbility.GUARDA_VIDAS, option.ability());
        assertEquals(ActionCost.REACTION, option.cost());
        assertEquals(PDCost.fixed(3), option.determinationCost());
        assertEquals(Teleportation.of(Range.DISTANCIA_CURTA), option.teleportation());
        assertTrue(option.affordable());
    }

    // "um aliado em Distância Curta" — the teleport is what bounds this, so an ally standing
    // further away is simply not somebody this Santo can reach in time.
    @Test
    void anAllyBeyondTheTeleportReachOffersNothing() {
        CombatantSheet santo = santoHolding(SantoAbility.GUARDA_VIDAS);
        CombatantSheet ally = plainSheet();

        List<ReactionOption> options = reactionOptionsService.getAvailableReactions(
                allyAttacked(santo, ally, Range.DISTANCIA_MEDIA));

        assertTrue(options.isEmpty());
    }

    @Test
    void anAllyWhoIsNotActuallyAnAllyOffersNothing() {
        CombatantSheet santo = santoHolding(SantoAbility.GUARDA_VIDAS);
        CombatantSheet stranger = plainSheet();
        // Named as threatened, but absent from the Santo's own ally list.
        SceneContext sceneContext = new SceneContext(List.of(), List.of(stranger),
                Map.of(stranger, Range.ADJACENTE));

        List<ReactionOption> options = reactionOptionsService.getAvailableReactions(
                ReactionContext.builder()
                        .trigger(ReactionTrigger.ALLY_TARGETED_BY_ATTACK)
                        .reactor(santo)
                        .reactorContext(sceneContext)
                        .threatenedAlly(stranger)
                        .build());

        assertTrue(options.isEmpty());
    }

    @Test
    void aSantoWhoDoesNotHoldGuardaVidasIsOfferedNothing() {
        CombatantSheet santo = santoHolding(SantoAbility.BASTIAO_DOS_NECESSITADOS);
        CombatantSheet ally = plainSheet();

        List<ReactionOption> options = reactionOptionsService.getAvailableReactions(
                allyAttacked(santo, ally, Range.ADJACENTE));

        assertTrue(options.isEmpty());
    }

    @Test
    void aCharacterWithNoTituloAtAllIsOfferedNothing() {
        CombatantSheet plain = plainSheet();
        CombatantSheet ally = plainSheet();

        List<ReactionOption> options = reactionOptionsService.getAvailableReactions(
                allyAttacked(plain, ally, Range.ADJACENTE));

        assertTrue(options.isEmpty());
    }

    // "Cannot tell" is nobody, not everybody — the same rule MovementReactionService follows.
    @Test
    void noSceneContextOffersNothing() {
        CombatantSheet santo = santoHolding(SantoAbility.GUARDA_VIDAS);

        List<ReactionOption> options = reactionOptionsService.getAvailableReactions(
                ReactionContext.builder()
                        .trigger(ReactionTrigger.ALLY_TARGETED_BY_ATTACK)
                        .reactor(santo)
                        .threatenedAlly(plainSheet())
                        .build());

        assertTrue(options.isEmpty());
    }

    // An unaffordable Reação is still a Reação the character *has* — a client greys it out rather
    // than pretending the Suprema isn't there.
    @Test
    void anUnaffordableReactionIsReportedRatherThanHidden() {
        CombatantSheet santo = santoHolding(SantoAbility.GUARDA_VIDAS);
        CombatantSheet ally = plainSheet();
        int maxDeterminationPoints = new DeterminationPointsServiceImpl()
                .getMaxDeterminationPoints(santo.getCharacter());
        // Down to 1PD, one short of Guarda-Vidas' 2.
        santo.spendDeterminationPoints(maxDeterminationPoints - 1);

        List<ReactionOption> options = reactionOptionsService.getAvailableReactions(
                allyAttacked(santo, ally, Range.DISTANCIA_CURTA));

        assertEquals(1, options.size());
        assertFalse(options.get(0).affordable());
    }

    @Test
    void aSantoEntitledToNoReactionsIsReportedAsUnableToAfford() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .reactions(0)
                .build();
        character.grantTitle(new Santo(List.of(), List.of(SantoAbility.GUARDA_VIDAS)), TitleSlot.PRIMARY);
        CombatantSheet santo = CharacterSheet.of(character, new Player());
        CombatantSheet ally = plainSheet();

        List<ReactionOption> options = reactionOptionsService.getAvailableReactions(
                allyAttacked(santo, ally, Range.DISTANCIA_CURTA));

        assertEquals(1, options.size());
        assertFalse(options.get(0).affordable());
    }

    private ReactionContext allyAttacked(final CombatantSheet reactor, final CombatantSheet ally,
                                         final Range distance) {
        return ReactionContext.builder()
                .trigger(ReactionTrigger.ALLY_TARGETED_BY_ATTACK)
                .reactor(reactor)
                .reactorContext(new SceneContext(List.of(ally), List.of(), Map.of(ally, distance)))
                .threatenedAlly(ally)
                .build();
    }

    private CombatantSheet santoHolding(final SantoAbility... abilities) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        character.grantTitle(new Santo(List.of(), List.of(abilities)), TitleSlot.PRIMARY);
        return CharacterSheet.of(character, new Player());
    }

    private CombatantSheet plainSheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }
}
