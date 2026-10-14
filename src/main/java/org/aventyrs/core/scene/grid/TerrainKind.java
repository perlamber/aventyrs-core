package org.aventyrs.core.scene.grid;

/** What a hex is made of, as a movement sees it (table ruling, 2026-10-14). */
public enum TerrainKind {
    /** Ordinary ground. */
    GROUND,
    /** Water: entered by Nadar, a Natação Movimento Base, or flight; its depth is below its ground level. */
    WATER,
    /** A wall or cliff face: elevation is gained on it only by Escalar or flight. */
    CLIMBABLE,
    /** Blocks movement and a straight line; a flyer passes above its {@link TerrainCell#groundLevel()}. */
    OBSTACLE
}
