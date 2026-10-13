package org.aventyrs.core.combat;

import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.AttackSource;

/**
 * What a defender's worn Aprimoramentos do back to the attack itself (core 0.1.6.11) — {@code
 * DefensiveImprovement#ESPINHOSA}'s thorns and {@code #RIGIDEZ_REFLETORA}'s reflection, resolved by {@code
 * item.DefensiveEnhancements#retaliationAgainst}. Fired hit or miss, against the attack's primary target only (table
 * ruling, 2026-10-12). <b>Reported, never dealt</b>, like {@link Retaliation}: the caller applies it, on the client that
 * owns the attacker.
 *
 * @param attackerDamage      the Dano Físico the melee attacker takes, through their own mitigation, or {@code null}
 * @param weaponDamage        the damage the melee attacker's weapon takes ({@code Item#applyDamage}); 0 for none
 * @param projectileDestroyed whether the fired projectile is destroyed — never recovered at combat end
 */
public record EquipmentRetaliation(Retaliation attackerDamage, int weaponDamage, boolean projectileDestroyed) {

    /**
     * Deals the attacker's share and damages their weapon. A natural weapon, an unarmed swing ({@code null}) and a
     * catalog template take nothing — {@link Item#applyDamage} is a no-op for them.
     *
     * @return the PV the attacker lost
     */
    public int applyTo(final CombatantSheet attacker, final AttackSource weapon, final CombatantSheet defender,
                       final SceneContext sceneContext, final DamageService damageService) {
        damageWeapon(weapon);
        return attackerDamage == null || attacker == null ? 0
                : attackerDamage.dealTo(attacker, defender, sceneContext, damageService);
    }

    /** Damages weapon by {@link #weaponDamage}, returning what landed (after the item's own mitigation). */
    public int damageWeapon(final AttackSource weapon) {
        return weaponDamage > 0 && weapon instanceof Item item ? item.applyDamage(weaponDamage) : 0;
    }
}
