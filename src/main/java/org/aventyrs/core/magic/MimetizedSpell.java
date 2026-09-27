package org.aventyrs.core.magic;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.aventyrs.core.sheet.FormType;
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
@EqualsAndHashCode
public final class MimetizedSpell {

    private final Spell spell;
    private final int determinationPointCost;
    private final ActivationTime activationTimeOverride;
    private final SpellDuration durationOverride;
    private final boolean selfOnly;

    /**
     * The Forma the caster must be in to mimetize this Magia, or {@code null} — "Magias das Árvores
     * escolhidas só podem ser Mimetizadas enquanto em sua forma Feérica" ({@code
     * GorgonaFeat#ABENCOADA_PELO_CONCLAVE}). Checked by {@code MimetizedSpellCastingService}.
     */
    private final FormType requiredForm;

    /**
     * Value equality, since 0.0.66: a Talento whose grant is derived live ({@code
     * Character#getMimetizedSpells}) hands out a fresh instance on every call, and the cast
     * service recognises a held Magia by {@code contains}. A cost of 0 is valid — a Semente costs
     * 0 PM, so "PD em substituição aos PM" makes it free.
     */
    @Builder
    public MimetizedSpell(final Spell spell, final int determinationPointCost,
                          final ActivationTime activationTimeOverride, final SpellDuration durationOverride,
                          final boolean selfOnly, final FormType requiredForm) {
        if (spell == null || determinationPointCost < 0) {
            throw new IllegalOperationException(INVALID_MIMETIZED_SPELL);
        }
        this.spell = spell;
        this.determinationPointCost = determinationPointCost;
        this.activationTimeOverride = activationTimeOverride;
        this.durationOverride = durationOverride;
        this.selfOnly = selfOnly;
        this.requiredForm = requiredForm;
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
