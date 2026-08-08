package org.aventyrs.core.scene;

import org.aventyrs.core.sheet.CharacterSheet;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * What a Character can currently perceive about nearby allies and enemies — a resolved
 * snapshot, not a live view: {@code allies}/{@code enemies} are the actual lists (typically
 * {@link Scene#getAllies}/{@link Scene#getEnemies}'s output at the moment this was built —
 * see {@link Scene#buildContext} for that common case — but this class itself doesn't hold or
 * query a {@code Scene}, so it stays a plain, cheap-to-construct value object: a test or a
 * caller without a live {@code Scene} can build one directly from any two lists). This core
 * has no grid/positioning system of its own, so {@code distances} is always supplied
 * already-resolved by a caller too (same as {@link InitiativeEntry}'s own {@code
 * initiativeValue} isn't rolled by this core either) — it only needs entries for whichever
 * participants are actually close enough to matter; anyone missing is treated as out of range
 * by every {@code *Within} method here.
 *
 * <p>Passed downstream into an Interaction (see {@code AbstractSkillInteraction
 * #applyTo(CharacterSheet, SceneContext)}) so a bonus conditioned on proximity — e.g.
 * {@code MedicinaECuraExcellency#FOCADO}'s "se não tiver inimigos próximos (Distância
 * Curta)", or a hypothetical bonus scaling with {@link #countAlliesWithin}/{@link
 * #countEnemiesWithin} — can consult it once a consumer needs to.
 *
 * <p>{@code terrainType} is the same kind of already-resolved fact as {@code distances}: this
 * class doesn't infer it, {@link Scene#buildContext} just carries {@link Scene#getTerrainType()}
 * along. {@code null} means "terrain unset/not tracked" — every {@link #isTerrain} check is
 * {@code false} in that case, not an error.
 */
public class SceneContext {
    private final List<CharacterSheet> allies;
    private final List<CharacterSheet> enemies;
    private final Map<CharacterSheet, Range> distances;
    private final TerrainType terrainType;

    public SceneContext(final List<CharacterSheet> allies, final List<CharacterSheet> enemies, final Map<CharacterSheet, Range> distances) {
        this(allies, enemies, distances, null);
    }

    /** Same as the 3-arg constructor, but also carrying the Scene's current {@code terrainType}. */
    public SceneContext(final List<CharacterSheet> allies, final List<CharacterSheet> enemies, final Map<CharacterSheet, Range> distances, final TerrainType terrainType) {
        this.allies = allies;
        this.enemies = enemies;
        this.distances = distances;
        this.terrainType = terrainType;
    }

    public List<CharacterSheet> getAllies() {
        return allies;
    }

    public List<CharacterSheet> getEnemies() {
        return enemies;
    }

    /** How far other is from the acting Character, or {@code null} if that distance wasn't supplied. */
    public Range getDistanceTo(final CharacterSheet other) {
        return distances.get(other);
    }

    /** Whether any ally is at maxRange or closer — e.g. a "you have a close ally" condition. */
    public boolean hasAllyWithin(final Range maxRange) {
        return countAlliesWithin(maxRange) > 0;
    }

    /** Whether any enemy is at maxRange or closer — e.g. MedicinaECuraExcellency#FOCADO's own condition. */
    public boolean hasEnemyWithin(final Range maxRange) {
        return countEnemiesWithin(maxRange) > 0;
    }

    /**
     * How many allies are at maxRange or closer — e.g. a hypothetical "+1 bonus per adjacent
     * ally" scaling effect, computed by a subclass overriding {@code AbstractSkillInteraction
     * #applyTo(CharacterSheet, SceneContext)} as {@code countAlliesWithin(Range.ADJACENTE) *
     * perAllyBonus}, typically clamped to some maximum the ability's own rules text specifies.
     */
    public int countAlliesWithin(final Range maxRange) {
        return (int) allies.stream().filter(ally -> isWithin(ally, maxRange)).count();
    }

    /** How many enemies are at maxRange or closer — same shape as {@link #countAlliesWithin}, for maluses like "surrounded". */
    public int countEnemiesWithin(final Range maxRange) {
        return (int) enemies.stream().filter(enemy -> isWithin(enemy, maxRange)).count();
    }

    private boolean isWithin(final CharacterSheet sheet, final Range maxRange) {
        Range distance = distances.get(sheet);
        return distance != null && distance.isWithin(maxRange);
    }

    public TerrainType getTerrainType() {
        return terrainType;
    }

    /**
     * Whether this Scene's current terrain is one of anyOf — e.g. {@code
     * AnoesRacialAbility#FILHOS_DA_MONTANHA}'s "terrenos montanhosos ou cavernas" is {@code
     * isTerrain(TerrainType.MOUNTAIN, TerrainType.CAVE)}. Always {@code false} when {@code
     * terrainType} is {@code null} (unset).
     */
    public boolean isTerrain(final TerrainType... anyOf) {
        return terrainType != null && Arrays.asList(anyOf).contains(terrainType);
    }
}
