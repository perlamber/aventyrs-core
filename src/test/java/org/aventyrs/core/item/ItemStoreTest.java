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
}
