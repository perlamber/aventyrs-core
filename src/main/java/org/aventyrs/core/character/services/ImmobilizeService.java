package org.aventyrs.core.character.services;

import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;

import java.util.Optional;

/**
 * Imobilizar (acoes-de-combate.txt 20261013, core 0.1.6.19) — "Tempo de Ação: 3PA. Imobiliza um alvo, impedindo-o de
 * agir. Apenas alvos Rendidos ou Inconscientes podem ser imobilizados. Alvo recebe a Condição Imobilizado."
 *
 * <p>No roll: the target is helpless. Rendido is the voluntary Condição (table ruling, 2026-10-14); Inconsciente is PV
 * below 0 ({@code CharacterStatus} past {@code LOW_LIFE}). The target must be adjacent. The Imobilização is sourced to
 * the captor, so its escape is the Resistida one ({@code GrappleService#escapeImmobilization}). Outside combat it
 * costs nothing (a prisoner tied up).
 */
public interface ImmobilizeService {

    /** "Tempo de Ação: 3PA". */
    ActionCost IMMOBILIZE_COST = ActionCost.ofActionPoints(3);

    /** Why captor may not immobilize target now — a {@code TranslatableMessages} key — or empty. */
    Optional<String> refusal(CombatantSheet captor, CombatantSheet target, SceneContext captorContext);

    /**
     * Immobilizes target, open-ended, sourced to captor.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException with {@link #refusal}'s key
     */
    void immobilize(CombatantSheet captor, CombatantSheet target, SceneContext captorContext);
}
