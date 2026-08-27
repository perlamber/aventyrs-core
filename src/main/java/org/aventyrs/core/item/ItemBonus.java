package org.aventyrs.core.item;

import org.aventyrs.core.modifier.ModifierType;

/**
 * One flat bonus an {@link ItemFavor} grants — a {@link ModifierType} and how much of it,
 * reusing the existing modifier taxonomy rather than inventing a parallel one, exactly as
 * {@code org.aventyrs.core.sheet.TemporaryBonus} and {@code org.aventyrs.core.sheet.Blessing}
 * already do for their own mechanisms.
 *
 * <p>It's neither of those two, though, and deliberately not merged into either: a {@code
 * TemporaryBonus} counts down in Rodadas and a {@code Blessing} carries a {@code TargetScope}
 * and a granting {@code source}, while an item's Favor is permanent for as long as the item is
 * carried and never applies to anyone but its wielder — so all three of those fields would be
 * dead weight here.
 *
 * <p>Use whichever {@code ModifierType} the Favor's rules text actually names — {@code
 * DAMAGE_REDUCTION} for RD, {@code ABSOLUTE_DAMAGE_REDUCTION} for RA, {@code DEFESAS},
 * {@code MOVEMENT}, the broad {@code SKILL_ROLL_BONUS}, or one specific Perícia's own
 * {@code <SKILL>_ROLL_BONUS} (see CLAUDE.md's "A ModifierType per skill"). A Favor whose
 * effect has no {@code ModifierType} at all yet stays as rules text on {@link
 * ItemFavor#getDescription()} until the mechanism it needs exists.
 */
public record ItemBonus(ModifierType modifierType, int value) {}
