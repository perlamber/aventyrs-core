package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.ego.EquipmentPointExpense;

/**
 * The one place a PE price takes its holder's discounts (core 0.1.6.4): every held Vantagem de Ego's {@code
 * EgoAdvantage#resolveEquipmentPointDiscount} for that kind of expense, the price floored at {@link #MINIMUM_PRICE} —
 * BARGANHISTA's "até o mínimo de 1PE". Shared by {@link ItemPurchaseService} and {@link EquipmentCraftingService}, so a
 * store and a forge can never floor differently.
 */
public final class EquipmentPointPricing {

    /** The least a discounted expense can cost, in PE. */
    public static final int MINIMUM_PRICE = 1;

    private EquipmentPointPricing() {
    }

    /** price less holder's discounts for kind, never below {@link #MINIMUM_PRICE}. {@code null} holder discounts nothing. */
    public static int discounted(final int price, final Character holder, final EquipmentPointExpense kind) {
        int discount = holder == null ? 0 : holder.getAllEgoAdvantages().stream()
                .mapToInt(advantage -> advantage.resolveEquipmentPointDiscount(kind))
                .sum();
        return Math.max(MINIMUM_PRICE, price - discount);
    }
}
