package org.aventyrs.core.magic;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

/** Casts Talento-granted mimetized Magias without involving {@link SpellCastingService}. */
public interface MimetizedSpellCastingService {

    /**
     * Validates that caster holds mimetizedSpell, spends its PD cost, and returns its effective
     * casting terms. Resolving its catalog effect is deliberately a later dedicated pipeline.
     *
     * @throws IllegalOperationException if the caster was not granted this exact MimetizedSpell
     */
    MimetizedSpellCastingResult cast(CombatantSheet caster, MimetizedSpell mimetizedSpell)
            throws IllegalOperationException;
}
