package org.aventyrs.core.character.services;

import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;

import java.util.Optional;

/**
 * Sacrifício (acoes-de-combate.txt 20261013, core 0.1.6.17) — "Apenas uma vez por Cena de Combate, quando um personagem
 * aliado adjacente for atacado você pode interceptar o ataque, sofrendo os danos e efeitos. O ataque interceptado
 * ignora suas Defesas, aplica o Efeito Crítico Menor natural e desencadeia Correntes de Efeitos."
 *
 * <p>Taking it spends the interceptor's Reação and its once-per-combat use; the caller then builds the attack against
 * the interceptor with {@code intercepted} set ({@code IncomingAttack}/{@code DeliveredAttack}), which applies the
 * table ruling of 2026-10-14: no defence, at least the Menor, every Corrente.
 */
public interface SacrificeService {

    /** The per-combat ledger key — {@code CombatantSheet#markAffectedThisCombat}. */
    String SACRIFICE_USE = "SACRIFICIO";

    /**
     * Why interceptor may not step in front of ally right now — a {@code TranslatableMessages} key — or empty.
     * interceptorContext is the interceptor's own snapshot: it must place ally adjacent.
     */
    Optional<String> refusalToSacrifice(CombatantSheet interceptor, CombatantSheet ally, SceneContext interceptorContext,
                                        int turnNumber);

    /**
     * Takes the Sacrifício: spends the Reação and the combat's use.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException with {@link #refusalToSacrifice}'s key
     */
    void sacrifice(CombatantSheet interceptor, CombatantSheet ally, SceneContext interceptorContext, int turnNumber);
}
