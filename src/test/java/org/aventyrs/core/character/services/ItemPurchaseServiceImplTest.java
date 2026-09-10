package org.aventyrs.core.character.services;

import java.util.ArrayList;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.ego.ResourcesAdvantage;
import org.aventyrs.core.item.ArmorItem;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemRarity;
import org.aventyrs.core.item.ItemStore;
import org.aventyrs.core.item.ItemTemplate;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemPurchaseServiceImplTest {

    private final ItemPurchaseService purchases = new ItemPurchaseServiceImpl();
    private final ItemStore store = new ItemStore(ItemRarity.RARE);

    /** Comum, Preço 5. */
    private static final ItemTemplate GLADIADOR = ArmorItem.ARMADURA_DE_GLADIADOR;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet buyer(final int equipmentPoints) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.grantEquipmentPoints(equipmentPoints);
        return sheet;
    }

    private static CharacterSheet barganhista(final int equipmentPoints) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .egoAdvantage(EgoDomain.RECURSOS, ResourcesAdvantage.BARGANHISTA)
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.grantEquipmentPoints(equipmentPoints);
        return sheet;
    }

    @Test
    void aPurchaseDebitsPeAndDropsADistinctCopyInTheInventory() {
        CharacterSheet buyer = buyer(20);

        Item copy = purchases.purchase(store, GLADIADOR, buyer);

        assertEquals(15, buyer.getEquipmentPoints());
        assertNotSame(GLADIADOR, copy);
        assertEquals(0, copy.getDamageTaken());
        assertTrue(buyer.getInventory().contains(copy));
    }

    @Test
    void anUnaffordablePurchaseChangesNothing() {
        CharacterSheet buyer = buyer(4); // Gladiador costs 5

        assertThrows(IllegalOperationException.class, () -> purchases.purchase(store, GLADIADOR, buyer));
        assertEquals(4, buyer.getEquipmentPoints());
        assertTrue(buyer.getInventory().isEmpty());
    }

    @Test
    void aStoreDoesNotSellAboveItsRarityCeiling() {
        CharacterSheet buyer = buyer(100);

        assertThrows(IllegalOperationException.class,
                () -> purchases.purchase(store, ArmorItem.ARMADURA_DE_JUSTA, buyer)); // Épico
        assertEquals(100, buyer.getEquipmentPoints());
    }

    @Test
    void barganhistaShavesTwoPeOffTheMarketPrice() {
        assertEquals(GLADIADOR.getPrice() - 2, purchases.getPurchasePrice(store, GLADIADOR, barganhista(0)));
        assertEquals(GLADIADOR.getPrice(), purchases.getPurchasePrice(store, GLADIADOR, buyer(0)));
    }

    @Test
    void theDiscountNeverDrivesThePriceBelowOnePe() {
        // Roupa Pesada — Preço 3; -2 would be 1, and that is the floor anyway
        ItemTemplate roupaPesada = ArmorItem.ROUPA_PESADA;
        int price = purchases.getPurchasePrice(store, roupaPesada, barganhista(0));

        assertTrue(price >= ItemPurchaseService.MINIMUM_PURCHASE_PRICE);
        assertFalse(price < 1);
    }
}
