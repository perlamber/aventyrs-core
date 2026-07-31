package org.aventyrs.core.sheet;

import org.aventyrs.core.character.CharacterStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * The outcome of an {@link Interaction} applied via {@link Interactable#receiveInteraction}.
 * Different Interactions fill in different fields — e.g. a damage-dealing Interaction sets
 * {@code resultStatus}, while a Perícia test sets {@code skillRollBonus} — the rest stay
 * {@code null} when not applicable to that particular Interaction.
 */
@Getter @Builder
public class InteractionResult {
    Interactable nextInteractable;
    CharacterStatus resultStatus;

    /** The Perícia roll bonus computed by a skill-test Interaction (e.g. AttentionInteraction). */
    Integer skillRollBonus;

    /**
     * Total GD (DifficultyLevel) steps reduced for this Perícia test, aggregated from
     * whatever's currently known on the CharacterSheet — for now, only the trained Skill's
     * unlocked {@link org.aventyrs.core.skill.SkillExcellency} tiers (e.g.
     * {@link org.aventyrs.core.skill.ArtesExcellency#PRODIGIO}). More sources (Talentos,
     * temporary buffs, etc.) would add to this same total as they're built.
     */
    Integer difficultyReduction;
}
