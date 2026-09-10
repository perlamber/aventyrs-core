package org.aventyrs.core.character.services;

import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemSpecification;
import org.aventyrs.core.item.ItemStore;
import org.aventyrs.core.item.ItemTemplate;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

/**
 * Buying Equipamento from an {@code org.aventyrs.core.scene.Scene}'s {@link ItemStore} —
 * the player's side of a shop transaction.
 *
 * <p>The split mirrors {@link EquipmentCraftingService} over {@code
 * org.aventyrs.core.item.ItemForgery}: the {@link ItemStore} describes <i>what</i> is on sale
 * (its whole {@code ItemCatalog}, up to a Raridade ceiling), and this service <i>sells</i> —
 * it forges the owned copy through {@code ItemForgery.purchased(...)}, prices it, debits the
 * buyer's Pontos de Equipamento ({@link CombatantSheet#spendEquipmentPoints(int)}) and drops
 * the copy into their {@link CombatantSheet#getInventory() inventory}.
 *
 * <p><b>What a store never sells: a Regalia.</b> Otherwise a bought copy can be anything a
 * forged one can — an Obra-Prima with Aprimoramentos — since a {@link ItemSpecification} is the
 * input. A buyer pays <b>full market value</b> ({@code ItemForgery#getPurchasePrice()}), less
 * the {@code ResourcesAdvantage#BARGANHISTA} discount, floored at 1PE.
 *
 * <p><b>This core never rolls dice and tracks no time</b>: a purchase is instantaneous and
 * always succeeds once the buyer can afford it and the store carries the item. The pure getter
 * {@link #getPurchasePrice} hands a caller the figure to show before committing.
 */
public interface ItemPurchaseService {

    /** The least any purchase can cost, in PE — a discount never drives the price below this. */
    int MINIMUM_PURCHASE_PRICE = 1;

    /**
     * What buying spec from store would cost buyer right now, in PE — the copy's full market
     * value less every {@code EgoAdvantage#resolveEquipmentPurchaseDiscount()} the buyer holds
     * ({@code BARGANHISTA}'s -2PE), floored at {@link #MINIMUM_PURCHASE_PRICE}. A pure question:
     * asking never buys anything.
     *
     * @throws IllegalOperationException ({@code ITEM_NOT_OFFERED}) if store does not carry spec
     */
    int getPurchasePrice(ItemStore store, ItemSpecification spec, CombatantSheet buyer);

    /** Convenience for a plain catalog item, forged as-is — {@code ItemSpecification.of(template)}. */
    int getPurchasePrice(ItemStore store, ItemTemplate template, CombatantSheet buyer);

    /**
     * Sells buyer a copy built to spec: validates the store carries it and the spec is no
     * Regalia, debits {@link #getPurchasePrice} from the buyer's PE, forges the copy and adds it
     * to their inventory. Nothing mutates until the PE spend succeeds.
     *
     * @return the forged owned copy (also now in {@code buyer.getInventory()})
     * @throws IllegalOperationException {@code ITEM_NOT_OFFERED} (store doesn't carry it),
     *         {@code STORE_DOES_NOT_SELL_REGALIA}, or {@code NOT_ENOUGH_EQUIPMENT_POINTS}
     */
    Item purchase(ItemStore store, ItemSpecification spec, CombatantSheet buyer);

    /** Convenience for a plain catalog item — {@code purchase(store, ItemSpecification.of(template), buyer)}. */
    Item purchase(ItemStore store, ItemTemplate template, CombatantSheet buyer);
}
