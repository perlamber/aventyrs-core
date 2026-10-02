package org.aventyrs.core.magic.invocation;

import org.aventyrs.core.scene.SceneSummon;

import java.util.List;

/**
 * What one invocation put into the Scene, and the PM it costs on top of the Magia's own Custo de Conjuração (core
 * 0.0.92) — Predador Regional's "+1PM", Canção de Flora's "+2PM para cada animal adicional". Reported, never spent: the
 * caller pays it with the cast.
 */
public record Invocation(List<SceneSummon> summons, int extraManaCost) {

    public Invocation {
        summons = List.copyOf(summons);
    }
}
