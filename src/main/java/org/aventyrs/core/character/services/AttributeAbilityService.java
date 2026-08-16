package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.SkillType;

import java.util.List;

public interface AttributeAbilityService {
    int FIRST_ABILITY_ATTRIBUTE_BASE = 3;
    int SECOND_ABILITY_ATTRIBUTE_BASE = 5;

    /**
     * How many attribute abilities a base of this magnitude has unlocked: 0 below
     * {@value #FIRST_ABILITY_ATTRIBUTE_BASE}, 1 from there on, 2 from
     * {@value #SECOND_ABILITY_ATTRIBUTE_BASE} on.
     */
    int getUnlockedAbilitySlots(int attributeBase);

    /**
     * Validates picking a new attribute ability: it must not already be chosen, and there
     * must be an unused slot unlocked by the attribute's current base.
     *
     * @throws IllegalOperationException if the ability was already chosen or no slot is free
     */
    void validateChoice(int attributeBase, List<AttributeAbility> alreadyChosen, AttributeAbility choice) throws IllegalOperationException;

    /**
     * The single entry point for actually acquiring an {@link AttributeAbility} — call this
     * (not {@link #validateChoice} alone) whenever a player picks one, in either of the two
     * moments that can happen:
     * <ul>
     *   <li><b>Character creation</b> — a starting {@code Character} whose Attribute bases
     *       already clear {@link #FIRST_ABILITY_ATTRIBUTE_BASE}/{@link
     *       #SECOND_ABILITY_ATTRIBUTE_BASE} (e.g. a high starting allocation) has slots to
     *       fill immediately.</li>
     *   <li><b>Character progression, after the fact</b> — most commonly, right after {@link
     *       CharacterAttributeService#upgradeBase} raises an Attribute's base past one of
     *       those same thresholds, unlocking a new slot the player then fills. This method
     *       takes no {@link org.aventyrs.core.sheet.CharacterSheet} and spends no experience
     *       itself, unlike {@code upgradeBase}/{@code SkillGraduationService#upgradeGraduation}
     *       — picking an already-unlocked attribute ability slot has no XP cost in this
     *       ruleset, only {@code upgradeBase} raising the base to unlock the slot does.</li>
     * </ul>
     *
     * <p>Internally: derives the relevant attribute's current base from {@code character}
     * itself (via {@code ability.getAttributeDomain()}), validates the choice through {@link
     * #validateChoice} <i>before</i> constructing anything — so a rejected call leaves the
     * {@code character} argument itself completely untouched, safe to keep using as-is — then
     * returns an {@link AttributeAbilityGrantResult} wrapping a **new** {@code Character}
     * (this library's {@code Character} is an immutable value object; the caller must start
     * using the returned instance, the argument is not mutated) with {@code ability} added to
     * {@code attributeAbilities}, and, only when {@link AttributeAbility#resolvePermanentEgoGain}
     * reports one, that permanent Ego point already applied too (e.g. {@code
     * CharismaAbility#DESTINO_FAVORAVEL}'s permanent Sorte point), and only when {@link
     * AttributeAbility#resolveActiveAbility} reports one, that {@code ActiveAbility} already
     * added to {@code activeAbilities} too (e.g. {@code FocusAbility#CONCENTRACAO_PROFUNDA}'s
     * own activatable state) — a caller never needs to check for or apply either separately.
     * The result's {@code pendingSkillTraitChoices} carries whatever {@link
     * AttributeAbility#resolvePendingSkillTraitChoices} reports for the granted ability (e.g.
     * {@code CharismaAbility#CHARME}'s own per-trained-Carisma-Perícia choices) — see {@link
     * #grantSkillTraitChoice} for resolving each one.
     *
     * @throws IllegalOperationException if the ability was already chosen or no slot is free — character is left untouched
     */
    AttributeAbilityGrantResult grantAttributeAbility(Character character, AttributeAbility ability) throws IllegalOperationException;

    /**
     * Resolves one {@link AttributeAbilityGrantResult#getPendingSkillTraitChoices()} entry —
     * e.g. one Carisma Perícia {@code CharismaAbility#CHARME} still owes a choice for — once
     * the player has picked which {@code SkillCompetencyAbility} and {@code
     * SkillSpecialization} they want for skillType. Adds competencyAbility to character's
     * {@code skillCompetencyAbilities} and specialization to the matching trained {@code
     * CharacterSkill}'s own {@code specializations}, returning a **new** {@code Character}
     * (the argument is not mutated).
     *
     * <p>Doesn't validate that skillType was actually a pending choice from some specific
     * {@link AttributeAbility} — this core doesn't track *why* a choice became available, same
     * restraint already applied to acquisition-legality checks elsewhere (e.g. no "Requer N
     * Graduações" enforcement) — only that skillType is actually trained, and that
     * competencyAbility/specialization both actually belong to it. Nor does it guard against
     * calling this twice for the same skillType — that would simply add a second Habilidade/
     * Especialização, since no cap on either exists for this today.
     *
     * @throws IllegalOperationException if skillType isn't trained, or if either trait belongs
     *         to a different SkillType — character is left untouched
     */
    Character grantSkillTraitChoice(Character character, SkillType skillType, SkillCompetencyAbility competencyAbility, SkillSpecialization specialization) throws IllegalOperationException;
}
