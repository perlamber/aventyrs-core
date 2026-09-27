package org.aventyrs.core.character.services;

import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * Presença de Carmilla's per-Rodada drain — "a cada Rodada você recupera 1PV para cada personagem
 * vivo em Distância Muito Curta que esteja ferido (que tenha perdido 1 ou mais PV), este é um efeito
 * de Roubo de Vida. A Distância aumenta em +1 nível para cada Título Aventyr que você possuir."
 *
 * <p>Caller-driven like every Rodada trigger that needs the Scene: call {@link #drain} once per
 * Rodada at the holder's Turn start, with the holder's own snapshot. It does nothing unless the Poder
 * is running ({@code sheet.CarmillaPresence}).
 */
public interface VampiricPresenceService {

    /**
     * Heals vampire 1PV per wounded, living combatant — ally or enemy — within the Presença's reach,
     * and returns the PV recovered. 0 when the Poder is not running or the context is {@code null}.
     */
    int drain(CombatantSheet vampire, SceneContext vampireContext);
}
