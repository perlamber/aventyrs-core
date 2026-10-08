package org.aventyrs.core.character.services;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Disease;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spreading a Doença — "Propagar doenças significa que personagens adjacentes, no início de cada
 * Rodada, devem rolar 1d6. Resultados 1 indicam que o personagem também é afetado pela doença."
 *
 * <p>Caller-driven, like everything positional here: at the start of each Rodada the caller asks
 * {@link #propagationCandidates} with the combatants adjacent to a holder (this core holds no
 * positions), throws a d6 for each, and hands each face to {@link #resolvePropagation}. The infecting
 * creature is immune to its own disease, and someone already Doente catches nothing more.
 */
public interface DiseaseService {

    /** The face that infects. */
    int INFECTING_FACE = 1;

    /** The spreading Doença holder holds, if any. */
    Optional<Disease> spreadingDisease(CombatantSheet holder);

    /** Who among adjacent must roll the d6 this Rodada — not yet Doente, and not the disease's own source. */
    List<CombatantSheet> propagationCandidates(CombatantSheet holder, Collection<CombatantSheet> adjacent);

    /**
     * Applies holder's spreading disease to target when face is {@link #INFECTING_FACE}. Returns whether
     * target caught it.
     */
    boolean resolvePropagation(CombatantSheet holder, CombatantSheet target, int face);
}
