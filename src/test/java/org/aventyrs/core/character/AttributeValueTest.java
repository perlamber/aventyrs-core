package org.aventyrs.core.character;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AttributeValueTest {

    @Test
    void defaultsToBaseOneWithNoBonuses() {
        AttributeValue value = AttributeValue.builder().domain(AttributeDomain.VIGOR).build();
        assertEquals(1, value.getTotal());
    }

    @Test
    void totalSumsAllComponents() {
        AttributeValue value = AttributeValue.builder().domain(AttributeDomain.VIGOR)
                .base(3).fixedRacialBonus(1).variable(2).build();
        assertEquals(6, value.getTotal());
    }

    /**
     * The racial contribution is stored in two halves — what the race dictated and what its player
     * chose — because the sum alone destroys that provenance. {@code getRacialBonus()} reports
     * them together, so a reader that only wants "how much of this is racial" is unaffected.
     */
    @Test
    void theTwoRacialHalvesAreKeptApartButReportedTogether() {
        AttributeValue value = AttributeValue.builder().domain(AttributeDomain.GNOSE)
                .base(2).fixedRacialBonus(1).chosenRacialBonus(2).variable(1).build();

        assertEquals(1, value.getFixedRacialBonus());
        assertEquals(2, value.getChosenRacialBonus());
        assertEquals(3, value.getRacialBonus(), "the sum, for every reader that wants only that");
        assertEquals(6, value.getTotal());
    }

    /** Either half alone still reads as a racial bonus — the question is "racial at all". */
    @Test
    void eitherHalfAloneCountsAsARacialBonus() {
        AttributeValue dictated = AttributeValue.builder().domain(AttributeDomain.VIGOR)
                .fixedRacialBonus(1).build();
        AttributeValue chosen = AttributeValue.builder().domain(AttributeDomain.VIGOR)
                .chosenRacialBonus(1).build();

        assertEquals(1, dictated.getRacialBonus());
        assertEquals(1, chosen.getRacialBonus());
        assertEquals(dictated.getTotal(), chosen.getTotal());
    }
}
