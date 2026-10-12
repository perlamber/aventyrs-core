package org.aventyrs.core.character.services;

import org.aventyrs.core.ego.EquipmentPointExpense;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemForgery;
import org.aventyrs.core.item.ItemSpecification;
import org.aventyrs.core.item.ItemStore;
import org.aventyrs.core.item.ItemTemplate;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import static org.aventyrs.core.util.TranslatableMessages.ITEM_NOT_OFFERED;

public class ItemPurchaseServiceImpl implements ItemPurchaseService {

    @Override
    public int getPurchasePrice(final ItemStore store, final ItemSpecification spec, final CombatantSheet buyer) {
        requireOffered(store, spec);
        int marketValue = ItemForgery.purchased(spec).getPurchasePrice();
        return EquipmentPointPricing.discounted(marketValue, buyer.getCharacter(), EquipmentPointExpense.PURCHASE);
    }

    @Override
    public int getPurchasePrice(final ItemStore store, final ItemTemplate template, final CombatantSheet buyer) {
        return getPurchasePrice(store, ItemSpecification.of(template), buyer);
    }

    @Override
    public Item purchase(final ItemStore store, final ItemSpecification spec, final CombatantSheet buyer) {
        requireOffered(store, spec);
        ItemForgery forgery = ItemForgery.purchased(spec);
        forgery.validate();
        buyer.spendEquipmentPoints(getPurchasePrice(store, spec, buyer));
        Item copy = forgery.forge();
        buyer.addToInventory(copy);
        return copy;
    }

    @Override
    public Item purchase(final ItemStore store, final ItemTemplate template, final CombatantSheet buyer) {
        return purchase(store, ItemSpecification.of(template), buyer);
    }

    @Override
    public int getPurchasePrice(final ItemStore store, final org.aventyrs.core.item.PowerStoneSpecification spec,
                                final CombatantSheet buyer) {
        if (store == null || !store.offers(spec)) {
            throw new IllegalOperationException(ITEM_NOT_OFFERED);
        }
        return EquipmentPointPricing.discounted(spec.getPrice(), buyer.getCharacter(), EquipmentPointExpense.PURCHASE);
    }

    @Override
    public org.aventyrs.core.item.PowerStone purchase(final ItemStore store,
                                                      final org.aventyrs.core.item.PowerStoneSpecification spec,
                                                      final CombatantSheet buyer) {
        buyer.spendEquipmentPoints(getPurchasePrice(store, spec, buyer));
        org.aventyrs.core.item.PowerStone stone = spec.create();
        buyer.addToInventory(stone);
        return stone;
    }

    private void requireOffered(final ItemStore store, final ItemSpecification spec) {
        if (store == null || !store.offers(spec)) {
            throw new IllegalOperationException(ITEM_NOT_OFFERED);
        }
    }
}
