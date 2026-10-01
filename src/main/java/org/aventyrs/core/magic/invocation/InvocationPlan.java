package org.aventyrs.core.magic.invocation;

import org.aventyrs.core.monster.summon.NatureSummon;

import java.util.List;

/**
 * What one invocation brings, before anything is placed (core 0.0.95) — the creatures (each at its Conjurador's
 * Graduação, its powers rolled), the exclusivity group they share, their Duração (the N of "Concentração + N" when
 * concentration), and the PM on top of the Magia's own cost. {@link NatureInvocationService} places a plan in a core
 * {@code Scene}; a client whose Scene lives on a server sends it there instead.
 */
public record InvocationPlan(List<NatureSummon> creatures, String exclusivityGroup, Integer rounds,
                             boolean concentration, int extraManaCost) {

    public InvocationPlan {
        creatures = List.copyOf(creatures);
    }
}
