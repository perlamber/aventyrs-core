package org.aventyrs.core.magic.invocation;

import org.aventyrs.core.subordinate.SubordinateBenefit;

import java.util.List;

/**
 * What a cast of an invocation Magia asks the Cena for (core 0.1.7.6) — {@link SpellInvocationService#plan}'s answer,
 * one per {@link SpellInvocation.Shape}. Report-only, like every plan here: core holds no server, so a caller sends
 * each order on (a summon or a spawner) or opens the benefit choice (a Subordinado) and then calls {@link
 * SpellInvocationService#subordinate}.
 */
public sealed interface InvocationOrder {

    /**
     * Creatures to place, each its own participant controlled by the caster.
     *
     * @param creatures   one order per creature, in the order the Magia states them
     * @param enhancement what the caster's Títulos added to the plan — {@link SummonEnhancement#NONE} for none
     */
    record Summons(List<SummonOrder> creatures, SummonEnhancement enhancement) implements InvocationOrder {

        public Summons {
            creatures = List.copyOf(creatures);
            enhancement = enhancement == null ? SummonEnhancement.NONE : enhancement;
        }
    }

    /** A spawner to stand on the server — Totem de Gaea, Festim dos Mortos. */
    record Spawner(SpawnerOrder spawner) implements InvocationOrder {
    }

    /**
     * A touched body or animal to make a Subordinado: the caster picks one of offered, then the caller calls {@link
     * SpellInvocationService#subordinate}.
     */
    record Subordination(List<SubordinateBenefit> offered) implements InvocationOrder {

        public Subordination {
            offered = List.copyOf(offered);
        }
    }
}
