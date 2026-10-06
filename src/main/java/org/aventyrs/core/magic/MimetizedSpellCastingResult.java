package org.aventyrs.core.magic;

import lombok.Getter;

/** The paid, effective terms of one mimetized spell cast. */
@Getter
public final class MimetizedSpellCastingResult {
    private final MimetizedSpell mimetizedSpell;
    private final ActivationTime activationTime;
    private final SpellDuration duration;

    /** PV paid instead of the PD — O Grande Bruxo — or 0 for an ordinary PD cast. */
    private final int hitPointsPaid;

    public MimetizedSpellCastingResult(final MimetizedSpell mimetizedSpell) {
        this(mimetizedSpell, 0);
    }

    public MimetizedSpellCastingResult(final MimetizedSpell mimetizedSpell, final int hitPointsPaid) {
        this.mimetizedSpell = mimetizedSpell;
        this.hitPointsPaid = hitPointsPaid;
        this.activationTime = mimetizedSpell.getEffectiveActivationTime();
        this.duration = mimetizedSpell.getEffectiveDuration();
    }
}
