package org.aventyrs.core.combat;

import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * The Defesa a defender's Talentos add against an attack made as a Reação ({@code Feat#resolveReactionDefenseBonus},
 * core 0.1.7.18) — shared by {@link AttackReceiver} (a rolled defence) and {@link AttackDelivery} (a flat Defesa).
 */
final class ReactionDefense {

    private ReactionDefense() {
    }

    /** The bonus defender takes against a Reação attack on defenseType; 0 when the attack is not one. */
    static int bonus(final CombatantSheet defender, final DefenseType defenseType, final boolean reaction) {
        if (!reaction || defender == null) {
            return 0;
        }
        return defender.getCharacter().getFeats().stream()
                .mapToInt(feat -> feat.resolveReactionDefenseBonus(defenseType, defender))
                .sum();
    }
}
