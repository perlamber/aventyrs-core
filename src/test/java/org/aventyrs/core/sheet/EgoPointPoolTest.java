package org.aventyrs.core.sheet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The two-pool arithmetic in isolation, with {@code permanentMax} and the penalty total supplied
 * directly rather than resolved off a sheet — see {@link EgoPointPool}'s own javadoc for the
 * model these pin down.
 */
class EgoPointPoolTest {

    private static final int MAX = 3;
    private static final int NO_PENALTY = 0;

    @Test
    void bothPoolsStartFull() {
        EgoPointPool pool = new EgoPointPool();

        assertEquals(3, pool.getPermanentRemaining(MAX));
        assertEquals(3, pool.getTemporaryCeiling(MAX, NO_PENALTY));
        assertEquals(3, pool.getTemporaryRemaining(MAX, NO_PENALTY));
    }

    @Test
    void theTemporaryCeilingTracksPermanentPointsRemainingNotThePermanentMaximum() {
        EgoPointPool pool = new EgoPointPool();
        pool.spendPermanent(MAX, 2);

        assertEquals(1, pool.getPermanentRemaining(MAX));
        assertEquals(1, pool.getTemporaryCeiling(MAX, NO_PENALTY));
    }

    /**
     * The worked example from {@link EgoPointPool}: spending temporary first yields all 6 points,
     * spending permanent first yields only 3, because the ceiling collapses with it.
     */
    @Test
    void spendingTemporaryFirstYieldsTwiceAsManyPointsAsSpendingPermanentFirst() {
        EgoPointPool temporaryFirst = new EgoPointPool();
        int viaTemporary = temporaryFirst.spendTemporary(MAX, NO_PENALTY, 3)
                + temporaryFirst.spendPermanent(MAX, 3);

        EgoPointPool permanentFirst = new EgoPointPool();
        int viaPermanent = permanentFirst.spendPermanent(MAX, 3)
                + permanentFirst.spendTemporary(MAX, NO_PENALTY, 3);

        assertEquals(6, viaTemporary);
        assertEquals(3, viaPermanent);
    }

    /** A permanent spend keeps the temporary points still held, while they fit under the new ceiling. */
    @Test
    void aPermanentSpendKeepsTheTemporaryPointsStillHeld() {
        EgoPointPool pool = new EgoPointPool();
        pool.spendTemporary(MAX, NO_PENALTY, 2);

        pool.spendPermanent(MAX, 1);

        assertEquals(2, pool.getTemporaryCeiling(MAX, NO_PENALTY));
        assertEquals(1, pool.getTemporaryRemaining(MAX, NO_PENALTY));
    }

    /** ...and clips them when they no longer fit. */
    @Test
    void aPermanentSpendClipsHeldTemporaryPointsDownToTheNewCeiling() {
        EgoPointPool pool = new EgoPointPool();

        pool.spendPermanent(MAX, 2);

        assertEquals(1, pool.getTemporaryRemaining(MAX, NO_PENALTY));
    }

    @Test
    void aSpendReturnsWhatWasActuallySpentNotWhatWasAsked() {
        EgoPointPool pool = new EgoPointPool();

        assertEquals(3, pool.spendTemporary(MAX, NO_PENALTY, 10));
        assertEquals(0, pool.spendTemporary(MAX, NO_PENALTY, 10));
        assertEquals(3, pool.spendPermanent(MAX, 99));
    }

    @Test
    void aNegativeSpendIsTreatedAsZero() {
        EgoPointPool pool = new EgoPointPool();

        assertEquals(0, pool.spendTemporary(MAX, NO_PENALTY, -5));
        assertEquals(3, pool.getTemporaryRemaining(MAX, NO_PENALTY));
    }

    @Test
    void aPenaltyLowersTheCeilingAndTheCeilingReturnsWhenItLifts() {
        EgoPointPool pool = new EgoPointPool();

        assertEquals(1, pool.getTemporaryCeiling(MAX, 2));
        assertEquals(3, pool.getTemporaryCeiling(MAX, NO_PENALTY));
    }

    /**
     * Spend truncated by a falling ceiling is normalized down to it, so the pool doesn't carry a
     * hidden debt that swallows the next recovery.
     */
    @Test
    void temporarySpentIsNormalizedDownWhenTheCeilingFallsBelowIt() {
        EgoPointPool pool = new EgoPointPool();
        pool.spendTemporary(MAX, NO_PENALTY, 3);
        pool.spendPermanent(MAX, 1);

        assertEquals(2, pool.getTemporaryCeiling(MAX, NO_PENALTY));
        assertEquals(0, pool.getTemporaryRemaining(MAX, NO_PENALTY));
        assertEquals(1, pool.recoverTemporary(MAX, NO_PENALTY, 1));
        assertEquals(1, pool.getTemporaryRemaining(MAX, NO_PENALTY));
    }

