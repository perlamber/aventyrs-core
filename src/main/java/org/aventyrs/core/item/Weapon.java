package org.aventyrs.core.item;

import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.skill.SkillType;

/**
 * An {@link Item} that can actually be swung — the only kind that has a Dano Base at all.
 *
 * <p>This exists so {@link #getDamageBase()} lives where it's meaningful instead of on every
 * {@code Item}. A pauldron has no Dano Base column, and a signature typed to {@code Item}
 * could only answer that question with a stand-in (1d6+0, indistinguishable from a real
 * dagger's). Typed to {@code Weapon}, the question can't be asked of a pauldron in the first
 * place: {@code DamageBaseService#getDamageBase} takes this type, so <b>the compiler refuses a
 * non-weapon</b> rather than a runtime guard rejecting one. Same enforcement-by-type discipline
 * as {@code CharacterSheet} vs {@code MonsterSheet} — there is deliberately no {@code
 * isWeapon()} flag and no check anywhere.
 *
 * <p>Everything else about a weapon is an ordinary {@code Item}: its Preço, Dureza, Raridade,
 * {@link ItemFavor} and even its DF/DM all behave identically and need no override. Only these
 * two columns are new, and both are declared abstract rather than defaulted — a type whose
 * whole reason to exist is having a Dano Base should have to say what it is, and what it takes
 * to swing.
 *
 * <p>Nothing validates that an implementation's {@link Item#getCategory()} is actually an
 * {@link ItemType#OFFENSIVE} one. That's the usual restraint: catalogs and builders in this
 * codebase are data holders, not gatekeepers.
 */
public interface Weapon extends Item {

    /**
     * The Dano Base this weapon deals — the starting row of {@link DamageBase}'s scale that its
     * wielder's own "+N Dano Base" grants are applied on top of, by {@code
     * org.aventyrs.core.character.services.DamageBaseService}.
     */
    DamageBase getDamageBase();

    /**
     * The Perícia that governs this weapon's use — Ataque Corpo a Corpo for a machado, Ataque à
     * Distância for an arco. This is a column of the weapon itself, not a per-swing decision:
     * it's what {@code DamageBaseService#getDamageBase(Character, Weapon)} scans by, selecting
     * which {@link org.aventyrs.core.skill.SkillExcellency} tiers (and which Perícia-scoped
     * Habilidade de Competência grants) can raise this weapon's Dano Base.
     *
     * <p>One weapon, one Perícia. A weapon whose rules text lets it be used with either — a
     * lança thrown rather than thrust — can't say so here; that would need a second column and
     * a per-swing choice, and no catalog entry needs it yet.
     */
    SkillType getSkillType();
}
