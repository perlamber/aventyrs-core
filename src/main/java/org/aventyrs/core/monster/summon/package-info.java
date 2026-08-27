/**
 * Invocações — creatures a Conjurador calls up, whose stat blocks are parameterized by whoever
 * did the calling.
 *
 * <p>{@link org.aventyrs.core.monster.summon.Zumbi} is the first and most basic. Each lives in
 * its own class rather than a shared catalog enum, because a summoned creature carries
 * per-instance choices an enum constant cannot hold — the Conjurador's Graduação, and often a
 * choice of its own (a Zumbi's Categoria de Tamanho is that of the corpse being animated).
 *
 * <p>The mechanism they share is {@link org.aventyrs.core.monster.SummonedMonsterTemplate}; read
 * its javadoc first, particularly for why the summoner arrives as a plain {@code int} Graduação
 * rather than as an entity, and why re-parameterizing returns a copy.
 */
package org.aventyrs.core.monster.summon;
