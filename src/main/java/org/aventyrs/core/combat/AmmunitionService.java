package org.aventyrs.core.combat;

import java.util.List;
import org.aventyrs.core.item.ProjectileItem;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * Ammunition for the weapons that fire it — an Arco's Flechas, a Besta's Virotes (table ruling, 2026-10-12): which
 * projectiles a combatant can fire, whether a shot may go, spending what it fired, and reloading a Besta.
 *
 * <p>A projectile is <b>consumed</b> by firing. An Arco draws it from the bag at each shot, the player picking which
 * kind; a Besta fires what its magazine holds, the kind picked when it was reloaded. Reloading costs its "Tempo de
 * Ação da recarga" in combat and nothing outside it, and the loaded state persists on the weapon copy across Cenas.
 * Half of every recoverable kind fired comes back at {@code CombatantSheet#endCombat()}.
 *
 * <p>Like every action here the PA are <b>reported, never deducted</b>, and {@code AttackDelivery} stays
 * report-only: it refuses a shot that cannot go ({@link #requireFirable}) and reports {@code
 * DeliveredAttackResult#getProjectilesToConsume()}; the caller then calls {@link #consume}.
 */
public interface AmmunitionService {

    /** One kind of projectile and how many units of it are at hand. */
    record AmmunitionStack(ProjectileItem projectile, int units) {
    }

    /**
     * The projectiles in holder's bag that weapon can fire or be reloaded with, one entry per kind, in catalog order.
     * Empty for a weapon that fires nothing.
     */
    List<AmmunitionStack> availableFor(CombatantSheet holder, Weapon weapon);

    /**
     * What weapon can fire right now: for an Arco, {@link #availableFor}; for a Besta, its magazine, grouped by kind
     * in firing order. Empty when it cannot shoot.
     */
    List<AmmunitionStack> firableFrom(CombatantSheet holder, Weapon weapon);

    /**
     * Refuses a shot of shots projectiles of projectile with weapon that cannot go: {@code AMMUNITION_REQUIRED} with
     * none named, {@code PROJECTILE_INCOMPATIBLE} for the wrong kind, {@code WEAPON_NOT_LOADED} for a Besta whose next
     * loaded projectile is not that one (or is nothing), {@code NOT_ENOUGH_AMMUNITION} when fewer are carried or
     * loaded than the shot fires. A weapon that fires nothing always passes.
     */
    void requireFirable(CombatantSheet holder, Weapon weapon, ProjectileItem projectile, int shots);

    /**
     * Spends shots projectiles of projectile fired with weapon — from the bag for an Arco (an emptied stack leaves
     * the inventory), from the magazine for a Besta — and, during a combat, notes them for recovery. Validates as
     * {@link #requireFirable} first, so nothing is spent on a refused call.
     */
    default void consume(CombatantSheet holder, Weapon weapon, ProjectileItem projectile, int shots) {
        consume(holder, weapon, projectile, shots, 0);
    }

    /**
     * Same as the four-argument form, for a shot whose destroyed projectiles never come back — a Rigidez Refletora
     * ({@code DeliveredAttackResult#getProjectilesDestroyed()}, core 0.1.6.11). Every shot is still spent; only
     * {@code shots - destroyed} join the recovery ledger.
     */
    void consume(CombatantSheet holder, Weapon weapon, ProjectileItem projectile, int shots, int destroyed);

    /**
     * What reloading weapon costs holder right now: {@link ActionCost#NONE} outside a combat, otherwise its "Tempo de
     * Ação da recarga" — or its Favor's price while holder meets the Favor ({@code Weapon#getFavoredReloadCost()}).
     * {@code null} for a weapon that never reloads.
     */
    ActionCost getReloadCost(CombatantSheet holder, Weapon weapon);

    /**
     * Fills weapon's magazine with projectile from holder's bag — as many as it has room for and holder carries
     * (a partial reload is allowed) — and returns the {@link #getReloadCost price}. Refuses ({@code
     * RELOAD_NOT_POSSIBLE}) a weapon with no magazine or a full one, and a projectile not carried; {@code
     * PROJECTILE_INCOMPATIBLE} for the wrong kind.
     */
    ActionCost reload(CombatantSheet holder, Weapon weapon, ProjectileItem projectile);
}