    /** The un-clamp: an expiring penalty gives back the points it truncated. */
    @Test
    void pointsTruncatedByAPenaltyComeBackOnceItExpires() {
        EgoPointPool pool = new EgoPointPool();
        pool.spendTemporary(MAX, NO_PENALTY, 3);
        assertEquals(0, pool.getTemporaryRemaining(MAX, 2));

        pool.recoverTemporary(MAX, NO_PENALTY, 3);

        assertEquals(3, pool.getTemporaryRemaining(MAX, NO_PENALTY));
    }

    @Test
    void aRecoveryIsBoundedByWhatWasActuallySpent() {
        EgoPointPool pool = new EgoPointPool();
        pool.spendTemporary(MAX, NO_PENALTY, 1);

        assertEquals(1, pool.recoverTemporary(MAX, NO_PENALTY, 10));
        assertEquals(3, pool.getTemporaryRemaining(MAX, NO_PENALTY));
        assertEquals(0, pool.recoverTemporary(MAX, NO_PENALTY, 10));
    }

    @Test
    void aReceivedPointOnAFullPoolIsHeldAsAnExtraAboveTheCeiling() {
        EgoPointPool pool = new EgoPointPool();

        assertEquals(1, pool.receiveTemporary(MAX, NO_PENALTY, "source", 1));
        assertEquals(3, pool.getTemporaryCeiling(MAX, NO_PENALTY));
        assertEquals(4, pool.getTemporaryRemaining(MAX, NO_PENALTY));
        assertEquals(1, pool.getExtras("source"));
    }

    @Test
    void aReceivedPointRefillsBeforeBecomingAnExtra() {
        EgoPointPool pool = new EgoPointPool();
        pool.spendTemporary(MAX, NO_PENALTY, 1);

        pool.receiveTemporary(MAX, NO_PENALTY, "source", 2);

        assertEquals(4, pool.getTemporaryRemaining(MAX, NO_PENALTY));
        assertEquals(1, pool.getExtras());
    }

    @Test
    void extrasAreSpentFirstOldestSourceFirst() {
        EgoPointPool pool = new EgoPointPool();
        pool.receiveTemporary(MAX, NO_PENALTY, "a", 1);
        pool.receiveTemporary(MAX, NO_PENALTY, "b", 1);

        assertEquals(1, pool.spendTemporary(MAX, NO_PENALTY, 1));

        assertEquals(0, pool.getExtras("a"));
        assertEquals(1, pool.getExtras("b"));
        assertEquals(3, pool.getTemporaryCeiling(MAX, NO_PENALTY));
        assertEquals(4, pool.getTemporaryRemaining(MAX, NO_PENALTY));
    }

    @Test
    void aSpendDrawsThroughTheExtrasIntoTheCeiling() {
        EgoPointPool pool = new EgoPointPool();
        pool.receiveTemporary(MAX, NO_PENALTY, "a", 1);

        assertEquals(4, pool.spendTemporary(MAX, NO_PENALTY, 10));
        assertEquals(0, pool.getTemporaryRemaining(MAX, NO_PENALTY));
    }

    @Test
    void aNonCumulativeReceiveGivesNothingWhileTheSourceStillHoldsItsPoint() {
        EgoPointPool pool = new EgoPointPool();
        pool.receiveNonCumulativeTemporary(MAX, NO_PENALTY, "source", 1);

        assertEquals(0, pool.receiveNonCumulativeTemporary(MAX, NO_PENALTY, "source", 1));
        assertEquals(4, pool.getTemporaryRemaining(MAX, NO_PENALTY));
    }

    @Test
    void anUnrelatedSourcesExtrasDoNotBlockANonCumulativeReceive() {
        EgoPointPool pool = new EgoPointPool();
        pool.receiveTemporary(MAX, NO_PENALTY, "other", 1);

        assertEquals(1, pool.receiveNonCumulativeTemporary(MAX, NO_PENALTY, "source", 1));
        assertEquals(5, pool.getTemporaryRemaining(MAX, NO_PENALTY));
    }

    @Test
    void theOverflowIsReceivedOnceAndAgainOnlyForWhatIsNew() {
        EgoPointPool pool = new EgoPointPool();
        pool.syncOverflow(MAX, NO_PENALTY, 2);
        pool.syncOverflow(MAX, NO_PENALTY, 2);
        assertEquals(2, pool.getExtras());

        pool.syncOverflow(MAX, NO_PENALTY, 3);
        assertEquals(3, pool.getExtras());
    }

    @Test
    void removingASourcesExtrasReportsWhatItHeld() {
        EgoPointPool pool = new EgoPointPool();
        pool.addExtra("loan", 1);

        assertEquals(1, pool.removeExtras("loan"));
        assertEquals(0, pool.removeExtras("loan"));
        assertEquals(3, pool.getTemporaryRemaining(MAX, NO_PENALTY));
    }

    @Test
    void everyReadFloorsAtZero() {
        EgoPointPool pool = new EgoPointPool();

        assertEquals(0, pool.getPermanentRemaining(0));
        assertEquals(0, pool.getTemporaryCeiling(MAX, 99));
        assertEquals(0, pool.getTemporaryRemaining(MAX, 99));
    }
}
