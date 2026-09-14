package org.aventyrs.core.effect;

/**
 * A {@link SpellEffect} that brings something into the Scene — a conjured creature, or an item
 * called into being.
 *
 * <p><b>No concrete implementation, and unlike {@link OffensiveEffect} this one is genuinely
 * blocked.</b> A Magia that invokes a creature describes its stat block in prose, because {@code
 * Spell} has no column pointing at a {@code MonsterTemplate} — the one exception the Magia
 * catalog makes to transcribing rules text verbatim. Until that link exists there is nothing for
 * an Invocation Effect to instantiate.
 *
 * <p>Two further pieces are missing even once it does: {@code Scene#addParticipant} would have to
 * be reached from inside an effect that today sees only its target's {@code CombatantSheet}, and
 * CLAUDE.md's "A summon acting on its summoner's roll" gap means nothing models the player then
 * rolling on the conjured creature's behalf.
 */
public interface InvocationEffect extends SpellEffect {
}
