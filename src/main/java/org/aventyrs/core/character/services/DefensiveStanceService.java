package org.aventyrs.core.character.services;

import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.DefensiveStance;

import java.util.Optional;

/**
 * Postura Defensiva (acoes-de-combate.txt 20261013, core 0.1.6.19) — "Tempo de Ação: 2PA … Você recebe Bônus de +2 em
 * Defesas, este Bônus muda para +3 se estiver utilizando um Escudo." Until the holder's own next Turn begins (table
 * ruling, 2026-10-14); taking it again renews it rather than stacking. PA are reported, never deducted.
 */
public interface DefensiveStanceService {

    /** "Tempo de Ação: 2PA". */
    ActionCost STANCE_COST = ActionCost.ofActionPoints(2);

    /** Why sheet may not take the posture now — a {@code TranslatableMessages} key — or empty. */
    Optional<String> refusal(CombatantSheet sheet, SceneContext sceneContext);

    /**
     * Takes the posture.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException with {@link #refusal}'s key
     */
    DefensiveStance take(CombatantSheet sheet, SceneContext sceneContext);
}
