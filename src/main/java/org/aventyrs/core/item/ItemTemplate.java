package org.aventyrs.core.item;

import org.aventyrs.core.ability.ItemActiveAbility;

/**
 * A catalog blueprint for a piece of Equipamento — the rules entry, not a unique forged copy.
 *
 * <p>This is the first migration step in the item redesign: the static catalog entries (such as
 * {@link ArmorItem}) now sit behind a dedicated template contract, while a true owned item keeps
 * living under {@link Item}. A template can still be forged into a runtime instance with {@link
 * #forge()}, which is the compatibility bridge for callers that still use armor entries as if they
 * were concrete items while the rest of the model moves to the new split.
 */
public interface ItemTemplate extends Item {

    @Override
    default ItemActiveAbility getActiveAbility() {
        return null;
    }

    @Override
    default RegaliaGrade getRegaliaGrade() {
        return null;
    }

    /**
     * Forge this template into a concrete item instance. This is the migration bridge between the
     * old enum-backed catalog model and the new template-plus-instance split.
     */
    default Item forge() {
        return AbstractItem.builder()
                .name(getName())
                .description(getDescription())
                .category(getCategory())
                .rarity(getRarity())
                .weightClass(getWeightClass())
                .price(getPrice())
                .physicalDefenseBonus(getPhysicalDefenseBonus())
                .magicDefenseBonus(getMagicDefenseBonus())
                .hardness(getHardness())
                .castingBonus(getCastingBonus())
                .favor(getFavor())
                .build();
    }
}
