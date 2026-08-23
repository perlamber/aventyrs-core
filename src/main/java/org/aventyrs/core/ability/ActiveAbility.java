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
public interface ActiveAbility {
    String getDescription();

    /** Pontos de Ação spent to trigger this ability's activated state. */
    int getActionPointCost();

    /** Pontos de Magia spent to trigger this ability's activated state. */
    int getMagicPointCost();

    /** How many Rodadas the activated state lasts once triggered. */
    int getDurationInRounds();

    /**
     * The {@link TemporaryEffect} this ability grants once activated, computed from
     * character's own current stats (e.g. their total Foco) — not yet applied to any
     * {@code CombatantSheet}.
     */
    TemporaryEffect resolveEffect(Character character);
}
