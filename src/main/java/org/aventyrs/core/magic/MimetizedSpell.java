package org.aventyrs.core.magic;

import lombok.Builder;
import lombok.Getter;
import org.aventyrs.core.sheet.IllegalOperationException;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_MIMETIZED_SPELL;

/**
 * A spell a character may cast through a mimetizing Talento without learning it. It retains the
 * catalog {@link #spell} and only the terms changed by the mimetizing rule: a Determinação cost
 * and optional activation-time, duration, and self-targeting constraints.
 *
 * <p>This is deliberately <b>not</b> a {@link Spell}. Learned Magias remain in {@code
 * Character.spells} and use {@code SpellCastingService}; mimetized Magias live in {@code
 * Character.mimetizedSpells} and will be resolved by a dedicated casting service. Keeping the
 * two acquisition and casting paths separate prevents mimicry from satisfying a Magic Tree's
 * cap, climb, or branch gates.
 *
 * <p>It is permission data only: it neither charges PD nor executes a spell effect. The dedicated
 * service will validate this object is held by the caster and charge {@link
 * #determinationPointCost} before resolving the catalog spell's effect.
 */
@Getter
public final class MimetizedSpell {

    private final Spell spell;
    private final int determinationPointCost;
    private final ActivationTime activationTimeOverride;
    private final SpellDuration durationOverride;
    private final boolean selfOnly;

    @Builder
    public MimetizedSpell(final Spell spell, final int determinationPointCost,
                          final ActivationTime activationTimeOverride, final SpellDuration durationOverride,
                          final boolean selfOnly) {
        if (spell == null || determinationPointCost <= 0) {
            throw new IllegalOperationException(INVALID_MIMETIZED_SPELL);
        }
        this.spell = spell;
        this.determinationPointCost = determinationPointCost;
        this.activationTimeOverride = activationTimeOverride;
        this.durationOverride = durationOverride;
        this.selfOnly = selfOnly;
    }

    /** The effective activation time: the mimicry override, or the catalog spell's authored one. */
    public ActivationTime getEffectiveActivationTime() {
        return activationTimeOverride == null ? spell.getActivationTime() : activationTimeOverride;
    }

    /** The effective duration: the mimicry override, or the catalog spell's authored one. */
    public SpellDuration getEffectiveDuration() {
        return durationOverride == null ? spell.getDuration() : durationOverride;
    }
}
