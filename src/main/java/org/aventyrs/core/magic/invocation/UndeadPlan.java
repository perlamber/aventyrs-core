package org.aventyrs.core.magic.invocation;

import org.aventyrs.core.monster.SummonedMonsterTemplate;
import org.aventyrs.core.monster.summon.UndeadSummonKind;

/**
 * One undead an invocation brings (core 0.1.6.25) — the {@link InvocationPlan} twin for the REANIMAR tree, which keeps
 * its own record because {@code InvocationPlan} is typed to {@code NatureSummon} for its callers.
 *
 * @param kind             what to rebuild it as on another machine ({@link UndeadSummonKind#restore})
 * @param creature         its stat block, at the Conjurador's Graduação and the body's Categoria
 * @param exclusivityGroup what it may not coexist with, or {@code null}
 * @param rounds           its Duração — the N of "Concentração + N" when concentration; {@code null} for none
 * @param concentration    whether it is held by Concentração
 * @param extraManaCost    the PM on top of the Magia's own (Açougueiro's +5), reported, the caller's to pay
 * @param butcher          raised as Açougueiro
 */
public record UndeadPlan(UndeadSummonKind kind, SummonedMonsterTemplate creature, String exclusivityGroup,
                         Integer rounds, boolean concentration, int extraManaCost, boolean butcher) {
}
