package org.aventyrs.core.character.services;

import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ContinuousNature;

/**
 * The Duração half of RD, RM, RE and RA (acoes-de-combate.txt, 20261013): each "Reduz a Duração de Efeitos …
 * contínuos em -1 Unidade (a redução de Duração não se acumula entre instâncias)".
 *
 * <ul>
 *   <li>RD — Físico, non-Primordial, damaging.</li>
 *   <li>RM — Mágico, non-Primordial.</li>
 *   <li>RE — Elemental, against the effect's own element (a Mágico Elemental one too).</li>
 *   <li>RA — any harmful continuous effect.</li>
 * </ul>
 *
 * <p>⚠️ A reading: each resistance that matches takes its own Unidade, so RD and RA together shorten a Sangramento
 * by 2 — the text forbids stacking only "entre instâncias" of one resistance. Applied once, as the effect lands, by
 * {@code CombatantSheet#applyEffect}; an effect it brings to 0 never takes hold.
 */
public interface ResistanceDurationService {

    /** How many Rodadas target's resistances take off an effect of nature — 0 when none applies. */
    int getDurationReduction(CombatantSheet target, ContinuousNature nature, SceneContext sceneContext);
}
