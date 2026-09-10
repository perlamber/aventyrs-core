package org.aventyrs.core.scene;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.scene.grid.GridPosition;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * A lasting Área de Efeito Magia registered on a Scene rather than on one CombatantSheet.
 *
 * <p>A null {@link #remainingRounds} represents its concentration phase. The trailing duration
 * remains available for the future concentration-break transition.
 */
@Getter
public class ActiveAreaSpellEffect {
    private final Spell spell;
    private final CombatantSheet caster;
    private final GridPosition positionTarget;
    private final int trailingDurationInRounds;
    private Integer remainingRounds;

    public ActiveAreaSpellEffect(@NonNull final Spell spell, @NonNull final CombatantSheet caster,
                                 final GridPosition positionTarget, final int durationInRounds) {
        this.spell = spell;
        this.caster = caster;
        this.positionTarget = positionTarget;
        this.trailingDurationInRounds = durationInRounds;
        this.remainingRounds = spell.getDuration().concentration() ? null : durationInRounds;
    }

    /** Advances a finite effect by one Scene Round. */
    void tick() {
        if (remainingRounds != null) {
            remainingRounds--;
        }
    }

    /** Whether this effect's finite duration has elapsed. */
    boolean isExpired() {
        return remainingRounds != null && remainingRounds <= 0;
    }
}
