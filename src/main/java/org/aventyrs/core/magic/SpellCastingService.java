package org.aventyrs.core.magic;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Interaction;

/**
 * Orchestrates casting a Magia, which per the rules always involves two separate rolls: the
 * Perícia that actually delivers the spell (e.g. Ataque à Distância for a ranged spell,
 * Ataque Corpo-a-Corpo for a Toque spell) compared against the target's own GD, followed by a
 * Domínio do Mana roll compared against the Magia's own GD.
 *
 * <p>TODO: no {@code Magia} entity/list exists yet, so {@code castSpell} only computes both
 * rolls' bonuses/difficultyReductions — it doesn't yet know either roll's target GD, so it
 * can't resolve success/failure for either roll. This is also where an ability that reduces
 * the *delivery* roll's GD specifically (not Domínio do Mana's own) would eventually be wired
 * in once Magias exist — no current ability needs this (see CLAUDE.md's
 * "Casting a Magia" section for the history of the abilities this was originally built for).
 */
public interface SpellCastingService {
    SpellCastingResult castSpell(CombatantSheet target, Interaction<CombatantSheet> deliveryInteraction);
}
