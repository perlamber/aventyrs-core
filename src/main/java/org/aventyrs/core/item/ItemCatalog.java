package org.aventyrs.core.item;

import java.util.Arrays;
import java.util.List;

/**
 * Every authored {@link ItemTemplate} — the single place to ask "what pieces of Equipamento
 * exist?", which no per-{@link ItemCategory} enum can answer on its own. The {@code Item}-side
 * equivalent of {@code org.aventyrs.core.feat.FeatCatalog}.
 *
 * <h2>Why an explicit registry, not a sealed {@code permits} clause</h2>
 *
 * {@code FeatCatalog} discovers its constants from {@code Feat.class.getPermittedSubclasses()},
 * which the compiler keeps exhaustive. {@link ItemTemplate} is deliberately <b>not</b> sealed:
 * several tests stand in an anonymous {@code new ItemTemplate() { … }} for "an item that belongs
 * to no catalog" (a forge of a one-off, a template with a zero Preço modifier), and a sealed
 * interface forbids anonymous implementors outright. So the catalog enums are listed here by
 * hand — one line each — and {@code ItemCatalogTest#catalogCoversEveryRegisteredEnum} guards the
 * list against a constant silently going missing.
 *
 * <p>Discovery lives in its own final class rather than as a {@code static} field on {@link
 * ItemTemplate}, for the same class-initialisation-cycle reason {@code FeatCatalog}'s javadoc
 * gives: nothing triggers this initialiser except asking it a question.
 */
public final class ItemCatalog {

    /** Every enum that contributes {@link ItemTemplate} constants — add a category's enum here. */
    private static final List<Class<? extends ItemTemplate>> CATALOG_ENUMS =
            List.of(ArmorItem.class, NaturalWeapon.class);

    private static final List<ItemTemplate> ALL = discover();

    private ItemCatalog() {
    }

    /** Every authored piece of Equipamento, across every category enum. */
    public static List<ItemTemplate> all() {
        return ALL;
    }

    /**
     * Every catalog entry that can be bought or sold — {@link #all()} minus every entry whose
     * {@link ItemRarity#isPurchasable()} is {@code false} (i.e. every Arma/Defesa Natural).
     */
    public static List<ItemTemplate> purchasable() {
        return ALL.stream().filter(template -> template.getRarity().isPurchasable()).toList();
    }

    /**
     * Every {@link #purchasable()} entry whose Raridade is no rarer than maxRarity — the slice
     * an {@code ItemStore} with that ceiling offers.
     */
    public static List<ItemTemplate> availableUpTo(final ItemRarity maxRarity) {
        return purchasable().stream()
                .filter(template -> template.getRarity().isAtMost(maxRarity))
                .toList();
    }

    private static List<ItemTemplate> discover() {
        return CATALOG_ENUMS.stream()
                .map(Class::getEnumConstants)
                .flatMap(Arrays::stream)
                .map(ItemTemplate.class::cast)
                .toList();
    }
}
