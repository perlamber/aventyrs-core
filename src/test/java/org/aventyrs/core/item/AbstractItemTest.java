package org.aventyrs.core.item;

import org.aventyrs.core.modifier.ModifierType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class AbstractItemTest {

    @Test
    void buildsAOneOffItemWithEveryColumn() {
        AbstractItem item = AbstractItem.builder()
                .name("Adaga Simples")
                .description("Uma lâmina curta e comum.")
                .category(ItemCategory.LIGHT_BLADE)
                .rarity(ItemRarity.COMMON)
                .weightClass(ItemWeightClass.LIGHT)
                .price(2)
                .physicalDefenseBonus(0)
                .magicDefenseBonus(0)
                .hardness(8)
                .castingBonus(1)
                .build();

        assertEquals("Adaga Simples", item.getName());
        assertEquals(ItemType.OFFENSIVE, item.getType());
        assertEquals(2, item.getPrice());
        assertEquals(8, item.getHardness());
        assertEquals(1, item.getCastingBonus());
    }

    @Test
    void grantsNoFavorWhenItCarriesNone() {
        AbstractItem item = AbstractItem.builder().name("Corda").category(ItemCategory.RING).build();

        assertNull(item.getFavor());
        assertFalse(item.grantsFavorTo(null));
        assertEquals(0, item.resolveFavorBonus(ModifierType.DAMAGE_REDUCTION, null));
        assertEquals(List.of(), item.resolveFavorBonuses(null));
    }
}
