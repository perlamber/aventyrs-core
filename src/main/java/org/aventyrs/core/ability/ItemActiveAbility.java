package org.aventyrs.core.ability;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.TemporaryEffect;

/**
 * An ability the holder must actively spend Pontos de Ação/Magia to trigger, lasting a fixed
 * number of Rodadas — as opposed to an {@link AttributeAbility}/{@code SkillCompetencyAbility}
 * whose effect (if any) is always on. {@code Character#getActiveAbilities()} holds every one a
 * character has acquired, granted at acquisition time by whichever ability describes it — see
 * {@link AttributeAbility#resolveActiveAbility()}.
 *
 * <p>{@link #resolveEffect(Character)} is what the activated state actually grants — a {@link
 * TemporaryEffect} (a {@code TemporaryBonus} for every concrete ability so far) sized off the
 * activating Character's own stats (e.g. {@code ConcentracaoProfundaActiveAbility}'s half-Foco
 * roll bonus), for {@link #getDurationInRounds()} Rodadas once applied. Resolving it doesn't
 * spend anything or mutate the Character — see {@code
 * org.aventyrs.core.character.services.ActiveAbilityService#activate} for the entry point that
 * validates affordability, spends the cost, and applies the resolved effect to a
 * {@code CombatantSheet} in one step.
 */
public interface ItemActiveAbility extends ActiveAbility {
    /**
     * An active ability bound to a specific item instance, such as a regalia or crafted item.
     * It shares the same activation contract as a character ability but remains item-scoped.
     */

    /**
     * What binding this ability into an item adds to that item's worth, in PE — read by {@code
     * org.aventyrs.core.item.ItemForgery#getTotalValue()}, which sums it alongside {@code
     * Masterpiece#getPriceModifier()} and {@code Improvement#getPriceModifier()}.
     *
     * <p>0 by default, and 0 everywhere today: no Preço column is authored for an item ability
     * any more than for an Obra-Prima or an Aprimoramento (CLAUDE.md's "Item numeric columns"
     * gap). The <em>arithmetic</em> is real all the same — author a constant's modifier here and
     * every forge that binds it prices it, with no further wiring.
     */
    default int getPriceModifier() {
        return 0;
    }
}
