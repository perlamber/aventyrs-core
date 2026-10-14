package org.aventyrs.core.character.services;

import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.TemporaryBonus;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.SkillRoll;

import java.util.Optional;

/**
 * Encontrão (acoes-de-combate.txt 20261013, core 0.1.6.19) — "Tempo de Ação: 3PA. Permite se mover e empurrar um alvo.
 * Distância mínima igual ao Movimento Base padrão … máxima igual ao dobro do Movimento Base atual … em linha reta …
 * Em caso de sucesso alvo é Empurrado 1UD para trás, se for um Acerto Crítico … 2UD. Recebe a Corrente de Efeitos –
 * Lançar ao Chão. Em caso de falha sofre Redutor de -2 em Defesas por 1 Rodada."
 *
 * <p>The movement, its line and its distances are {@code ChargeService}'s, shared with the Investida. The roll is an
 * Ataque Corpo-a-Corpo that deals no damage (a reading approved with the 2026-10-14 plan) against the target's Defesa;
 * Lançar ao Chão lands at the Corrente margin. The push itself is the caller's, through {@code ForcedMovementService}.
 */
public interface ShoveChargeService {

    /** "Empurrado 1UD para trás". */
    int PUSH_UD = 1;
    /** "se for um Acerto Crítico alvo é Empurrado 2UD". */
    int CRITICAL_PUSH_UD = 2;

    /** What it costs — the Investida's price. */
    ActionCost getCost(CombatantSheet sheet, SceneContext sceneContext);

    /** Why sheet may not charge now — a {@code TranslatableMessages} key — or empty. */
    Optional<String> refusal(CombatantSheet sheet, SceneContext sceneContext);

    /**
     * Resolves the Encontrão: claims the movement, rolls, and on a hit reports the push and applies Lançar ao Chão at
     * the Corrente margin; on a miss applies the -2 Defesas.
     *
     * @param targetDefense the target's Defesa Física (a foe's own, or a sheet's from {@code DefenseService})
     * @throws org.aventyrs.core.sheet.IllegalOperationException with {@link #refusal}'s key
     */
    ShoveResult shove(CombatantSheet attacker, CombatantSheet target, int targetDefense, SceneContext sceneContext,
                      SkillRoll roll);

    /**
     * What the Encontrão did.
     *
     * @param pushUd       how far the target is pushed — 0 on a miss
     * @param knockedDown  whether Lançar ao Chão landed (the target is Caído)
     * @param missPenalty  the -2 Defesas granted on a miss, or {@code null}
     */
    record ShoveResult(ActionCost actionCost, int total, int required, boolean hit, int pushUd, boolean knockedDown,
                       TemporaryBonus missPenalty, InteractionResult rollResult) {
    }
}
