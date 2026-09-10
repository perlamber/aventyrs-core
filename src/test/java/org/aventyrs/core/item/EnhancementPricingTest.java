package org.aventyrs.core.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The two price grids under "Itens Obras-Primas" in {@code docs/rules/equipamentos.txt}, row by
 * row. Every figure here is authored data, so the tests are transcriptions rather than
 * derivations — nothing about the ladder is computable (Armas overtakes Armaduras at Raro and
 * they meet again at Mítico).
 */
class EnhancementPricingTest {

    @Test
    void masterpiecePricesMatchTheArmasColumn() {
        assertEquals(6, EnhancementPricing.masterpiecePrice(ItemRarity.COMMON, EnhancementPriceCategory.WEAPON));
        assertEquals(12, EnhancementPricing.masterpiecePrice(ItemRarity.UNCOMMON, EnhancementPriceCategory.WEAPON));
        assertEquals(24, EnhancementPricing.masterpiecePrice(ItemRarity.RARE, EnhancementPriceCategory.WEAPON));
        assertEquals(36, EnhancementPricing.masterpiecePrice(ItemRarity.EPIC, EnhancementPriceCategory.WEAPON));
        assertEquals(48, EnhancementPricing.masterpiecePrice(ItemRarity.MYTHIC, EnhancementPriceCategory.WEAPON));
    }

    @Test
    void masterpiecePricesMatchTheArmadurasColumn() {
        assertEquals(8, EnhancementPricing.masterpiecePrice(ItemRarity.COMMON, EnhancementPriceCategory.ARMOR));
        assertEquals(12, EnhancementPricing.masterpiecePrice(ItemRarity.UNCOMMON, EnhancementPriceCategory.ARMOR));
        assertEquals(20, EnhancementPricing.masterpiecePrice(ItemRarity.RARE, EnhancementPriceCategory.ARMOR));
        assertEquals(32, EnhancementPricing.masterpiecePrice(ItemRarity.EPIC, EnhancementPriceCategory.ARMOR));
        assertEquals(48, EnhancementPricing.masterpiecePrice(ItemRarity.MYTHIC, EnhancementPriceCategory.ARMOR));
    }

    @Test
    void masterpiecePricesMatchThePedrasDoPoderColumn() {
        assertEquals(4, EnhancementPricing.masterpiecePrice(ItemRarity.COMMON, EnhancementPriceCategory.POWER_STONE));
        assertEquals(6, EnhancementPricing.masterpiecePrice(ItemRarity.UNCOMMON, EnhancementPriceCategory.POWER_STONE));
        assertEquals(9, EnhancementPricing.masterpiecePrice(ItemRarity.RARE, EnhancementPriceCategory.POWER_STONE));
        assertEquals(13, EnhancementPricing.masterpiecePrice(ItemRarity.EPIC, EnhancementPriceCategory.POWER_STONE));
        assertEquals(18, EnhancementPricing.masterpiecePrice(ItemRarity.MYTHIC, EnhancementPriceCategory.POWER_STONE));
    }

    @Test
    void improvementPricesMatchTheArmasColumn() {
        assertEquals(4, EnhancementPricing.improvementPrice(ItemRarity.COMMON, EnhancementPriceCategory.WEAPON));
        assertEquals(8, EnhancementPricing.improvementPrice(ItemRarity.UNCOMMON, EnhancementPriceCategory.WEAPON));
        assertEquals(13, EnhancementPricing.improvementPrice(ItemRarity.RARE, EnhancementPriceCategory.WEAPON));
        assertEquals(19, EnhancementPricing.improvementPrice(ItemRarity.EPIC, EnhancementPriceCategory.WEAPON));
        assertEquals(26, EnhancementPricing.improvementPrice(ItemRarity.MYTHIC, EnhancementPriceCategory.WEAPON));
    }

    @Test
    void improvementPricesMatchTheArmadurasColumn() {
        assertEquals(5, EnhancementPricing.improvementPrice(ItemRarity.COMMON, EnhancementPriceCategory.ARMOR));
        assertEquals(8, EnhancementPricing.improvementPrice(ItemRarity.UNCOMMON, EnhancementPriceCategory.ARMOR));
        assertEquals(12, EnhancementPricing.improvementPrice(ItemRarity.RARE, EnhancementPriceCategory.ARMOR));
        assertEquals(18, EnhancementPricing.improvementPrice(ItemRarity.EPIC, EnhancementPriceCategory.ARMOR));
        assertEquals(24, EnhancementPricing.improvementPrice(ItemRarity.MYTHIC, EnhancementPriceCategory.ARMOR));
    }

    @Test
    void improvementPricesMatchThePedrasDoPoderColumn() {
        assertEquals(2, EnhancementPricing.improvementPrice(ItemRarity.COMMON, EnhancementPriceCategory.POWER_STONE));
        assertEquals(4, EnhancementPricing.improvementPrice(ItemRarity.UNCOMMON, EnhancementPriceCategory.POWER_STONE));
        assertEquals(7, EnhancementPricing.improvementPrice(ItemRarity.RARE, EnhancementPriceCategory.POWER_STONE));
        assertEquals(11, EnhancementPricing.improvementPrice(ItemRarity.EPIC, EnhancementPriceCategory.POWER_STONE));
        assertEquals(16, EnhancementPricing.improvementPrice(ItemRarity.MYTHIC, EnhancementPriceCategory.POWER_STONE));
    }

    /** An Aprimoramento is always cheaper than an Obra-Prima of the same tier and column. */
    @Test
    void anAprimoramentoIsCheaperThanAnObraPrimaOfTheSameTier() {
        for (ItemRarity rarity : ItemRarity.values()) {
            if (rarity == ItemRarity.NATURAL) {
                continue;
            }
            for (EnhancementPriceCategory category : EnhancementPriceCategory.values()) {
                assertEquals(true,
                        EnhancementPricing.improvementPrice(rarity, category)
                                < EnhancementPricing.masterpiecePrice(rarity, category),
                        rarity + "/" + category);
            }
        }
    }

    /** A body part is never master-crafted, so neither table has a row for it. */
    @Test
    void naturalEquipamentoHasNoPriceInEitherTable() {
        assertThrows(IllegalStateException.class,
                () -> EnhancementPricing.masterpiecePrice(ItemRarity.NATURAL, EnhancementPriceCategory.WEAPON));
        assertThrows(IllegalStateException.class,
                () -> EnhancementPricing.improvementPrice(ItemRarity.NATURAL, EnhancementPriceCategory.WEAPON));
    }
}
