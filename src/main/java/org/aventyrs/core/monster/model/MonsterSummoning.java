package org.aventyrs.core.monster.model;

import org.aventyrs.core.monster.MonsterCategory;

/**
 * What a Habilidade Monstruosa invokes (core 0.1.6.26) — reported, never placed by the activation itself, which sees
 * no {@code Scene}: the caller hands it to {@code magic.invocation.UndeadInvocationService#invokeCarrascos}. Procissão
 * dos Desencarnados ("Invoca um Carrasco Moribundo Presa … por Concentração + 5 Rodadas") is the first.
 *
 * @param category      the Categoria of the generated Carrasco Moribundo ({@code monster.summon.CarrascoMorimbundo})
 * @param count         how many
 * @param rounds        their Duração — the N of "Concentração + N" when concentration
 * @param concentration whether they are held by Concentração
 */
public record MonsterSummoning(MonsterCategory category, int count, Integer rounds, boolean concentration) {
}
