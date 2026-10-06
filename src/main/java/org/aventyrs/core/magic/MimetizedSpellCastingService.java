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

    /**
     * As {@link #cast(CombatantSheet, MimetizedSpell)}, paying in PV instead of PD when
     * payWithHitPoints — only where a held Título offers a price ({@code
     * AventyrTitle#resolveMimicryHitPointCost}, Bruxo's O Grande Bruxo). The PV go through {@code
     * CombatantSheet#payWithVitality}, so only a Descanso Verdadeiro recovers them, and the Título's
     * use is spent ({@code AventyrTitle#consumeMimicryHitPointPayment}).
     *
     * @throws IllegalOperationException {@code HIT_POINT_PAYMENT_NOT_PERMITTED} when no held Título
     *         offers a PV price, or anything the PD form throws
     */
    MimetizedSpellCastingResult cast(CombatantSheet caster, MimetizedSpell mimetizedSpell, boolean payWithHitPoints)
            throws IllegalOperationException;

    /**
     * The PV a cast of mimetizedSpell would cost caster instead of its PD right now, or empty when no
     * held Título permits it — what a client asks before offering the choice.
     */
    java.util.OptionalInt resolveHitPointCost(CombatantSheet caster, MimetizedSpell mimetizedSpell);
}
