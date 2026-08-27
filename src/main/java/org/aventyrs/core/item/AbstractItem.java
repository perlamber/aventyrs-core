package org.aventyrs.core.item;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * A plain builder-built {@link Item}, for a one-off or caller-supplied piece of Equipamento
 * that doesn't belong in one of this package's own catalog enums (e.g. {@link ArmorItem}) —
 * mirroring {@code org.aventyrs.core.feat.AbstractFeat}'s identical role alongside {@code
 * org.aventyrs.core.feat.ArtesMarciaisFeat}.
 *
 * <p>An item built through this has no Dano Base and cannot be swung — see {@link
 * AbstractWeapon} for the {@link Weapon} counterpart, which extends this class and adds that
 * one column.
 *
 * <p>Nothing here validates the values it's handed: like every other builder in this codebase
 * (see CLAUDE.md's note on {@code AttributeValue.builder()}), it's a data holder, not a
 * gatekeeper.
 */
@Getter
@SuperBuilder
public class AbstractItem implements Item {

    private String name;
    private String description;
    private ItemCategory category;
    private ItemRarity rarity;
    private ItemWeightClass weightClass;
    private int price;
    private int physicalDefenseBonus;
    private int magicDefenseBonus;
    private int hardness;
    private int castingBonus;
    private ItemFavor favor;
}
