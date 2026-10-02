package org.aventyrs.core.item;

import lombok.Getter;

/**
 * Utilidades — the store's {@link ItemCategory#UTILITIES} tab: tools and kits a character carries rather than
 * wields or wears. Its first entry is the kit {@code MedicinaECuraCompetencyAbility#BOM_DOUTOR} asks for (table
 * ruling, 2026-10-02); potions and the like are meant to join it.
 *
 * <p>⚠️ The kit's Preço and Dureza are placeholders — the rules text authors neither.
 */
@Getter
public enum UtilityItem implements ItemTemplate {

    /** "Se tiver um kit de primeiros socorros em mãos" — carried in the inventory or equipped. */
    KIT_DE_PRIMEIROS_SOCORROS(
            "Kit de Primeiros Socorros",
            "Bandagens, unguentos e instrumentos para tratar ferimentos — o kit que Bom Doutor pede em mãos.",
            ItemWeightClass.LIGHT,
            ItemRarity.COMMON,
            1, 5);

    private final String name;
    private final String description;
    private final ItemWeightClass weightClass;
    private final ItemRarity rarity;
    private final int price;
    private final int hardness;

    UtilityItem(final String name, final String description, final ItemWeightClass weightClass,
                final ItemRarity rarity, final int price, final int hardness) {
        this.name = name;
        this.description = description;
        this.weightClass = weightClass;
        this.rarity = rarity;
        this.price = price;
        this.hardness = hardness;
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.UTILITIES;
    }

    @Override
    public int getPhysicalDefenseBonus() {
        return 0;
    }

    @Override
    public int getMagicDefenseBonus() {
        return 0;
    }

    @Override
    public int getCastingBonus() {
        return 0;
    }

    @Override
    public ItemFavor getFavor() {
        return null;
    }

    /** Whether holder carries or wears a copy of this kit. */
    public boolean isHeldBy(final org.aventyrs.core.sheet.CombatantSheet holder) {
        return holder != null && java.util.stream.Stream.concat(holder.getInventory().stream(),
                        holder.getCharacter().getEquipment().stream())
                .anyMatch(item -> name.equals(item.getName()) && item.getCategory() == ItemCategory.UTILITIES);
    }
}
