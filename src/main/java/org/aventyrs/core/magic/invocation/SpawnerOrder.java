package org.aventyrs.core.magic.invocation;

import org.aventyrs.core.monster.summon.LacertoPower;

import java.util.List;

/**
 * The spawner an {@link InvocationOrder.Spawner} stands (core 0.1.7.6) — the figures a server's spawner needs.
 *
 * @param rounds                   how many Rodadas it keeps invoking
 * @param kind                     the creature it invokes ({@code NatureSummonKind} name), or {@code null} when its
 *                                 rule decides each one (Festim's 1d6)
 * @param conjuradorManaGraduation the caster's Domínio do Mana Graduação
 * @param powers                   Lacerto's powers for each creature; empty for the rest
 * @param summonRounds             each creature's own Duração, or {@code null} for the rule's
 * @param enhancement              what the caster's Títulos gave each creature — {@link SummonEnhancement#NONE}
 * @param rule                     {@link #FESTIM}, {@link #FESTIM_HORDA}, or {@code null} for one fixed kind
 * @param summonLimit              the caster's limit the rule fills up to, or {@code null}
 */
public record SpawnerOrder(int rounds, String kind, int conjuradorManaGraduation, List<LacertoPower> powers,
                           Integer summonRounds, SummonEnhancement enhancement, String rule, Integer summonLimit) {

    /** Festim dos Mortos' Sarcófago Negro. */
    public static final String FESTIM = "FESTIM";
    /** Festim dos Mortos' Horda Faminta. */
    public static final String FESTIM_HORDA = "FESTIM_HORDA";

    public SpawnerOrder {
        powers = powers == null ? List.of() : List.copyOf(powers);
        enhancement = enhancement == null ? SummonEnhancement.NONE : enhancement;
    }
}
