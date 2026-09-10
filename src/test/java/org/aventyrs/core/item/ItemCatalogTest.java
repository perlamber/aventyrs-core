package org.aventyrs.core.item;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemCatalogTest {

    @Test
    void allGathersEveryConstantOfEveryRegisteredCategoryEnum() {
        List<ItemTemplate> all = ItemCatalog.all();

        assertTrue(all.containsAll(List.of(ArmorItem.values())));
        assertTrue(all.containsAll(List.of(BootsItem.values())));
        assertTrue(all.containsAll(List.of(CloakItem.values())));
        assertTrue(all.containsAll(List.of(ShieldItem.values())));
        assertTrue(all.containsAll(List.of(HelmetItem.values())));
        assertTrue(all.containsAll(List.of(NaturalWeapon.values())));
        assertTrue(all.containsAll(List.of(BowItem.values())));
        assertTrue(all.containsAll(List.of(ThrowableItem.values())));
        assertTrue(all.containsAll(List.of(CrossbowItem.values())));
        assertTrue(all.containsAll(List.of(WhipItem.values())));
        assertTrue(all.containsAll(List.of(ClubItem.values())));
        assertTrue(all.containsAll(List.of(LightBladeItem.values())));
        assertTrue(all.containsAll(List.of(HeavyBladeItem.values())));
        assertTrue(all.containsAll(List.of(SpearItem.values())));
        assertEquals(ArmorItem.values().length + BootsItem.values().length
                + CloakItem.values().length + ShieldItem.values().length
                + HelmetItem.values().length + NaturalWeapon.values().length
                + BowItem.values().length + ThrowableItem.values().length
                + CrossbowItem.values().length + WhipItem.values().length
                + ClubItem.values().length + LightBladeItem.values().length
                + HeavyBladeItem.values().length + SpearItem.values().length, all.size());
    }

    @Test
    void purchasableDropsEveryNaturalEquipmentEntry() {
        List<ItemTemplate> purchasable = ItemCatalog.purchasable();

        assertTrue(purchasable.stream().noneMatch(template -> template.getRarity() == ItemRarity.NATURAL));
        assertFalse(purchasable.stream().anyMatch(NaturalWeapon.class::isInstance));
        assertTrue(purchasable.contains(ArmorItem.ARMADURA_DE_GLADIADOR));
    }

    @Test
    void availableUpToKeepsOnlyEntriesNoRarerThanTheCeiling() {
        List<ItemTemplate> common = ItemCatalog.availableUpTo(ItemRarity.COMMON);
        assertTrue(common.contains(ArmorItem.ARMADURA_DE_GLADIADOR)); // Comum
        assertFalse(common.contains(ArmorItem.ARMADURA_COMPLETA));    // Raro
        assertFalse(common.contains(ArmorItem.ARMADURA_DE_JUSTA));    // Épico

        List<ItemTemplate> epic = ItemCatalog.availableUpTo(ItemRarity.EPIC);
        assertTrue(epic.contains(ArmorItem.ARMADURA_COMPLETA));
        assertTrue(epic.contains(ArmorItem.ARMADURA_DE_JUSTA));
    }
}
