package org.aventyrs.core.item;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

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
 *
 * <p><b>Obra-Primas and Aprimoramentos are offered too.</b> Since a store copy can be forged as
 * an Obra-Prima with Aprimoramentos, the store also lists which of each it carries — again the
 * whole authored catalog, both the defensive ({@link DefensiveMasterpiece} / {@link
 * DefensiveImprovement}) and the offensive ({@link OffensiveMasterpiece} / {@link
 * OffensiveImprovement}) halves, capped by the same {@link #getMaxRarity()} ceiling. {@link #offers(ItemSpecification)} enforces that ceiling on every
 * part of a requested copy, not just its base.
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

    /**
     * Every Obra-Prima a copy bought here can be forged as — both authored catalogs ({@link
     * DefensiveMasterpiece} then {@link OffensiveMasterpiece}), no rarer than {@link
     * #getMaxRarity()}.
     *
     * <p>One list rather than two accessors: a store's stock is a rarity question, not a
     * defensive/offensive one, and which of the two an entry can actually be fitted to is already
     * enforced where it belongs ({@code AbstractItem#setMasterpiece}).
     */
    public List<Masterpiece> getOfferedMasterpieces() {
        return Stream.concat(Arrays.stream(DefensiveMasterpiece.values()),
                        Arrays.stream(OffensiveMasterpiece.values()))
                .filter(masterpiece -> masterpiece.getRarity().isAtMost(maxRarity))
                .map(Masterpiece.class::cast)
                .toList();
    }

    /**
     * Every Aprimoramento a copy bought here can have fitted — both authored catalogs ({@link
     * DefensiveImprovement} then {@link OffensiveImprovement}), no rarer than {@link
     * #getMaxRarity()}.
     */
    public List<Improvement> getOfferedImprovements() {
        return Stream.concat(Arrays.stream(DefensiveImprovement.values()),
                        Arrays.stream(OffensiveImprovement.values()))
                .filter(improvement -> improvement.getRarity().isAtMost(maxRarity))
                .map(Improvement.class::cast)
                .toList();
    }

    /** Whether this store carries template — purchasable and no rarer than {@link #getMaxRarity()}. */
    public boolean offers(final ItemTemplate template) {
        return template != null
                && template.getRarity().isPurchasable()
                && template.getRarity().isAtMost(maxRarity);
    }

    /** Whether this store carries masterpiece — no rarer than {@link #getMaxRarity()}. */
    public boolean offers(final Masterpiece masterpiece) {
        return masterpiece != null && masterpiece.getRarity().isAtMost(maxRarity);
    }

    /** Whether this store carries improvement — no rarer than {@link #getMaxRarity()}. */
    public boolean offers(final Improvement improvement) {
        return improvement != null && improvement.getRarity().isAtMost(maxRarity);
    }

    /**
     * Whether this store would sell a copy built to spec — it must not be a Regalia (never
     * stocked, whatever its base), and its base, its Obra-Prima and every fitted Aprimoramento
     * must each be no rarer than {@link #getMaxRarity()}.
     */
    public boolean offers(final ItemSpecification spec) {
        return spec != null
                && !spec.isRegalia()
                && offers(spec.getBase())
                && (spec.getMasterpiece() == null || offers(spec.getMasterpiece()))
                && spec.getImprovements().stream().allMatch(this::offers);
    }
}
