package org.aventyrs.core.sheet;

import org.aventyrs.core.character.fixture.CharacterFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The per-Turn activation count — what "se ativada duas vezes no mesmo Turno" reads
 * ({@code AbracadoPelaEscuridaoAbility#SACRIFICIO_YMIRIANO}).
 */
class AbilityActivationCountTest {

    private static final Object ABILITY = new Object();
    private static final Object OTHER_ABILITY = new Object();

    private CharacterSheet sheet;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        sheet = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }

    @Test
    void anUnactivatedAbilityCountsZero() {
        assertEquals(0, sheet.countActivationsThisTurn(ABILITY));
    }

    @Test
    void eachActivationRaisesItsOwnCountOnly() {
        sheet.recordAbilityActivation(ABILITY);
        sheet.recordAbilityActivation(ABILITY);
        sheet.recordAbilityActivation(OTHER_ABILITY);

        assertEquals(2, sheet.countActivationsThisTurn(ABILITY));
        assertEquals(1, sheet.countActivationsThisTurn(OTHER_ABILITY));
    }

    @Test
    void startTurnClearsEveryCount() {
        sheet.recordAbilityActivation(ABILITY);
        sheet.recordAbilityActivation(OTHER_ABILITY);

        sheet.startTurn(1);

        assertEquals(0, sheet.countActivationsThisTurn(ABILITY));
        assertEquals(0, sheet.countActivationsThisTurn(OTHER_ABILITY));
    }

    /** A Rodada boundary is not a Turn boundary — only {@code startTurn} resets the count. */
    @Test
    void startNewRoundAloneDoesNotClearTheCount() {
        sheet.recordAbilityActivation(ABILITY);

        sheet.startNewRound();

        assertEquals(1, sheet.countActivationsThisTurn(ABILITY));
    }
}
