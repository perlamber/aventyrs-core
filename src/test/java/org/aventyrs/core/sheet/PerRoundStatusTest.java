package org.aventyrs.core.sheet;

import java.util.Optional;

import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The per-Rodada damage path and its effect on the reported {@link CharacterStatus} tier.
 *
 * <p>This is the case that motivated deriving status rather than storing it. The whole loop —
 * {@code Scene#next()} to {@link CombatantSheet#finishTurn()} to {@link
 * CombatantSheet#tickTemporaryEffects()} to {@link Bleeding}/{@link Withering}'s own
 * {@code applyRoundEffect} — holds no {@code DamageService} at any level, so there was nothing
 * anywhere in it that could have refreshed a stored tier. A bleeding character could cross every
 * tier down to {@code DEAD} while the stored value stayed at whatever the last mitigated hit
 * wrote, or at {@code CLEAN} if there had never been one.
 *
 * <p>Everything below goes through {@code finishTurn()} rather than calling {@code
 * tickTemporaryEffects()} directly, so it exercises the entry point {@code Scene#next()}
 * actually uses.
 */
class PerRoundStatusTest {

    /** Vigor 1, no {@code LIFE_MULTIPLIER} source: {@code 10 + 1 * 4}. */
    private static final int BLANK_MAX_HIT_POINTS = 14;

    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet blankSheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }

    /**
     * Starts at 9 damage — 5 PV left, {@code MEDIUM_LIFE} — and bleeds 1 per Rodada. The tier
     * has to move on its own as the Rodadas pass, with nothing but {@code finishTurn()} driving
     * it.
     */
    @Test
    void bleedingTicksCarryTheDerivedStatusDownThroughTiers() {
        CharacterSheet sheet = blankSheet();
        sheet.applyDamage(9);
        assertEquals(CharacterStatus.MEDIUM_LIFE, hitPointsService.getStatus(sheet));

        sheet.applyEffect(new Bleeding(1, Optional.empty()));

        // 10 damage -> 4 PV, under the one-third threshold.
        sheet.finishTurn();
        assertEquals(10, sheet.getDamageTaken());
        assertEquals(CharacterStatus.LOW_LIFE, hitPointsService.getStatus(sheet));

        // 11, 12, 13 damage -> 3, 2, 1 PV, all still above zero.
        for (int round = 0; round < 3; round++) {
            sheet.finishTurn();
            assertEquals(CharacterStatus.LOW_LIFE, hitPointsService.getStatus(sheet));
        }

        // 14 damage -> exactly 0 PV, which is where FALLEN starts.
        sheet.finishTurn();
        assertEquals(BLANK_MAX_HIT_POINTS, sheet.getDamageTaken());
        assertEquals(CharacterStatus.FALLEN, hitPointsService.getStatus(sheet));
    }

    /**
     * Withering drains through {@link CombatantSheet#applyCurseDamage}, which bypasses Shield
     * entirely — and, like Bleeding, reaches the sheet with no service in scope. One Rodada is
     * enough here to cross the last boundary there is.
     */
    @Test
    void witheringCurseTicksReachDeadWithNoDamageServiceInvolved() {
        CharacterSheet sheet = blankSheet();
        sheet.applyCurseDamage(2 * BLANK_MAX_HIT_POINTS - 1);
        assertEquals(CharacterStatus.COMMA, hitPointsService.getStatus(sheet));

        sheet.applyEffect(new Withering(1, Optional.empty()));
        sheet.finishTurn();

        assertEquals(2 * BLANK_MAX_HIT_POINTS, sheet.getDamageTaken());
        assertEquals(CharacterStatus.DEAD, hitPointsService.getStatus(sheet));
    }

    /**
     * Shield absorbs Bleeding's own per-Rodada loss the same way it absorbs a hit (it goes
     * through {@link CombatantSheet#applyDamage}), so a shielded character's tier holds still
     * while the Shield lasts and only starts moving once it is gone.
     */
    @Test
    void bleedingMovesNoTierWhileShieldPointsStillAbsorbIt() {
        CharacterSheet sheet = blankSheet();
        sheet.addShield(2);
        sheet.applyEffect(new Bleeding(1, Optional.empty()));

        sheet.finishTurn();
        sheet.finishTurn();
        assertEquals(0, sheet.getDamageTaken());
        assertEquals(CharacterStatus.CLEAN, hitPointsService.getStatus(sheet));

        sheet.finishTurn();
        assertEquals(1, sheet.getDamageTaken());
        assertEquals(CharacterStatus.HIGH_LIFE, hitPointsService.getStatus(sheet));
    }
}
