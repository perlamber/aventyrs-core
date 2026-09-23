package org.aventyrs.core.combat;

import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import static org.aventyrs.core.util.TranslatableMessages.FORCED_ATTACK_TARGET_REQUIRED;

/**
 * The provoking-Aura rule both {@link AttackDelivery} and {@link AttackReceiver} apply, written
 * once: refuse an attack a bound attacker owes to an Aura's holder, and otherwise report whether a
 * bound attacker's attack on someone else deals Meio-Dano. See {@code
 * org.aventyrs.core.scene.ActiveAura}.
 *
 * <p>Only enforced when the request names both a live {@link Scene} and an attacker, since both
 * are optional on the two request types; without them there is nothing to read.
 */
final class AuraTargeting {

    private AuraTargeting() {
    }

    /**
     * Whether an Aura makes attacker's attack on defender deal Meio-Dano.
     *
     * @param forcedTargetUnavailable the caller's word that the Aura's holder is not a valid target
     *                                right now (out of reach, hidden…), which this core can't judge
     * @throws IllegalOperationException ({@code FORCED_ATTACK_TARGET_REQUIRED}) if an Aura
     *         requires attacker to attack its holder first and defender is someone else
     */
    static boolean resolveHalvesDamage(final Scene scene, final CombatantSheet attacker,
                                       final CombatantSheet defender, final boolean forcedTargetUnavailable) {
        if (scene == null || attacker == null) {
            return false;
        }
        boolean aimedElsewhere = scene.getForcedAttackTarget(attacker)
                .filter(holder -> !holder.getId().equals(defender.getId()))
                .isPresent();
        if (aimedElsewhere && !forcedTargetUnavailable) {
            throw new IllegalOperationException(FORCED_ATTACK_TARGET_REQUIRED);
        }
        return scene.auraHalvesDamage(attacker, defender);
    }
}
