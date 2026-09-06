package org.aventyrs.core.item;

/**
 * The Preço, in Pontos de Equipamento, of one Obra-Prima or one Aprimoramento — a lookup into
 * the two rarity-by-category tables under "Itens Obras-Primas" in {@code
 * docs/rules/equipamentos.txt}. An enhancement's Preço is a pure function of its {@link
 * ItemRarity} and the kind of thing it is fitted to ({@link EnhancementPriceCategory}); nothing
 * about the individual copy, its host's own Preço or its crafter enters into it, which is why
 * this is a static table rather than a service.
 *
 * <p>Kept beside {@link ItemRarity} rather than on it: the per-tier tables {@code ItemRarity}
 * already carries (fabrication/repair GD, Obra-Prima Graduação floor) are one-dimensional, and
 * these two are a rarity <em>by</em> category grid. Holding both grids in one file is what makes
 * them checkable against the source rows.
 *
 * <p>This is what {@link Masterpiece#getPriceModifier()} and {@link
 * Improvement#getPriceModifier()} resolve to, so a catalog constant states its Raridade and its
 * column and is priced with no further wiring — and through them, what {@code
 * ItemForgery#getTotalValue()} sums into a forge's worth. {@link PowerStoneMasterpiece} and
 * {@link PowerStoneImprovement} read the third column directly: they are not {@code
 * Masterpiece}/{@code Improvement} implementations, since a Pedra do Poder's refinements adjust
 * its charge economy rather than granting an item bonus.
 */
public final class EnhancementPricing {

    private EnhancementPricing() {
    }

    /**
     * The Preço in PE of an Obra-Prima of this Raridade fitted to this kind of thing — the
     * "Preços de Obras-Primas" table: Comum 6/8/4, Incomum 12/12/6, Raro 24/20/9, Épico
     * 36/32/13, Mítico 48/48/18 (Armas / Armaduras / Pedras do Poder).
     *
     * @throws IllegalStateException for {@link ItemRarity#NATURAL} — an Equipamento Natural is
     *         part of a body, never master-crafted and never priced.
     */
    public static int masterpiecePrice(final ItemRarity rarity, final EnhancementPriceCategory category) {
        return switch (rarity) {
            case COMMON -> byCategory(category, 6, 8, 4);
            case UNCOMMON -> byCategory(category, 12, 12, 6);
            case RARE -> byCategory(category, 24, 20, 9);
            case EPIC -> byCategory(category, 36, 32, 13);
            case MYTHIC -> byCategory(category, 48, 48, 18);
            case NATURAL -> throw naturalUnsupported();
        };
    }

    /**
     * The Preço in PE of an Aprimoramento of this Raridade fitted to this kind of thing — the
     * "Preços de Aprimoramentos" table: Comum 4/5/2, Incomum 8/8/4, Raro 13/12/7, Épico
     * 19/18/11, Mítico 26/24/16 (Armas / Armaduras / Pedras do Poder).
     *
     * <p>An Aprimoramento is priced from <b>its own</b> Raridade, not its host Obra-Prima's:
     * {@link Improvement#getRarity()} is already the tier its install Grau de Dificuldade is
     * read from, and this is the same column.
     *
     * @throws IllegalStateException for {@link ItemRarity#NATURAL}.
     */
    public static int improvementPrice(final ItemRarity rarity, final EnhancementPriceCategory category) {
        return switch (rarity) {
            case COMMON -> byCategory(category, 4, 5, 2);
            case UNCOMMON -> byCategory(category, 8, 8, 4);
            case RARE -> byCategory(category, 13, 12, 7);
            case EPIC -> byCategory(category, 19, 18, 11);
            case MYTHIC -> byCategory(category, 26, 24, 16);
            case NATURAL -> throw naturalUnsupported();
        };
    }

    private static int byCategory(final EnhancementPriceCategory category, final int weapon, final int armor,
                                  final int powerStone) {
        return switch (category) {
            case WEAPON -> weapon;
            case ARMOR -> armor;
            case POWER_STONE -> powerStone;
        };
    }

    private static IllegalStateException naturalUnsupported() {
        return new IllegalStateException("Equipamentos Naturais are part of a body — never enhanced or priced.");
    }
}
