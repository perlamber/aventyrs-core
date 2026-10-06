package org.aventyrs.core.title.bruxo;

import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.MagicType;
import org.aventyrs.core.magic.catalog.MagicTree;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MisticismoFilterTest {

    private final MisticismoFilter ventoOuDivina = MisticismoFilter.elementOr(ElementalType.VENTO, MagicType.DIVINA);

    @Test
    void anyAdmitsEveryTree() {
        for (MagicTree tree : MagicTree.values()) {
            assertTrue(MisticismoFilter.ANY.admits(tree), tree.name());
        }
    }

    @Test
    void elementMatchesTheTreesElement() {
        assertTrue(ventoOuDivina.admits(MagicTree.VOO));
        assertFalse(ventoOuDivina.admits(MagicTree.FURIA_DE_TESLA));
    }

    @Test
    void eitherHalfOfTheTagSatisfiesTheType() {
        // Piromancia is Divina/Elemental: Fogo — Divina as its first half.
        assertTrue(ventoOuDivina.admits(MagicTree.PIROMANCIA));
        // Vida is Natural/Divina — Divina as its second half.
        assertTrue(ventoOuDivina.admits(MagicTree.VIDA));
    }

    @Test
    void arsenalElementalSatisfiesEveryElement() {
        assertTrue(ventoOuDivina.admits(MagicTree.ARSENAL_ELEMENTAL));
        assertTrue(MisticismoFilter.elementOr(ElementalType.GELO, MagicType.DIVINA).admits(MagicTree.ARSENAL_ELEMENTAL));
    }

    @Test
    void elementalNaturalAlsoAdmitsAStandaloneNaturalTree() {
        MisticismoFilter natural = MisticismoFilter.elementOr(ElementalType.NATURAL, MagicType.DIVINA);

        assertTrue(natural.admits(MagicTree.ALIADOS_DA_NATUREZA));
        assertTrue(natural.admits(MagicTree.POLIMORFISMO));
        assertFalse(natural.admits(MagicTree.TEMPO));
    }

    @Test
    void invocationFiltersSplitTheCatalog() {
        assertTrue(MisticismoFilter.INVOCATION.admits(MagicTree.ALIADOS_DA_NATUREZA));
        assertTrue(MisticismoFilter.INVOCATION.admits(MagicTree.REANIMAR));
        assertTrue(MisticismoFilter.INVOCATION.admits(MagicTree.TRANSPORTE));
        assertFalse(MisticismoFilter.INVOCATION.admits(MagicTree.VIDA));

        assertFalse(MisticismoFilter.NON_INVOCATION.admits(MagicTree.ALIADOS_DA_NATUREZA));
        assertTrue(MisticismoFilter.NON_INVOCATION.admits(MagicTree.VIDA));
    }
}
