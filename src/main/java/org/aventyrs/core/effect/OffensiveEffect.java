package org.aventyrs.core.effect;

/**
 * A {@link SpellEffect} that harms its target — a Magia's damage, or a Malefício it inflicts.
 *
 * <p><b>No concrete implementation yet, and the gap is a consumer rather than a mechanism.</b>
 * Both halves already exist one step short of being applied:
 *
 * <ul>
 *   <li><b>Damage</b> — {@code SpellDamage}/{@code ResolvedSpellDamage} already resolve a Magia's
 *       {@code Efeito:} damage line against its caster, and {@code SpellCastingResult
 *       #getPrimaryDamage()} reports it. An Offensive Effect would be that figure fed into a
 *       {@code DamageInteraction} instead of handed back as a number — nothing new is needed
 *       beyond the caller's rolled dice, which this core never rolls.</li>
 *   <li><b>Debuffs</b> — {@code CombatantSheet#applyCondition} is real and {@code ConditionType}
 *       is fully authored, so inflicting a Malefício is the exact mirror of {@link
 *       ConditionCleansingEffect} lifting one.</li>
 * </ul>
 *
 * <p>It is left unimplemented deliberately, per CLAUDE.md's "build for the second real consumer":
 * the Magia trees whose principal effect is damage are not being wired yet, and an Offensive
 * Effect written ahead of one would be parameterized by guesswork rather than by a branch that
 * actually needs it.
 */
public interface OffensiveEffect extends SpellEffect {
}
