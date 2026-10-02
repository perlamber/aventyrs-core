package org.aventyrs.core.character.services;

import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.ego.EgoSetback;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * An Ego reaching zero (Ego plan Phase 9, core 0.0.82): "rolar 1d6 na tabela de reveses do Ego". Core never throws
 * dice — the caller asks {@code CombatantSheet#getOwedEgoSetbacks()} after anything that spends permanent points,
 * throws a d6 for each, and records it here. The setback then holds while the Ego's permanent points are 0.
 */
public interface EgoSetbackService {

    /**
     * Records domain's setback for face.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code NO_EGO_SETBACK_OWED} when domain isn't at zero or
     *         already rolled, {@code INVALID_DIE_ROLL} for a face off the d6
     */
    EgoSetback rollSetback(CombatantSheet sheet, EgoDomain domain, int face);
}
