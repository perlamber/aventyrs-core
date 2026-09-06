package org.aventyrs.core.item;

/**
 * Which column of the "Preços de Obras-Primas" / "Preços de Aprimoramentos" tables an
 * enhancement is priced from — Armas, Armaduras or Pedras do Poder (see {@code
 * docs/rules/equipamentos.txt}, under "Itens Obras-Primas"). {@link EnhancementPricing} is the
 * lookup; every {@link Masterpiece} and {@link Improvement} names its own column through
 * {@code getPriceCategory()}.
 *
 * <p>Deliberately its own enum rather than a reuse of {@link ItemType}: the third column prices
 * refinements fitted to a {@link PowerStone}, which is not an {@link Item} at all, and the first
 * two collapse the four {@code ItemType}s into the two columns the table actually has. Nothing
 * prices a Consumível or an Utilitário — no such Obra-Prima exists in the source.
 */
public enum EnhancementPriceCategory {

    /** The "Armas" column — an Obra-Prima or Aprimoramento Ofensivo, fitted to a {@link Weapon}. */
    WEAPON,

    /** The "Armaduras" column — an Obra-Prima or Aprimoramento Defensivo. */
    ARMOR,

    /** The "Pedras do Poder" column — a {@link PowerStoneMasterpiece}/{@link PowerStoneImprovement}. */
    POWER_STONE
}
