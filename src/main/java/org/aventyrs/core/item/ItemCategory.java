package org.aventyrs.core.item;

public enum ItemCategory {
    ARMOR(ItemType.DEFENSIVE), BOOTS(ItemType.DEFENSIVE), CLOAK(ItemType.DEFENSIVE), GLOVES(ItemType.DEFENSIVE),
    HELMET(ItemType.DEFENSIVE), RING(ItemType.UTILITY), SHIELD(ItemType.DEFENSIVE),
    BOW(ItemType.OFFENSIVE), THROWABLE(ItemType.OFFENSIVE), CROSSBOW(ItemType.OFFENSIVE), 
    WHIP(ItemType.OFFENSIVE), CLUB(ItemType.OFFENSIVE),
    LIGHT_BLADE(ItemType.OFFENSIVE), HEAVY_BLADE(ItemType.OFFENSIVE), SPEAR(ItemType.OFFENSIVE), 
    PROJECTILE(ItemType.OFFENSIVE), POTION(ItemType.CONSUMABLE), SCROLL(ItemType.CONSUMABLE);

    private final ItemType type;

    ItemCategory(final ItemType type) {
        this.type = type;
    }

    public ItemType getType() {
        return type;
    }
}
