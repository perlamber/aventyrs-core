package org.aventyrs.core.monster.summon;

import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * A Lacerto creature's elemental power as figures a caller can deal (core 0.0.97) — Sopro Elemental's cone, or Aura
 * Elemental's per-Rodada damage to whoever is adjacent. ⚠️ Natural, by table ruling (2026-10-01): the rules name no
 * element.
 *
 * <p>This core does no geometry, so the caller resolves who stands in the cone ({@code scene.grid.AreaFootprint}) or
 * beside the creature, throws {@link #diceCount()} d6 once, and deals each target its share with {@link #dealTo}.
 *
 * @param diceCount      d6 thrown once for the whole discharge
 * @param flat           added to the dice
 * @param reductionPerUd "reduzido em N a cada UD" — ⚠️ counted past the first UD, so an adjacent target takes it whole
 * @param descriptor     what kind of damage, so each target's own resistances judge it
 * @param coneLength     the cone's length in UD — 0 for an aura
 * @param actionPoints   what using it costs — 0 for an aura, which needs no action
 * @param cooldownRounds Refrigeração — Rodadas before it can be used again
 */
public record ElementalDischarge(int diceCount, int flat, int reductionPerUd, DamageDescriptor descriptor,
                                 int coneLength, int actionPoints, int cooldownRounds) {

    /** What a target distanceUd away takes from a discharge whose dice came to rolled. */
    public int damageAt(final int rolled, final int distanceUd) {
        return Math.max(0, rolled + flat - reductionPerUd * Math.max(0, distanceUd - 1));
    }

    /** Deals target its share, judged by its own RE, RM and immunities. Returns the PV it lost. */
    public int dealTo(final CombatantSheet target, final CombatantSheet source, final int rolled, final int distanceUd,
                      final SceneContext sceneContext, final DamageService damageService) {
        int before = target.getDamageTaken();
        damageService.applyDamage(target, sceneContext, descriptor, source, damageAt(rolled, distanceUd), false);
        return target.getDamageTaken() - before;
    }
}
