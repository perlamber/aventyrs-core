package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.PeleDePedra;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Corpo Indestrutível de Epona — one hit negated outright, then an RDS that wears away two points
 * at a time.
 */
class CorpoIndestrutivelDeEponaInteractionTest {

    private final CorpoIndestrutivelDeEponaInteraction interaction = new CorpoIndestrutivelDeEponaInteraction();
    private final DamageService damageService = new DamageServiceImpl();
    private final DeterminationPointsService determinationPointsService = new DeterminationPointsServiceImpl();

    private CharacterSheet holder;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        holder = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }

    private int take(final int rawDamage) {
        return damageService.calculateFinalDamage(holder, null, (DamageType) null, null, rawDamage, false);
    }

    @Test
    void activationSpendsFourPdAndRaisesTheStone() {
        int pdBefore = determinationPointsService.getCurrentDeterminationPoints(holder.getCharacter(), holder);

        InteractionResult result = interaction.applyTo(holder);

        assertEquals(4, result.getDeterminationPointsSpent());
        assertEquals(pdBefore - 4, determinationPointsService.getCurrentDeterminationPoints(holder.getCharacter(), holder));
        assertTrue(holder.getPeleDePedra().isPresent());
        assertTrue(holder.getPeleDePedra().orElseThrow().negatesNextHit());
    }

    /** "O primeiro ataque que lhe causaria Danos é reduzido à zero." */
    @Test
    void theFirstDamagingHitIsNegatedOutright() {
        interaction.applyTo(holder);

        assertEquals(0, take(10));
        assertFalse(holder.getPeleDePedra().orElseThrow().negatesNextHit());
    }

    /** "após isso você recebe RDS 5. A Redução … é reduzida em -2 para cada dano sofrido." */
    @Test
    void theStoneThenReducesByFiveAndWearsDownTwoAtATime() {
        interaction.applyTo(holder);
        take(10);

        assertEquals(10 - PeleDePedra.INITIAL_DAMAGE_REDUCTION, take(10));
        assertEquals(10 - 3, take(10));
        assertEquals(10 - 1, take(10));
        // Floored at 0 — a spent stone stops helping and never starts adding to incoming damage.
        assertEquals(10, take(10));
        assertEquals(10, take(10));
    }

    /**
     * "que lhe *causaria* Danos" — a blow the ordinary mitigation already turned aside must not
     * spend the one negation.
     */
    @Test
    void aHitThatWouldHaveDealtNothingAnywayDoesNotSpendTheNegation() {
        interaction.applyTo(holder);

        assertEquals(0, take(0));

        assertTrue(holder.getPeleDePedra().orElseThrow().negatesNextHit());
        assertEquals(0, take(10));
    }

    /** The Duração is the other limit, and it ends the effect whatever stone is left. */
    @Test
    void theStoneLapsesAfterItsOneRodada() {
        interaction.applyTo(holder);

        holder.finishTurn();

        assertTrue(holder.getPeleDePedra().isEmpty());
        assertEquals(10, take(10));
    }

    /** Re-entering the state replaces it rather than layering a second skin of stone. */
    @Test
    void reactivatingReplacesRatherThanStacks() {
        interaction.applyTo(holder);
        take(10);
        assertFalse(holder.getPeleDePedra().orElseThrow().negatesNextHit());

        interaction.applyTo(holder);

        assertTrue(holder.getPeleDePedra().orElseThrow().negatesNextHit());
        assertEquals(0, take(10));
    }
}
