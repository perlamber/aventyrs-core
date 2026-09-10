package org.aventyrs.core.item;

import org.aventyrs.core.sheet.CombatantSheet;

/**
 * A live-state gate an individual {@link ItemBonus} can carry — resolved against the wielder's
 * {@link CombatantSheet}, on top of the {@link ItemFavor}'s own {@link ItemRequirements}.
 *
 * <p>Most Favor bonuses need none: an item's Favor is otherwise a "carried it, met the
 * Requisitos" affair with nothing per-Rodada about it, which is why {@link ItemBonus} defaults
 * to {@link #NONE} and {@link ItemFavor}'s plain {@code Character} resolution can still answer.
 * The Escudos are the exception the ruleset forces — their "se não realizou nenhuma ação
 * ofensiva nesta Rodada" bonuses ({@code ShieldItem#ESCUDO_MEDIO} etc.) genuinely blink on and
 * off within a combat, so their {@code ItemBonus}es carry {@link #NO_OFFENSIVE_ACTION_THIS_ROUND}
 * and only resolve through {@link ItemFavor#resolveBonus(org.aventyrs.core.modifier.ModifierType,
 * CombatantSheet)}.
 *
 * <p>A {@code Character}-only resolution treats every non-{@link #NONE} condition as unmet —
 * "no sheet to ask" reads as "cannot tell", the same convention a {@code null} {@code
 * CombatantSheet} already has on the {@code Feat} hooks.
 */
public enum FavorCondition {

    /** Always active once the Favor's {@link ItemRequirements} are met. */
    NONE {
        @Override
        boolean isMetBy(final CombatantSheet sheet) {
            return true;
        }
    },

    /**
     * Active only while the wielder has taken no offensive action so far this Rodada — see
     * {@link CombatantSheet#hasActedOffensivelyThisRound()}.
     */
    NO_OFFENSIVE_ACTION_THIS_ROUND {
        @Override
        boolean isMetBy(final CombatantSheet sheet) {
            return sheet != null && !sheet.hasActedOffensivelyThisRound();
        }
    };

    /** Whether this condition currently holds for sheet — {@code false} for a {@code null} sheet. */
    abstract boolean isMetBy(CombatantSheet sheet);
}
