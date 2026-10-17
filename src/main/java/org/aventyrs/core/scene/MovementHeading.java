package org.aventyrs.core.scene;

import org.aventyrs.core.sheet.CombatantSheet;

import java.util.Set;
import java.util.UUID;

/**
 * Who a movement heads toward (core 0.1.7.21) — every combatant the move ends nearer to than it started, by the
 * caller's own geometry (this core holds no positions). Table ruling 2026-10-17: "em direção a" is read as ending at
 * a smaller hex distance. What {@link Scene#getHeadingMovementBonus} reads for {@code
 * org.aventyrs.core.feat.MobilidadeFeat#PORTA_ESTANDARTE}'s "+2UD sempre que se mover em direção a um inimigo" and
 * its allies' "+2UD para se moverem em sua direção".
 *
 * @param approachedSheetIds the {@link CombatantSheet#getId()} of each combatant the move ends nearer to
 */
public record MovementHeading(Set<UUID> approachedSheetIds) {

    /** A movement nearing nobody. */
    public static final MovementHeading NONE = new MovementHeading(Set.of());

    public MovementHeading {
        approachedSheetIds = approachedSheetIds == null ? Set.of() : Set.copyOf(approachedSheetIds);
    }

    /** Whether the move ends nearer to sheet. */
    public boolean approaches(final CombatantSheet sheet) {
        return sheet != null && approachedSheetIds.contains(sheet.getId());
    }
}
