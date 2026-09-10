package org.aventyrs.core.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The forge's input value — its defaults, its two shorthands, and the fact that it validates
 * nothing (that is {@link ItemForgery}'s job, exercised in {@code ItemForgeryTest}).
 */
class ItemSpecificationTest {

    @Test
    void theShorthandIsJustACatalogItemForgedAsIs() {
        ItemSpecification specification = ItemSpecification.of(ArmorItem.ARMADURA_COMPLETA);

        assertSame(ArmorItem.ARMADURA_COMPLETA, specification.getBase());
        assertNull(specification.getMasterpiece());
        assertNull(specification.getRegaliaGrade());
        assertTrue(specification.getImprovements().isEmpty());
        assertFalse(specification.isRegalia());
    }

    @Test
    void aRegaliaSpecificationCarriesItsGrade() {
        ItemSpecification specification =
                ItemSpecification.regalia(ArmorItem.COURACA, RegaliaGrade.SUPERIOR);

        assertEquals(RegaliaGrade.SUPERIOR, specification.getRegaliaGrade());
        assertTrue(specification.isRegalia());
    }

    @Test
    void everyColumnSurvivesTheBuilder() {
        ItemMasterpiece masterpiece = ItemMasterpiece.of(DefensiveMasterpiece.REFORCADA);
        ItemSpecification specification = ItemSpecification.builder()
                .base(ArmorItem.ARMADURA_COMPLETA)
                .masterpiece(masterpiece)
                .improvement(ItemImprovement.of(DefensiveImprovement.RESISTENTE))
                .improvement(ItemImprovement.of(DefensiveImprovement.AJUSTADA))
                .regaliaGrade(RegaliaGrade.MENOR)
                .build();

        assertSame(masterpiece, specification.getMasterpiece());
        assertEquals(2, specification.getImprovements().size());
        assertEquals(RegaliaGrade.MENOR, specification.getRegaliaGrade());
    }

    /** A base is the one column with no default — a specification without one is not one. */
    @Test
    void theBaseIsRequired() {
        assertThrows(NullPointerException.class, () -> ItemSpecification.builder().build());
    }

    /**
     * It describes; it does not judge. Three Aprimoramentos on a Leve armor with no Obra-Prima to
     * host them builds fine here, and is refused at the forge.
     */
    @Test
    void anImpossibleSpecificationStillBuilds() {
        ItemSpecification specification = ItemSpecification.builder()
                .base(ArmorItem.ROUPA_PESADA)
                .improvement(ItemImprovement.of(DefensiveImprovement.RESISTENTE))
                .improvement(ItemImprovement.of(DefensiveImprovement.AJUSTADA))
                .improvement(ItemImprovement.of(DefensiveImprovement.OCULTA))
                .build();

        assertEquals(3, specification.getImprovements().size());
        assertEquals(1, specification.getBase().getWeightClass().getMaximumImprovements());
    }

    @Test
    void toBuilderReusesASpecificationWithOneColumnChanged() {
        ItemSpecification menor = ItemSpecification.regalia(ArmorItem.COURACA, RegaliaGrade.MENOR);

        ItemSpecification divina = menor.toBuilder().regaliaGrade(RegaliaGrade.DIVINA).build();

        assertEquals(RegaliaGrade.MENOR, menor.getRegaliaGrade());
        assertEquals(RegaliaGrade.DIVINA, divina.getRegaliaGrade());
        assertSame(menor.getBase(), divina.getBase());
    }
}
