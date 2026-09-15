package org.aventyrs.core.effect;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Procrastinar Ferimento — "os PV … perderia em decorrência de um ataque sejam perdidos apenas no
 * Rodada seguinte".
 */
class PostponedDamageTest {

    private static final int RAW_DAMAGE = 12;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet newSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        return CharacterSheet.of(character, new Player());
    }

    private InteractionResult hit(final CharacterSheet target, final DamageInteraction damage) {
        return damage.applyTo(target, null, DamageType.FISICO, null, RAW_DAMAGE, true, null);
    }

    @Test
    void thePvAreNotLostWhenTheHitResolves() {
        CharacterSheet target = newSheet();

        hit(target, new DamageInteraction().postponing());

        assertEquals(0, target.getDamageTaken());
    }

    @Test
    void theyLandAtTheNextRodadaBoundary() {
        CharacterSheet target = newSheet();

        hit(target, new DamageInteraction().postponing());
        target.startNewRound();

        assertEquals(RAW_DAMAGE, target.getDamageTaken());
    }

    @Test
    void aPostponedHitDoesNotLandInTheRodadaItWasPostponedIn() {
        CharacterSheet target = newSheet();

        hit(target, new DamageInteraction().postponing());
        // finishTurn drives tickTemporaryEffects, which is Turn end rather than the Rodada
        // boundary — the precise confusion PostponedDamage exists to avoid.
        target.finishTurn();

        assertEquals(0, target.getDamageTaken());
    }

    @Test
    void itLandsOnceAndOnlyOnce() {
        CharacterSheet target = newSheet();

        hit(target, new DamageInteraction().postponing());
        target.startNewRound();
        target.startNewRound();

        assertEquals(RAW_DAMAGE, target.getDamageTaken());
    }

    @Test
    void severalPostponedInOneRodadaAllLandTogether() {
        CharacterSheet target = newSheet();

        hit(target, new DamageInteraction().postponing());
        hit(target, new DamageInteraction().postponing());
        target.startNewRound();

        assertEquals(RAW_DAMAGE * 2, target.getDamageTaken());
    }

    @Test
    void theDamageIsDeferredNotCancelledSoTheFigureIsStillReported() {
        CharacterSheet target = newSheet();

        InteractionResult result = hit(target, new DamageInteraction().postponing());

        assertEquals(RAW_DAMAGE, result.getResourceLossValue());
        assertEquals(ResourceType.HIT_POINTS, result.getResourceLossType());
    }

    @Test
    void theChainStillForwardsSoTheHitsOtherEffectsAreUntouched() {
        CharacterSheet target = newSheet();
        Definhar successor = new Definhar();

        InteractionResult result = new DamageInteraction().postponing().chainInto(successor)
                .applyTo(target, null, DamageType.FISICO, null, RAW_DAMAGE, true, null);

        assertNotNull(result.getNextInteraction(),
                "zeroing the figure would have swallowed every Corrente and Efeito Crítico");
        assertSame(successor, result.getNextInteraction());
    }

    @Test
    void anOrdinaryHitIsUnaffected() {
        CharacterSheet target = newSheet();

        hit(target, new DamageInteraction());

        assertEquals(RAW_DAMAGE, target.getDamageTaken());
    }

    @Test
    void mitigationIsNotChargedTwice() {
        CharacterSheet target = newSheet();
        // ignoreDamageReduction=false, so RD applies once at resolution; the boundary must apply
        // the already-mitigated figure raw rather than re-running the scan.
        int mitigated = new DamageInteraction()
                .applyTo(target, null, DamageType.FISICO, null, RAW_DAMAGE, false, null)
                .getResourceLossValue();

        CharacterSheet postponedTarget = newSheet();
        new DamageInteraction().postponing()
                .applyTo(postponedTarget, null, DamageType.FISICO, null, RAW_DAMAGE, false, null);
        postponedTarget.startNewRound();

        assertEquals(mitigated, postponedTarget.getDamageTaken());
    }
}
