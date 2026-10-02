package org.aventyrs.core.sheet;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.EgoValue;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.EgoPointsService;
import org.aventyrs.core.character.services.EgoPointsServiceImpl;
import org.aventyrs.core.ego.AutocontroleAdvantage;
import org.aventyrs.core.effect.Primor;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.skill.CriticalResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end exercise of the two-pool Ego model on a live sheet, in the shape of {@code
 * AttributeSubstitutionFeatureTest} — the pool arithmetic itself is pinned in isolation by
 * {@link EgoPointPoolTest}; this is about the pieces working together through a real
 * {@link CharacterSheet}.
 */
class EgoPointFeatureTest {

    private final EgoPointsService egoPointsService = new EgoPointsServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /** Sorte 3 via {@code variable}, every other Ego left at the fixture's default 2. */
    private CharacterSheet sorteThreeSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .egos(CharacterEgos.builder()
                        .sorte(EgoValue.builder().base(2).variable(1).build())
                        .build())
                .build();
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void sorteThreeOffersThreePermanentAndThreeTemporaryPoints() {
        CharacterSheet sheet = sorteThreeSheet();

        assertEquals(3, sheet.getPermanentEgoPoints(EgoDomain.SORTE));
        assertEquals(3, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
        assertEquals(6, sheet.getAvailableEgoPoints(EgoDomain.SORTE));
    }

    /** The worked example: spending temporary first yields all six points. */
    @Test
    void spendingTemporaryFirstYieldsAllSixPoints() {
        CharacterSheet sheet = sorteThreeSheet();

        int spent = sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.TEMPORARY, 3).getValue()
                + sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.PERMANENT, 3).getValue();

