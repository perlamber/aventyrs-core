package org.aventyrs.core.magic;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Interaction;

/**
 * Orchestrates casting a Magia, which per the rules always involves two separate rolls: the
 * Perícia that actually delivers the spell (e.g. Ataque à Distância for a ranged spell,
 * Ataque Corpo-a-Corpo for a Toque spell) compared against the target's own GD, followed by a
 * Domínio do Mana roll compared against the Magia's own GD.
 *
 * <p>TODO: {@code castSpell} only computes both rolls' bonuses/difficultyReductions — it doesn't
 * yet know either roll's target GD, so it can't resolve success/failure for either. <b>The Magia
 * half of that is now closed</b>: {@code org.aventyrs.core.magic.catalog.SpellCatalog} holds all
 * 145 authored Magias and {@link Spell#getCastingDifficultyLevel()} is real data, so the second
 * roll's GD is available the moment {@code castSpell} is given the {@code Spell} itself (see the
 * next TODO). What is still genuinely missing is the <em>delivery</em> roll's GD, which is the
 * target's Defesa Mágica — an authored flat number on a {@code MonsterSheet} that nothing
 * compares a roll against.
 *
 * <p>Two of the authored GDs additionally are not a fixed tier: {@link
 * Spell#isCastingDifficultyFlooredByTargetMagicDefense()} makes the target's DM a floor, and
 * {@link Spell#getCastingDifficultyAgainst} scales the GD to the rung of the effect being undone.
 * Both are data with no reader, and both are the same missing DM comparison.
 *
 * <p>This is also where an ability that reduces the *delivery* roll's GD specifically (not
 * Domínio do Mana's own) would eventually be wired in — no current ability needs this (see
 * CLAUDE.md's "Casting a Magia" section for the history of the abilities this was originally
 * built for).
 *
 * <p>TODO: {@code castSpell} takes an already-built delivery {@link Interaction} and reaches it
 * through {@code CombatantSheet#receiveInteraction} — the 1-arg {@code applyTo} — so the {@link
 * Spell} never reaches the delivery roll as the {@code
 * org.aventyrs.core.skill.AttackSource} it now is. A delivery-scoped ability like {@code
 * AtaqueADistanciaCompetencyAbility#ARREMESSO_PODEROSO} therefore does not fire on this path; a
 * caller who needs it rolls the delivery through {@code org.aventyrs.core.combat.AttackDelivery},
 * passing the Magia itself as the attack's source. Closing this needs {@code castSpell} to take
 * the {@code Spell} rather than a pre-built Interaction — the same change the missing target-GD
 * resolution above will force, and now a cheap one, since the Magia is already the value the roll
 * wants.
 */
public interface SpellCastingService {
    SpellCastingResult castSpell(CombatantSheet target, Interaction<CombatantSheet> deliveryInteraction);
}
