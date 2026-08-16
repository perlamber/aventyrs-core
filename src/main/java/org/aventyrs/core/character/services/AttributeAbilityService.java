package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.IllegalOperationException;

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
     * returns a **new** {@code Character} (this library's {@code Character} is an immutable
     * value object; the caller must start using the returned instance, the argument is not
     * mutated) with {@code ability} added to {@code attributeAbilities}, and, only when {@link
     * AttributeAbility#resolvePermanentEgoGain} reports one, that permanent Ego point already
     * applied too (e.g. {@code CharismaAbility#DESTINO_FAVORAVEL}'s permanent Sorte point) —
     * a caller never needs to check for or apply that separately.
     *
     * @throws IllegalOperationException if the ability was already chosen or no slot is free — character is left untouched
     */
    Character grantAttributeAbility(Character character, AttributeAbility ability) throws IllegalOperationException;
}
