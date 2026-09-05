package org.aventyrs.core.item;

import java.util.List;

import org.aventyrs.core.sheet.IllegalOperationException;

import static org.aventyrs.core.util.TranslatableMessages.STORE_RARITY_NOT_PURCHASABLE;

/**
 * A place a character can buy Equipamento during play — held by a {@code
 * org.aventyrs.core.scene.Scene} that has one (a town, a travelling merchant), {@code null} on a
 * Scene that doesn't (a dungeon corridor), exactly like {@code Scene#getTerrainType()}.
 *
 * <p><b>Not a stocked shelf.</b> A store carries no list of individual items: it offers the
 * <em>whole</em> {@link ItemCatalog}, on demand and in unlimited quantity, capped only by
 * {@link #getMaxRarity()} — the one property that distinguishes a village smithy from a
 * capital-city arsenal. {@link #getOfferedItems()} resolves that slice fresh each call.
 *
 * <p><b>The store describes the offering; it does not sell.</b> Producing the owned {@link Item}
 * copy, pricing it and debiting the buyer's Pontos de Equipamento is {@code
 * org.aventyrs.core.character.services.ItemPurchaseService}'s job — the same split {@code
 * EquipmentCraftingService} keeps over {@link ItemForgery}. A purchase runs through {@link
 * ItemForgery#purchased(ItemSpecification)}, so a store copy can be an Obra-Prima with
 * Aprimoramentos; the one thing a store never sells is a Regalia.
 */
public class ItemStore {

    private final ItemRarity maxRarity;

    /**
     * @param maxRarity the rarest tier this store carries — must be a purchasable tier
     *                  ({@link ItemRarity#isPurchasable()}); {@link ItemRarity#NATURAL} is a
     *                  body-part marker, not a stock ceiling.
     * @throws IllegalOperationException ({@code STORE_RARITY_NOT_PURCHASABLE}) otherwise
     */
    public ItemStore(final ItemRarity maxRarity) {
        if (maxRarity == null || !maxRarity.isPurchasable()) {
            throw new IllegalOperationException(STORE_RARITY_NOT_PURCHASABLE);
        }
        this.maxRarity = maxRarity;
    }

    /** The rarest tier this store carries. */
    public ItemRarity getMaxRarity() {
        return maxRarity;
    }

    /** Every catalog entry on sale here — {@link ItemCatalog#availableUpTo(ItemRarity)}. */
    public List<ItemTemplate> getOfferedItems() {
        return ItemCatalog.availableUpTo(maxRarity);
    }

    /** Whether this store carries template — purchasable and no rarer than {@link #getMaxRarity()}. */
    public boolean offers(final ItemTemplate template) {
        return template != null
                && template.getRarity().isPurchasable()
                && template.getRarity().isAtMost(maxRarity);
    }

    /**
     * Whether this store would sell a copy built to spec — its base must be {@link
     * #offers(ItemTemplate) offered}, and it must not be a Regalia (never stocked, whatever its
     * base).
     */
    public boolean offers(final ItemSpecification spec) {
        return spec != null && !spec.isRegalia() && offers(spec.getBase());
    }
}
