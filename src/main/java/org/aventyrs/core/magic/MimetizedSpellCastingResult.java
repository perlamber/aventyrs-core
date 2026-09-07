package org.aventyrs.core.magic;

import lombok.Getter;

/** The paid, effective terms of one mimetized spell cast. */
@Getter
public final class MimetizedSpellCastingResult {
    private final MimetizedSpell mimetizedSpell;
    private final ActivationTime activationTime;
    private final SpellDuration duration;

    public MimetizedSpellCastingResult(final MimetizedSpell mimetizedSpell) {
        this.mimetizedSpell = mimetizedSpell;
        this.activationTime = mimetizedSpell.getEffectiveActivationTime();
        this.duration = mimetizedSpell.getEffectiveDuration();
    }
}
