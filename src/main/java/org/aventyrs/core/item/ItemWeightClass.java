package org.aventyrs.core.item;

/**
 * How heavy an {@link Item} is — the first half of an item's own "(Pesado/Raro)"-style
 * heading, alongside {@link ItemRarity}.
 *
 * <p>It drives the Destreza penalty from defensive equipment in {@code
 * EsquivaEApararInteraction}, and caps how many Aprimoramentos an Obra-Prima copy can carry
 * ({@link #getMaximumImprovements()}, read by {@code
 * org.aventyrs.core.item.AbstractItem#addImprovement}). Carga (a character's carrying capacity,
 * which a weight class would presumably also feed into) remains unmodeled.
 */
public enum ItemWeightClass {
    LIGHT,
    MEDIUM,
    HEAVY;

    /** Moves this class by a signed number of weight categories, bounded to the authored range. */
    public ItemWeightClass adjustedBy(final int adjustment) {
        int adjustedOrdinal = Math.max(0, Math.min(values().length - 1, ordinal() + adjustment));
        return values()[adjustedOrdinal];
    }

    /**
     * How many distinct Aprimoramentos an Obra-Prima of this weight class may carry — "Itens
     * Leves podem possuir apenas 1 Aprimoramento, Médios podem possuir até 2 diferentes
     * Aprimoramentos e Itens Pesados podem somar até 3". One per ordinal: LIGHT 1, MEDIUM 2,
     * HEAVY 3.
     */
    public int getMaximumImprovements() {
        return ordinal() + 1;
    }
}
