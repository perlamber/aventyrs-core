package org.aventyrs.core.sheet;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.EgoValue;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.magic.MimetizedSpellCastingServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.util.TranslatableMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The title-agnostic sheet machinery 0.0.51 added: time passing, true rests, delayed recoveries, the damage floor. */
class GameTimeAndEgoCostTest {

    private CharacterSheet sheet;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .egos(CharacterEgos.builder().autocontrole(EgoValue.builder().base(4).build()).build())
                .build();
        sheet = CharacterSheet.of(character, new Player());
    }

    @Test
    void passingHoursEndsRodadaCountedStatesButNotOpenEndedOnes() {
        sheet.grantTemporaryBonus(ModifierType.DEFESAS, 2, 10);
        sheet.applyEffect(new Exhaustion("test"));

        sheet.passHours(1);

        assertEquals(-2, sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
        assertEquals(0, sheet.getTemporaryBonus(ModifierType.DEFESAS));
    }

    @Test
    void hourlyRecoveryBanksPartialHours() {
        sheet.spendEgoPoints(EgoDomain.AUTOCONTROLE, EgoPointType.TEMPORARY, 3);
        sheet.oweHourlyEgoRecovery(EgoDomain.AUTOCONTROLE, 3, 2);

        sheet.passHours(1);
        sheet.passHours(1);
        assertEquals(2, sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
        sheet.passHours(3);
        assertEquals(3, sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
        assertEquals(1, sheet.getOwedHourlyEgoRecovery(EgoDomain.AUTOCONTROLE));
    }

    @Test
    void anEffectUntilATrueRestIgnoresARestOfAWeakerTier() {
        sheet.applyEffectUntilTrueRest(new Exhaustion("test"), RestType.CURTO);

        sheet.completeTrueRest(RestType.MINIMO);
        assertEquals(-2, sheet.getTemporaryBonus(ModifierType.DAMAGE_ROLL_BONUS));
        sheet.completeTrueRest(RestType.LONGO);
        assertEquals(0, sheet.getTemporaryBonus(ModifierType.DAMAGE_ROLL_BONUS));
    }

    @Test
    void aGuardedDelayedGrantWaitsItsRoundsAndIsDroppedWhenTheGuardFails() {
        sheet.spendEgoPoints(EgoDomain.AUTOCONTROLE, EgoPointType.TEMPORARY, 2);
        sheet.scheduleTemporaryEgoPointGrant(new DelayedEgoGrant(EgoDomain.AUTOCONTROLE, "a", 1, 2, s -> true, true));
        sheet.scheduleTemporaryEgoPointGrant(new DelayedEgoGrant(EgoDomain.AUTOCONTROLE, "b", 1, 1, s -> false, true));

        sheet.startNewRound();
        assertEquals(2, sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
        sheet.startNewRound();
        assertEquals(3, sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
    }

    @Test
    void theDamageFloorCatchesOnlyTheNextHit() {
        int max = new HitPointsServiceImpl().getMaxHitPoints(sheet.getCharacter(), sheet);
        sheet.floorNextDamageAt(1);

        sheet.applyDamage(max * 3);
        assertEquals(max - 1, sheet.getDamageTaken());
        sheet.applyDamage(5);
        assertEquals(max + 4, sheet.getDamageTaken());
    }

    @Test
    void wouldDropToZeroOnlyFromAbove() {
        int max = new HitPointsServiceImpl().getMaxHitPoints(sheet.getCharacter(), sheet);
        var damage = new DamageServiceImpl();

        assertFalse(damage.wouldDropToZeroOrBelow(sheet, max - 1));
        assertTrue(damage.wouldDropToZeroOrBelow(sheet, max));
        sheet.applyDamage(max);
        assertFalse(damage.wouldDropToZeroOrBelow(sheet, 3));
    }

    @Test
    void aFrenesiForbidsMimetizing() {
        sheet.startFrenzy(new Frenzy(sheet.getId(), 2));

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> new MimetizedSpellCastingServiceImpl().cast(sheet, null));
        assertEquals(TranslatableMessages.SPELL_CASTING_PREVENTED, refused.getMessage());
    }

    @Test
    void hourlyDebtAndExhaustionRoundTripForPersistence() {
        sheet.spendEgoPoints(EgoDomain.AUTOCONTROLE, EgoPointType.TEMPORARY, 2);
        sheet.oweHourlyEgoRecovery(EgoDomain.AUTOCONTROLE, 2, 2);
        sheet.passHours(1);
        sheet.applyEffectUntilTrueRest(new Exhaustion("test"), RestType.CURTO);

        CharacterSheet restored = CharacterSheet.of(sheet.getCharacter(), new Player());
        sheet.getHourlyEgoRecoveries().forEach(restored::restoreHourlyEgoRecovery);
        restored.spendEgoPoints(EgoDomain.AUTOCONTROLE, EgoPointType.TEMPORARY, 2);
        restored.passHours(1);

        assertTrue(sheet.isExhausted());
        assertFalse(restored.isExhausted());
        assertEquals(3, restored.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
    }
}
