package org.aventyrs.core.scene;

/**
 * Roll-relevant environmental facts resolved for one SceneContext actor. Lighting and altitude
 * describe their surroundings; flight and submersion describe the actor's current state.
 */
public record EnvironmentalState(
        LightLevel lightLevel,
        Altitude altitude,
        boolean flying,
        boolean atLeastHalfSubmerged
) {
    public static final EnvironmentalState ORDINARY =
            new EnvironmentalState(LightLevel.NORMAL, Altitude.ORDINARY, false, false);

    public EnvironmentalState {
        if (lightLevel == null || altitude == null) {
            throw new IllegalArgumentException("Environmental state requires light level and altitude.");
        }
    }
}
