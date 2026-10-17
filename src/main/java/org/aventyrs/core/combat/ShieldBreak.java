package org.aventyrs.core.combat;

import lombok.NonNull;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * Damage a landed attack deals to its target's worn defensive equipment — {@code StrengthAbility#ESTILHACADOR}'s
 * "seus Ataques Corpo-a-Corpo bem-sucedidos contra outros personagens causam 2 pontos de dano ao item tipo
 * 'escudo' que o alvo esteja utilizando; caso o escudo seja destruído, ou o alvo não utilize um, este dano é
 * causado a um item tipo 'armadura'".
 *
 * <p>Reported by {@code AttackDelivery} as {@code DeliveredAttackResult#getDefensiveEquipmentDamage()} — an amount,
 * not an item, because the attacker's client may hold only a stand-in for the target. Whoever owns the target's
 * real sheet resolves the item here ({@link #against}) and applies it. "Caso o escudo seja destruído" is read as an
 * already-destroyed Escudo: one these 2 points break takes them, and the next hit falls to the Armadura. A foe's
 * stat block carries no equipment, so nothing is struck.
 *
 * @param item   the struck Escudo or Armadura
 * @param damage the raw damage, before the item's own reductions
 */
public record ShieldBreak(@NonNull Item item, int damage) {

    /** ESTILHACADOR's figure. */
    public static final int ESTILHACADOR_DAMAGE = 2;

    /** The first worn, intact Escudo, else Armadura, of target taking damage — {@code null} when it wears neither. */
    public static ShieldBreak against(@NonNull final CombatantSheet target, final int damage) {
        if (damage <= 0) {
            return null;
        }
        Item struck = firstIntact(target, ItemCategory.SHIELD);
        if (struck == null) {
            struck = firstIntact(target, ItemCategory.ARMOR);
        }
        return struck == null ? null : new ShieldBreak(struck, damage);
    }

    /** Deals {@link #damage} to {@link #item}, returning what landed after the item's own mitigation. */
    public int apply() {
        return item.applyDamage(damage);
    }

    private static Item firstIntact(final CombatantSheet target, final ItemCategory category) {
        return target.getCharacter().getEquipment().stream()
                .filter(item -> item.getCategory() == category && !item.isDestroyed())
                .findFirst()
                .orElse(null);
    }
}
