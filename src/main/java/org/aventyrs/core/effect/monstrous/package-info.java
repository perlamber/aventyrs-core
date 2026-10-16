/**
 * The Correntes de Efeitos Monstruosas — the ones {@code docs/rules/criacao-de-monstros.txt} names inside a
 * Habilidade Monstruosa ("recebem a Corrente de Efeitos Monstruosa – Ofuscar"), as opposed to the 17 generic ones of
 * {@code efeitos-criticos.txt} in {@code org.aventyrs.core.effect}.
 *
 * <p>Each is an {@link org.aventyrs.core.effect.EffectChain}, built by the Habilidade's {@code
 * MonstrousAbility#resolveAttackEffectChains} and carried by the holder's attack through {@code
 * MonstrousAbilityGrant#attackEffectChains} (core 0.1.7.2). They land like any other Corrente: after a hit clears the
 * target's Defesa by the Resistência à Correntes de Efeitos margin.
 */
package org.aventyrs.core.effect.monstrous;
