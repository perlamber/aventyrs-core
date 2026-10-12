package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.ego.EquipmentPointExpense;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;

import java.util.Map;

/**
 * The one place a PE price takes its holder's discounts (core 0.1.6.4): for that kind of expense, every held Vantagem
 * de Ego's {@code EgoAdvantage#resolveEquipmentPointDiscount}, and since 0.1.6.5 every Habilidade de Competência's
 * (Cambalacho) and every unlocked Excelência tier's (Profissão Focado/Lenda) twin of it — the three-source scan — all
 * summed, the price floored at {@link #MINIMUM_PRICE} —
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
        if (holder == null) {
            return Math.max(MINIMUM_PRICE, price);
        }
        int discount = holder.getAllEgoAdvantages().stream()
                .mapToInt(advantage -> advantage.resolveEquipmentPointDiscount(kind))
                .sum();
        discount += SkillCompetencyAbility.allFor(holder).stream()
                .mapToInt(ability -> ability.resolveEquipmentPointDiscount(kind))
                .sum();
        for (Map.Entry<SkillType, CharacterSkill> entry : holder.getSkills().entrySet()) {
            discount += SkillExcellency.unlockedBy(entry.getKey().getExcellencyClass(),
                            entry.getValue().getGraduation().getGraduationValue()).stream()
                    .mapToInt(excellency -> excellency.resolveEquipmentPointDiscount(kind))
                    .sum();
        }
        return Math.max(MINIMUM_PRICE, price - discount);
    }
}
