package org.aventyrs.core.sheet;

import org.aventyrs.core.ability.GnoseAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.effect.Primor;
import org.aventyrs.core.skill.CriticalResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code GnoseAbility#ESTABILIDADE_EMOCIONAL}'s conditional half, end to end on a live sheet:
 * "a primeira vez em cada sessão de jogo que seu Autocontrole for reduzido a zero você receberá
 * 1 ponto temporário neste Ego na Rodada seguinte". Its permanent-point half is covered by
 * {@code AttributeAbilityServiceTest}.
 */
class EstabilidadeEmocionalFeatureTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /** Every Ego at the fixture's default 2, holding the ability itself. */
    private CharacterSheet sheetHoldingTheAbility() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(GnoseAbility.ESTABILIDADE_EMOCIONAL)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    /** Empties Autocontrole outright: both pools, temporary first. */
    private void emptyAutocontrole(final CombatantSheet sheet) {
        sheet.spendEgoPoints(EgoDomain.AUTOCONTROLE, EgoPointType.TEMPORARY,
                sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
        sheet.spendEgoPoints(EgoDomain.AUTOCONTROLE, EgoPointType.PERMANENT,
                sheet.getPermanentEgoPoints(EgoDomain.AUTOCONTROLE));
    }

    @Test
    void thePointArrivesOnlyOnTheFollowingRodada() {
        CharacterSheet sheet = sheetHoldingTheAbility();

        emptyAutocontrole(sheet);

        // Still empty for the rest of the Rodada it was drained in.
        assertEquals(0, sheet.getAvailableEgoPoints(EgoDomain.AUTOCONTROLE));

        sheet.startNewRound();

        assertEquals(1, sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
        assertEquals(1, sheet.getAvailableEgoPoints(EgoDomain.AUTOCONTROLE));
    }

    /** And it stays there — the next Rodada doesn't hand over a second one. */
    @Test
    void theGrantIsDeliveredOnceNotEveryRodada() {
        CharacterSheet sheet = sheetHoldingTheAbility();
        emptyAutocontrole(sheet);

        sheet.startNewRound();
        sheet.startNewRound();

        assertEquals(1, sheet.getAvailableEgoPoints(EgoDomain.AUTOCONTROLE));
    }

    /** "a primeira vez em cada sessão": emptying it a second time in the same session owes nothing. */
    @Test
    void asecondDepletionInTheSameSessionGrantsNothing() {
        CharacterSheet sheet = sheetHoldingTheAbility();
        emptyAutocontrole(sheet);
        sheet.startNewRound();

        emptyAutocontrole(sheet);
        sheet.startNewRound();

        assertEquals(0, sheet.getAvailableEgoPoints(EgoDomain.AUTOCONTROLE));
    }

    /** …but the next session it fires again, and the granted point is genuinely spendable. */
    @Test
    void aNewSessionArmsItAgainAndTheGrantedPointCanBeSpent() {
        CharacterSheet sheet = sheetHoldingTheAbility();
        emptyAutocontrole(sheet);
        sheet.startNewRound();
        emptyAutocontrole(sheet);

        sheet.startNewSession();
        emptyAutocontrole(sheet);
        sheet.startNewRound();

        assertEquals(1, sheet.getAvailableEgoPoints(EgoDomain.AUTOCONTROLE));
        assertEquals(1, sheet.spendEgoPoints(EgoDomain.AUTOCONTROLE, EgoPointType.TEMPORARY, 1).getValue());
        assertEquals(0, sheet.getAvailableEgoPoints(EgoDomain.AUTOCONTROLE));
    }

    /**
     * "for reduzido a zero" doesn't say by whom — a Primor drain empties the pool just as a
     * deliberate use does, unlike {@code AutocontroleAdvantage#DETERMINACAO_HEROICA}, which
     * reacts only to the holder's own expenditure.
     */
    @Test
    void anEnemysPrimorDrainTriggersItToo() {
        CharacterSheet sheet = sheetHoldingTheAbility();
        // Primor Maior drains 2 temporary points; the permanent pool is spent to reach zero.
        new Primor(CriticalResult.ACERTO_CRITICO_MAIOR, EgoDomain.AUTOCONTROLE).applyTo(sheet);
        sheet.spendEgoPoints(EgoDomain.AUTOCONTROLE, EgoPointType.PERMANENT,
                sheet.getPermanentEgoPoints(EgoDomain.AUTOCONTROLE));

        sheet.startNewRound();

        assertEquals(1, sheet.getAvailableEgoPoints(EgoDomain.AUTOCONTROLE));
    }

    /** A domain merely drawn down, not emptied, owes nothing. */
    @Test
    void spendingWithoutReachingZeroGrantsNothing() {
        CharacterSheet sheet = sheetHoldingTheAbility();

        sheet.spendEgoPoints(EgoDomain.AUTOCONTROLE, EgoPointType.TEMPORARY, 2);
        sheet.startNewRound();

        assertEquals(2, sheet.getAvailableEgoPoints(EgoDomain.AUTOCONTROLE));
        assertFalse(sheet.hasConsumedOncePerSession(GnoseAbility.ESTABILIDADE_EMOCIONAL));
    }

    /** "neste Ego", and no other: emptying Sorte owes nothing, and doesn't burn the session claim. */
    @Test
    void emptyingAnotherEgoDoesNothing() {
        CharacterSheet sheet = sheetHoldingTheAbility();

        sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.TEMPORARY, 2);
        sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.PERMANENT, 2);
        sheet.startNewRound();

        assertEquals(0, sheet.getAvailableEgoPoints(EgoDomain.SORTE));
        assertFalse(sheet.hasConsumedOncePerSession(GnoseAbility.ESTABILIDADE_EMOCIONAL));
    }

    /** A character without the ability gets nothing at all. */
    @Test
    void aCharacterNotHoldingItGetsNothing() {
        CharacterSheet sheet = CharacterSheet.of(
                CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());

        sheet.spendEgoPoints(EgoDomain.AUTOCONTROLE, EgoPointType.TEMPORARY, 2);
        sheet.spendEgoPoints(EgoDomain.AUTOCONTROLE, EgoPointType.PERMANENT, 2);
        sheet.startNewRound();

        assertEquals(0, sheet.getAvailableEgoPoints(EgoDomain.AUTOCONTROLE));
    }

    /** The session marker itself: claimed exactly once, forgotten by a new session. */
    @Test
    void aSessionMarkerIsClaimedOnceAndClearedByANewSession() {
        CharacterSheet sheet = sheetHoldingTheAbility();

        assertTrue(sheet.consumeOncePerSession("a-marker"));
        assertFalse(sheet.consumeOncePerSession("a-marker"));
        assertTrue(sheet.hasConsumedOncePerSession("a-marker"));

        sheet.startNewSession();

        assertFalse(sheet.hasConsumedOncePerSession("a-marker"));
        assertTrue(sheet.consumeOncePerSession("a-marker"));
    }
}
