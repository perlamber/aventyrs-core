package org.aventyrs.core.effect;

/**
 * A {@link SpellEffect} that brings something into the Scene — a conjured creature, or an item
 * called into being.
 *
 * <p><b>Still no concrete implementation, deliberately.</b> An effect sees only its target's {@code
 * CombatantSheet}, while an invocation needs the {@code Scene} and the caster. Invocations are therefore a service
 * called after the cast: {@code magic.invocation.NatureInvocationService} (core 0.0.92) spawns the ALIADOS DA
 * NATUREZA creatures ({@code monster.summon.NatureSummon}) and places them with {@code Scene#addSummons}, each its own
 * participant that its caster's player controls and rolls for. Reanimar's Zumbi and the other trees' summons can
 * follow the same shape.
 */
public interface InvocationEffect extends SpellEffect {
}
