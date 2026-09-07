package org.aventyrs.core.scene;

/** The illumination currently affecting a SceneContext's actor. */
public enum LightLevel {
    DARK,
    SHADOWED,
    NORMAL,
    BRIGHT;

    /** Whether this level represents a shadow-covered location or night. */
    public boolean isDarkOrShadowed() {
        return this == DARK || this == SHADOWED;
    }
}
