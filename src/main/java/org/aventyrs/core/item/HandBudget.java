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
        return handCost(item, null);
    }

    /**
     * {@link #handCost(Item)} for wielder — whose {@code Character#carriedWeightClassOf} may count a weapon lighter
     * (Besta de Carga: a Médio weapon is Leve, so one hand). {@code null} reads the authored column alone.
     */
    public static int handCost(@NonNull final Item item, final org.aventyrs.core.character.Character wielder) {
        if (item.getCategory() == ItemCategory.SHIELD) {
            return 1;
        }
        if (item.getCategory() == ItemCategory.PROJECTILE || !(item instanceof Weapon weapon)) {
            return 0;
        }
        return isTwoHanded(weapon, wielder) ? 2 : 1;
    }

    /** The hands items take up together. */
    public static int handsUsed(@NonNull final List<? extends Item> items) {
        return handsUsed(items, null);
    }

    /** {@link #handsUsed(List)} for wielder — see {@link #handCost(Item, org.aventyrs.core.character.Character)}. */
    public static int handsUsed(@NonNull final List<? extends Item> items,
                                final org.aventyrs.core.character.Character wielder) {
        return items.stream().mapToInt(item -> handCost(item, wielder)).sum();
    }

    /**
     * Whether weapon needs both hands — an Arco, a Besta, anything Médio or Pesado, or one a fitted Corrente com
     * Peso "passa a exigir uso de ambas as mãos".
     */
    public static boolean isTwoHanded(@NonNull final Weapon weapon) {
        return isTwoHanded(weapon, null);
    }

    /** {@link #isTwoHanded(Weapon)} with the weight class as wielder carries it ({@code null}: as authored). */
    public static boolean isTwoHanded(@NonNull final Weapon weapon, final org.aventyrs.core.character.Character wielder) {
        ItemWeightClass authored = weapon.getWeightClass();
        ItemWeightClass weight = wielder == null || authored == null ? authored
                : wielder.carriedWeightClassOf(authored);
        return weapon.getCategory() == ItemCategory.BOW
                || weapon.getCategory() == ItemCategory.CROSSBOW
                || weight == ItemWeightClass.MEDIUM
                || weight == ItemWeightClass.HEAVY
                || WeaponEnhancements.offensiveImprovements(weapon.host()).stream()
                        .anyMatch(OffensiveImprovement::requiresBothHands);
    }
}
