package org.aventyrs.core.magic.invocation;

import org.aventyrs.core.monster.summon.NatureSummon;

import java.util.List;

/**
 * What one invocation brings, before anything is placed (core 0.0.95) — the creatures (each at its Conjurador's
 * Graduação, its powers rolled), the exclusivity group they share, their Duração (the N of "Concentração + N" when
 * concentration), and the PM on top of the Magia's own cost. Since core 0.1.2 it also carries what {@link
 * NatureInvocationService#enhance} added: the PA and PD the summoner's opt-ins cost on top of the cast, how many
 * times over the Concentração upkeep is owed (Invocação Dupla), and the {@link SummonEnhancement} every creature
 * carries — whose reported clauses (comet, aura, arrival burst, wings) the caller resolves. {@link NatureInvocationService} places a plan in a core
 * {@code Scene}; a client whose Scene lives on a server sends it there instead.
 */
public record InvocationPlan(List<NatureSummon> creatures, String exclusivityGroup, Integer rounds,
                             boolean concentration, int extraManaCost, int extraActionPoints,
                             int extraDeterminationCost, int concentrationUpkeepMultiplier,
                             SummonEnhancement enhancement) {

    public InvocationPlan {
        creatures = List.copyOf(creatures);
        enhancement = enhancement == null ? SummonEnhancement.NONE : enhancement;
    }

    /** A plan as the Magia states it — no surcharge and no enhancement (before core 0.1.2, the only shape). */
    public InvocationPlan(final List<NatureSummon> creatures, final String exclusivityGroup, final Integer rounds,
                          final boolean concentration, final int extraManaCost) {
        this(creatures, exclusivityGroup, rounds, concentration, extraManaCost, 0, 0, 1, SummonEnhancement.NONE);
    }
}
