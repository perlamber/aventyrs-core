package org.aventyrs.core.feat;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.ActiveAbilityService;
import org.aventyrs.core.character.services.ActiveAbilityServiceImpl;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.race.NascidoDoDragao;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.rest.RestService;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.FormType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Entering a Forma as a transaction — {@code DraconicoFeat#DRACONATO} through {@code
 * FormaActiveAbility}: 3PA + 3PD, three Rodadas, and no second transformation until a Descanso
 * Longo. Covers the two pieces slice 2 added to the activation cycle (a Pontos de Determinação
 * cost, and a Resfriamento measured in Descansos) plus the {@code FormEffect} that ends the shape.
 *
 * <p>What the Talento's text <em>also</em> promises while transformed — "+2 Categoria de Tamanho,
 * Força e Foco para cada Título" — is deliberately not asserted: it isn't granted yet, and its two
 * missing mechanisms are recorded on the constant.
 */
class FormaActivationTest {

    private final ActiveAbilityService activeAbilityService = new ActiveAbilityServiceImpl();
    private final RestService restService = new RestServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /** A Nascido do Dragão with a Título — Draconato's own Pré-requisito. */
    private static Character draconico() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .race(new NascidoDoDragao(new Human(), ElementalType.FOGO))
                .feats(new ArrayList<>())
                .build();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        character.grantFeat(DraconicoFeat.DRACONATO);
        return character;
    }

    private static ActiveAbility transformationOf(final Character character) {
        return character.getActiveAbilities().stream()
                .filter(FormaActiveAbility.class::isInstance)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Draconato grants no transformation"));
    }

    @Test
    void transformingSpendsThreeDeterminationAndEntersTheShape() throws IllegalOperationException {
        Character character = draconico();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        activeAbilityService.activate(character, sheet, transformationOf(character), 0);

        assertEquals(3, sheet.getDeterminationSpent(), "3PD — the clause prices it in Determinação");
        assertEquals(0, sheet.getManaSpent(), "and not a point of Mana");
        assertTrue(sheet.isInForm(FormType.DRACONATO));
    }

    /** "A Duração na forma de Draconato é de 3 Rodadas" — and then it ends on its own. */
    @Test
    void theShapeLapsesOnItsOwnWhenTheDuracaoRunsOut() throws IllegalOperationException {
        Character character = draconico();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        activeAbilityService.activate(character, sheet, transformationOf(character), 0);

        sheet.finishTurn();
        sheet.finishTurn();
        assertTrue(sheet.isInForm(FormType.DRACONATO), "still two Rodadas in");

        sheet.finishTurn();

        assertNull(sheet.getCurrentForm(), "the FormEffect put them back");
    }

    /**
     * "Este Efeito não poderá ser reativado até que passe por um Descanso Longo" — a Resfriamento
     * in a different unit from Rodadas, so no number of Rodada boundaries frees it.
     */
    @Test
    void itCannotBeUsedAgainUntilADescansoLongo() throws IllegalOperationException {
        Character character = draconico();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        ActiveAbility transformation = transformationOf(character);
        activeAbilityService.activate(character, sheet, transformation, 0);

        assertTrue(sheet.isAwaitingRest(transformation));
        sheet.startNewRound();
        sheet.startNewRound();
        assertTrue(sheet.isAwaitingRest(transformation), "Rodadas do not clear a Descanso gate");
        assertThrows(IllegalOperationException.class,
                () -> activeAbilityService.activate(character, sheet, transformation, 0));

        restService.applyRest(character, sheet, RestType.CURTO);
        assertTrue(sheet.isAwaitingRest(transformation), "a Curto is not enough");

        restService.applyRest(character, sheet, RestType.LONGO);

        assertFalse(sheet.isAwaitingRest(transformation));
        assertDoesNotThrow(() -> activeAbilityService.activate(character, sheet, transformation, 0));
    }

    /** A stronger Descanso frees it too — {@code RestType#isAtLeast}, not an exact match. */
    @Test
    void aDescansoTotalAlsoFreesAnAbilityWaitingOnALongo() throws IllegalOperationException {
        Character character = draconico();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        ActiveAbility transformation = transformationOf(character);
        activeAbilityService.activate(character, sheet, transformation, 0);

        restService.applyRest(character, sheet, RestType.TOTAL);

        assertFalse(sheet.isAwaitingRest(transformation));
    }

    /** Not enough Determinação refuses the whole transaction, and transforms nobody. */
    @Test
    void anActivationRefusedForMissingDeterminationChangesNothing() {
        Character character = draconico();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        ActiveAbility transformation = transformationOf(character);
        sheet.spendDeterminationPoints(availableDetermination(character, sheet));

        assertThrows(IllegalOperationException.class,
                () -> activeAbilityService.activate(character, sheet, transformation, 0));

        assertNull(sheet.getCurrentForm());
        assertFalse(sheet.isAwaitingRest(transformation), "a refused activation locks nothing out");
    }

    private static int availableDetermination(final Character character, final CharacterSheet sheet) {
        return new org.aventyrs.core.character.services.DeterminationPointsServiceImpl()
                .getCurrentDeterminationPoints(character, sheet);
    }
}
