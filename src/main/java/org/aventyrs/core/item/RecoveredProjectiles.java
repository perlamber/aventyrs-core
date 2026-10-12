package org.aventyrs.core.item;

/**
 * Projectiles of one kind a combatant picked back up when a combat ended — floor(half) of the units of projectile
 * they fired in it (table ruling, 2026-10-12), already returned to their bag. A Bodkin is never recovered ({@link
 * ProjectileHead#isRecoverable()}). Reported by {@code CombatantSheet#endCombat()} for the caller to tell the player.
 */
public record RecoveredProjectiles(ProjectileItem projectile, int units) {
}
