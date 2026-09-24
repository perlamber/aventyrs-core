package org.aventyrs.core.scene;

import java.util.Set;
import java.util.UUID;

/**
 * Roll-relevant environmental facts resolved for one SceneContext actor. Lighting and altitude
 * describe their surroundings; flight and submersion describe the actor's current state.
 *
 * <p>The two positional facts are the caller's too, since this core holds no positions:
 * {@code inDifficultTerrain} is whether the actor's own space is Terreno Difícil, and {@code
 * sharingSpaceWith} names the combatants (by sheet id) standing in that same space — Entre as
 * Pernas' "permanecer em um mesmo espaço ocupado por inimigo". Neither is judged here; a caller
 * that does not know leaves them at their "no" defaults.
 */
public record EnvironmentalState(
        LightLevel lightLevel,
        Altitude altitude,
        boolean flying,
        boolean atLeastHalfSubmerged,
        boolean inDifficultTerrain,
        Set<UUID> sharingSpaceWith
) {
    public static final EnvironmentalState ORDINARY =
            new EnvironmentalState(LightLevel.NORMAL, Altitude.ORDINARY, false, false);

    public EnvironmentalState {
        if (lightLevel == null || altitude == null) {
            throw new IllegalArgumentException("Environmental state requires light level and altitude.");
        }
        sharingSpaceWith = sharingSpaceWith == null ? Set.of() : Set.copyOf(sharingSpaceWith);
    }

    /** The surroundings-only form: not in Terreno Difícil, sharing no space. */
    public EnvironmentalState(final LightLevel lightLevel, final Altitude altitude, final boolean flying,
                              final boolean atLeastHalfSubmerged) {
        this(lightLevel, altitude, flying, atLeastHalfSubmerged, false, Set.of());
    }

    /** This state with the actor's positional facts replaced. */
    public EnvironmentalState withPosition(final boolean inDifficultTerrain, final Set<UUID> sharingSpaceWith) {
        return new EnvironmentalState(lightLevel, altitude, flying, atLeastHalfSubmerged, inDifficultTerrain,
                sharingSpaceWith);
    }

    /** Whether the actor stands in the same space as the combatant with sheetId. */
    public boolean isSharingSpaceWith(final UUID sheetId) {
        return sheetId != null && sharingSpaceWith.contains(sheetId);
    }
}
