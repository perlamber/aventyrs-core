package org.aventyrs.core.sheet;

import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.modifier.ModifierType;

import lombok.Builder;
import lombok.Getter;

/**
 * The outcome of an {@link Interaction} applied via {@link Interactable#receiveInteraction}.
 * Different Interactions fill in different fields — e.g. a damage-dealing Interaction sets
 * {@code resultStatus}, while a Perícia test sets {@code skillRollBonus} — the rest stay
 * {@code null} when not applicable to that particular Interaction.
 */
@Getter @Builder(toBuilder = true)
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

    /**
     * A temporary bonus this roll produced for *someone else* to receive — e.g. {@code
     * ArtesCompetencyAbility#DOM_BARDICO}: motivating allies grants a bonus this roll
     * computes, but who actually receives it is resolved by a layer above this core, via
     * {@code org.aventyrs.core.scene.Scene#getAllies}. {@code null} when this Interaction
     * didn't grant one — same stays-{@code null}-when-not-applicable convention as every
     * other field here. When non-{@code null}, a caller is expected to call {@code
     * CharacterSheet#grantTemporaryBonus} on each intended recipient with this value,
     * {@link #temporaryBonusModifierType}, and {@link #temporaryBonusRounds}.
     */
    Integer temporaryBonusValue;

    /**
     * The {@link ModifierType} {@link #temporaryBonusValue} should be granted as — the broad
     * {@code ModifierType#SKILL_ROLL_BONUS} (DOM_BARDICO's own case — its rules text says
     * "rolagens de Perícias", unrestricted, rather than naming one specific Perícia) or one
     * specific Perícia's own type (e.g. {@code org.aventyrs.core.skill.SkillType#ATLETISMO
     * .getRollBonusType()}), matching {@link org.aventyrs.core.sheet.TemporaryBonus}'s own
     * field — this is a {@code ModifierType}, not a {@code SkillType}, for exactly that
     * reason: it's what {@code CharacterSheet#grantTemporaryBonus} actually takes, with no
     * extra mapping step for the caller to get wrong.
     */
    ModifierType temporaryBonusModifierType;

    /**
     * How many Rodadas {@link #temporaryBonusValue} lasts once granted — e.g. DOM_BARDICO's
     * own 1/2/3 Rodadas depending on the caster's Artes Graduação.
     */
    Integer temporaryBonusRounds;
}
