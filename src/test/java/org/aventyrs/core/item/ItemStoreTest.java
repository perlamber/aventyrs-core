package org.aventyrs.core.item;

import org.aventyrs.core.sheet.IllegalOperationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemStoreTest {

    @Test
    void aStoreCannotHaveANaturalRarityCeiling() {
        assertThrows(IllegalOperationException.class, () -> new ItemStore(ItemRarity.NATURAL));
        assertThrows(IllegalOperationException.class, () -> new ItemStore(null));
    }

    @Test
    void offeredItemsAreTheCatalogSliceUpToTheCeiling() {
        ItemStore store = new ItemStore(ItemRarity.RARE);

        assertEquals(ItemCatalog.availableUpTo(ItemRarity.RARE), store.getOfferedItems());
        assertTrue(store.getOfferedItems().contains(ArmorItem.ARMADURA_COMPLETA));
        assertFalse(store.getOfferedItems().contains(ArmorItem.ARMADURA_DE_JUSTA));
    }

    @Test
    void offersGatesByRarityAndPurchasability() {
        ItemStore store = new ItemStore(ItemRarity.RARE);

        assertTrue(store.offers(ArmorItem.ARMADURA_COMPLETA)); // Raro
        assertFalse(store.offers(ArmorItem.ARMADURA_DE_JUSTA)); // Épico
        assertFalse(store.offers(NaturalWeapon.values()[0]));   // Natural
    }

    @Test
    void offersRejectsAnyRegaliaSpecificationWhateverItsBase() {
        ItemStore store = new ItemStore(ItemRarity.MYTHIC);

        assertTrue(store.offers(ItemSpecification.of(ArmorItem.ARMADURA_COMPLETA)));
        assertFalse(store.offers(
                ItemSpecification.regalia(ArmorItem.ARMADURA_COMPLETA, RegaliaGrade.MENOR)));
    }

    @Test
    void offeredMasterpiecesAndImprovementsAreTheDefensiveCatalogUpToTheCeiling() {
        ItemStore store = new ItemStore(ItemRarity.RARE);

        assertEquals(
                java.util.Arrays.stream(DefensiveMasterpiece.values())
                        .filter(masterpiece -> masterpiece.getRarity().isAtMost(ItemRarity.RARE))
                        .toList(),
                store.getOfferedMasterpieces());
        assertTrue(store.getOfferedMasterpieces().contains(DefensiveMasterpiece.REFORCADA)); // Comum
        assertFalse(store.getOfferedMasterpieces().contains(DefensiveMasterpiece.MITRAL));   // Épico

        assertTrue(store.getOfferedImprovements().contains(DefensiveImprovement.RESISTENTE)); // Comum
        assertFalse(store.getOfferedImprovements().contains(DefensiveImprovement.ENCAIXE));   // Épico
    }

    @Test
    void offersGatesMasterpiecesAndImprovementsByRarity() {
        ItemStore store = new ItemStore(ItemRarity.RARE);

        assertTrue(store.offers(DefensiveMasterpiece.REFORCADA));
        assertFalse(store.offers(DefensiveMasterpiece.MITRAL));
        assertTrue(store.offers(DefensiveImprovement.RESISTENTE));
        assertFalse(store.offers(DefensiveImprovement.ENCAIXE));
    }

    @Test
    void offersAppliesTheCeilingToEveryPartOfARequestedCopy() {
        ItemStore store = new ItemStore(ItemRarity.RARE);

        assertTrue(store.offers(ItemSpecification.builder()
                .base(ArmorItem.ARMADURA_COMPLETA)
                .masterpiece(ItemMasterpiece.of(DefensiveMasterpiece.REFORCADA))
                .improvement(ItemImprovement.of(DefensiveImprovement.RESISTENTE))
                .build()));

        assertFalse(store.offers(ItemSpecification.builder()
                .base(ArmorItem.ARMADURA_COMPLETA)
                .masterpiece(ItemMasterpiece.of(DefensiveMasterpiece.MITRAL)) // Épico
                .build()));

        assertFalse(store.offers(ItemSpecification.builder()
                .base(ArmorItem.ARMADURA_COMPLETA)
                .improvement(ItemImprovement.of(DefensiveImprovement.ENCAIXE)) // Épico
                .build()));
    }
}