        assertEquals(6, spent);
        assertEquals(0, sheet.getAvailableEgoPoints(EgoDomain.SORTE));
    }

    /** …and spending permanent first yields only three, because the ceiling collapses with it. */
    @Test
    void spendingPermanentFirstYieldsOnlyThreePoints() {
        CharacterSheet sheet = sorteThreeSheet();

        int spent = sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.PERMANENT, 3).getValue()
                + sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.TEMPORARY, 3).getValue();

        assertEquals(3, spent);
        assertEquals(0, sheet.getAvailableEgoPoints(EgoDomain.SORTE));
    }

    @Test
    void aPrimorDrainAndItsRestRefundAndASessionRecoveryComposeOnOneSheet() {
        CharacterSheet sheet = sorteThreeSheet();
        sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.TEMPORARY, 1);
        assertEquals(2, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));

        new Primor(CriticalResult.ACERTO_CRITICO_MAIOR, EgoDomain.SORTE).applyTo(sheet);
        assertEquals(0, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));

        // The Rest returns only Primor's own two points — not the one the holder spent itself.
        sheet.applyPendingEgoRecoveries(RestType.LONGO);
        assertEquals(2, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));

        // A session then returns the last one.
        egoPointsService.applySessionRecovery(sheet, EgoDomain.SORTE);
        assertEquals(3, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    @Test
    void aTemporaryEgoPenaltyLowersTheCeilingButNeverTouchesAHeldExtra() {
        CharacterSheet sheet = sorteThreeSheet();
        sheet.receiveTemporaryEgoPoints(EgoDomain.SORTE, "a-source", 2);
        assertEquals(5, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));

        sheet.applyEffect(new TemporaryEgoPenalty(EgoDomain.SORTE, 2, 1));
        assertEquals(1, sheet.getMaxTemporaryEgoPoints(EgoDomain.SORTE));
        assertEquals(3, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));

        sheet.finishTurn();
        assertEquals(5, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    /** Nothing leaks between the four domains — every other one is untouched throughout. */
    @Test
    void theOtherThreeDomainsAreUnaffectedByEverythingDoneToSorte() {
        CharacterSheet sheet = sorteThreeSheet();
        sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.PERMANENT, 3);
        sheet.receiveTemporaryEgoPoints(EgoDomain.SORTE, "a-source", 2);
        sheet.applyEffect(new TemporaryEgoPenalty(EgoDomain.SORTE, 1, 1));

        for (EgoDomain domain : EgoDomain.values()) {
            if (domain != EgoDomain.SORTE) {
                assertEquals(2, sheet.getPermanentEgoPoints(domain));
                assertEquals(2, sheet.getTemporaryEgoPoints(domain));
            }
        }
    }

    // ---------- received points and extras (table ruling, 0.0.76) ----------

    /**
     * The table's own example: Ego 3 full receives 1 → 4 temporary; spend 2 → 2; receive 1 → back
     * to 3 / 3. The extra was consumed first and is gone — the second point only refills.
     */
    @Test
    void aReceivedPointRefillsFirstAndIsOtherwiseAConsumedExtra() {
        CharacterSheet sheet = sorteThreeSheet();

        sheet.receiveTemporaryEgoPoints(EgoDomain.SORTE, "gm", 1);
        assertEquals(4, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
        assertEquals(3, sheet.getMaxTemporaryEgoPoints(EgoDomain.SORTE));

        sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.TEMPORARY, 2);
        assertEquals(2, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
        assertEquals(0, sheet.getExtraTemporaryEgoPoints(EgoDomain.SORTE));

        sheet.receiveTemporaryEgoPoints(EgoDomain.SORTE, "gm", 1);
        assertEquals(3, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
        assertEquals(3, sheet.getPermanentEgoPoints(EgoDomain.SORTE));
        assertEquals(0, sheet.getExtraTemporaryEgoPoints(EgoDomain.SORTE));
    }

    /** A spent extra is never given back — a recovery only refills under the ceiling. */
    @Test
    void aSpentExtraIsNeverRecovered() {
        CharacterSheet sheet = sorteThreeSheet();
        sheet.receiveTemporaryEgoPoints(EgoDomain.SORTE, "gm", 1);
        sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.TEMPORARY, 1);

        assertEquals(0, sheet.recoverTemporaryEgoPoints(EgoDomain.SORTE, 1));
        assertEquals(3, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    /** Session points only refill up to the base — a full pool gains nothing, and never an extra. */
    @Test
    void sessionRecoveryNeverCreatesAnExtra() {
        CharacterSheet sheet = sorteThreeSheet();

        egoPointsService.applySessionRecovery(sheet, EgoDomain.SORTE);

        assertEquals(3, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
        assertEquals(0, sheet.getExtraTemporaryEgoPoints(EgoDomain.SORTE));
    }

    /** The Narrador's temporary grant is a received point. */
    @Test
    void aNarratorTemporaryGrantIsAReceivedPoint() {
        CharacterSheet sheet = sorteThreeSheet();
        sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.TEMPORARY, 1);

        assertEquals(4, egoPointsService.grantTemporaryByNarrator(sheet, EgoDomain.SORTE, 2));
        assertEquals(1, sheet.getExtraTemporaryEgoPoints(EgoDomain.SORTE));
    }

    /** The Narrador's permanent grant raises the Ego itself, on the rebuilt Character. */
    @Test
    void aNarratorPermanentGrantRaisesTheEgoOnTheRebuiltCharacter() {
        Character raised = egoPointsService.grantPermanentByNarrator(
                sorteThreeSheet().getCharacter(), EgoDomain.SORTE, 1);

        CharacterSheet sheet = CharacterSheet.of(raised, new Player());
        assertEquals(4, sheet.getPermanentEgoPoints(EgoDomain.SORTE));
        assertEquals(4, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
        assertEquals(2, raised.getEgos().getSorte().getBase());
    }

    // ---------- no Ego above 5 ----------

    private CharacterSheet recursosSevenSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .egos(CharacterEgos.builder()
                        .recursos(EgoValue.builder().base(3).variable(4).build())
                        .build())
                .build();
        return CharacterSheet.of(character, new Player());
    }

    /** Recursos 7 = Recursos 5 + 2 extra temporary points, and counts as 5 everywhere else. */
    @Test
    void anEgoPastFiveCountsAsFiveAndHoldsTheRestAsExtras() {
        CharacterSheet sheet = recursosSevenSheet();

        assertEquals(5, sheet.getCharacter().getEffectiveEgoTotal(EgoDomain.RECURSOS));
        assertEquals(2, sheet.getCharacter().getEgoOverflow(EgoDomain.RECURSOS));
        assertEquals(5, sheet.getPermanentEgoPoints(EgoDomain.RECURSOS));
        assertEquals(5, sheet.getMaxTemporaryEgoPoints(EgoDomain.RECURSOS));
        assertEquals(7, sheet.getTemporaryEgoPoints(EgoDomain.RECURSOS));
        assertEquals(2, sheet.getExtraTemporaryEgoPoints(EgoDomain.RECURSOS));
    }

    /** The overflow is received once: spent, it is gone, and no later read hands it over again. */
    @Test
    void theOverflowIsReceivedOnlyOnce() {
        CharacterSheet sheet = recursosSevenSheet();
        sheet.spendEgoPoints(EgoDomain.RECURSOS, EgoPointType.TEMPORARY, 2);

        assertEquals(0, sheet.recoverTemporaryEgoPoints(EgoDomain.RECURSOS, 2));

        assertEquals(5, sheet.getTemporaryEgoPoints(EgoDomain.RECURSOS));
        assertEquals(2, sheet.getEgoOverflowReceived(EgoDomain.RECURSOS));
    }

    /** A rebuilt sheet restored from persisted state doesn't receive its overflow afresh. */
    @Test
    void restoringExtrasReplacesTheFreshSheetsOverflow() {
        CharacterSheet sheet = recursosSevenSheet();

        sheet.restoreExtraTemporaryEgoPoints(EgoDomain.RECURSOS, 0, 2);

        assertEquals(5, sheet.getTemporaryEgoPoints(EgoDomain.RECURSOS));
        assertEquals(0, sheet.getExtraTemporaryEgoPoints(EgoDomain.RECURSOS));
    }

    /** A point earned past 5 on a restored sheet still arrives — only the unreceived part is owed. */
    @Test
    void restoringAnOlderOverflowMarkReceivesOnlyTheNewPart() {
        CharacterSheet sheet = recursosSevenSheet();

        sheet.restoreExtraTemporaryEgoPoints(EgoDomain.RECURSOS, 0, 1);

        assertEquals(1, sheet.getExtraTemporaryEgoPoints(EgoDomain.RECURSOS));
        assertEquals(2, sheet.getEgoOverflowReceived(EgoDomain.RECURSOS));
    }

    /**
     * The distinction {@code EgoPointsService#useEgoPointsForEffect} exists to draw: DETERMINACAO
     * _HEROICA rewards <em>using</em> Autocontrole points, so a Primor drain — points taken from
     * the victim against their will — must not heal the character it just crit.
     */
    @Test
    void aPrimorDrainDoesNotTriggerDeterminacaoHeroicasRecovery() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .egoAdvantage(EgoDomain.AUTOCONTROLE, AutocontroleAdvantage.DETERMINACAO_HEROICA)
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.applyDamage(10);

        new Primor(CriticalResult.ACERTO_CRITICO_MAIOR, EgoDomain.AUTOCONTROLE).applyTo(sheet);

        // The drain landed for real (2 points gone) — and healed nothing.
        assertEquals(0, sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
        assertEquals(10, sheet.getDamageTaken());
    }

    /** …while the same character deliberately using a point is healed for the rolled value. */
    @Test
    void deliberatelyUsingAnAutocontrolePointDoesTriggerTheRecovery() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .egoAdvantage(EgoDomain.AUTOCONTROLE, AutocontroleAdvantage.DETERMINACAO_HEROICA)
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.applyDamage(10);

        new EgoPointsServiceImpl().useEgoPointsForEffect(
                sheet, EgoDomain.AUTOCONTROLE, EgoPointType.TEMPORARY, 1, 3);

        assertEquals(7, sheet.getDamageTaken());
    }
}
