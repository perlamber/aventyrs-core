package org.aventyrs.core.item;

import lombok.NonNull;

import java.util.List;

/**
 * How many hands something takes up — the two-hand budget a character's loadout lives within.
 * Handedness is <b>inferred</b> from {@link ItemWeightClass} and category, never authored ({@code
 * equipamentos.txt} gives no weapon a hands column): an Escudo takes one, a {@code MEDIUM}/{@code
 * HEAVY} weapon or any Arco/Besta both, any other weapon one, and a projectile or anything not
 * held to fight with none.
 *
 * <p>Two consumers: {@code CharacterSheet}'s equip validation, and {@code GrappleService}, where a
 * hold occupies a hand too (table ruling, 2026-10-07).
 */
public final class HandBudget {

    /** A character has two hands. */
    public static final int AVAILABLE_HANDS = 2;

    private HandBudget() {
    }

    /** How many hands item takes up while held — 0 for anything not held to fight with. */
    public static int handCost(@NonNull final Item item) {
        if (item.getCategory() == ItemCategory.SHIELD) {
            return 1;
        }
        if (item.getCategory() == ItemCategory.PROJECTILE || !(item instanceof Weapon weapon)) {
            return 0;
        }
        return isTwoHanded(weapon) ? 2 : 1;
    }

    /** The hands items take up together. */
    public static int handsUsed(@NonNull final List<? extends Item> items) {
        return items.stream().mapToInt(HandBudget::handCost).sum();
    }

    /** Whether weapon needs both hands — an Arco, a Besta, or anything Médio or Pesado. */
    public static boolean isTwoHanded(@NonNull final Weapon weapon) {
        return weapon.getCategory() == ItemCategory.BOW
                || weapon.getCategory() == ItemCategory.CROSSBOW
                || weapon.getWeightClass() == ItemWeightClass.MEDIUM
                || weapon.getWeightClass() == ItemWeightClass.HEAVY;
    }
}
