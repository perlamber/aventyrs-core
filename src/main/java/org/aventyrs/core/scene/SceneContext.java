package org.aventyrs.core.scene;

import org.aventyrs.core.sheet.CharacterSheet;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
 *
 * <p>{@code combatScene}/{@code currentRound}/{@code wonInitiative} are the same kind of
 * already-resolved snapshot too, carried in by {@link Scene#buildContext} from {@link
 * Scene#isCombatScene()}/{@link Scene#getCurrentRound()}/{@link Scene#wonInitiative} — the
 * facts {@code EgoAdvantage#resolveConditionalRollBonus}/{@code #resolveDamageBonus} (e.g.
 * {@code InitiativeAdvantage#IMPETO}) need to condition a bonus on "the first two Rounds of a
 * Cena de Combate" and/or "the character won initiative."
 */
public class SceneContext {
    private final List<CharacterSheet> allies;
    private final List<CharacterSheet> enemies;
    private final Map<CharacterSheet, Range> distances;
    private final TerrainType terrainType;
    private final boolean combatScene;
    private final int currentRound;
    private final boolean wonInitiative;

    public SceneContext(final List<CharacterSheet> allies, final List<CharacterSheet> enemies, final Map<CharacterSheet, Range> distances) {
        this(allies, enemies, distances, null);
    }

    /** Same as the 3-arg constructor, but also carrying the Scene's current {@code terrainType}. */
    public SceneContext(final List<CharacterSheet> allies, final List<CharacterSheet> enemies, final Map<CharacterSheet, Range> distances, final TerrainType terrainType) {
        this(allies, enemies, distances, terrainType, false, 0, false);
    }

    /**
     * Same as the 4-arg constructor, but also carrying combatScene/currentRound/wonInitiative
     * — see {@link Scene#buildContext} for the common case of resolving these from a live
     * {@link Scene}. A caller building a {@code SceneContext} directly (e.g. a test, or no
     * active Scene at all) that doesn't care about these gets sensible non-combat defaults
     * from the shorter constructors instead.
     */
    public SceneContext(final List<CharacterSheet> allies, final List<CharacterSheet> enemies, final Map<CharacterSheet, Range> distances,
                         final TerrainType terrainType, final boolean combatScene, final int currentRound, final boolean wonInitiative) {
        this.allies = allies;
        this.enemies = enemies;
        this.distances = distances;
        this.terrainType = terrainType;
        this.combatScene = combatScene;
        this.currentRound = currentRound;
        this.wonInitiative = wonInitiative;
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

    /**
     * Every ally at maxRange or closer — e.g. a Título ability whose own condition needs to
     * inspect each qualifying ally individually (not just count/detect them), such as
     * {@code SantoAbility#BASTIAO_DOS_NECESSITADOS}'s own PV comparison against each adjacent
     * ally. Same filter as {@link #countAlliesWithin}, returning the matching sheets themselves
     * instead of just how many there are.
     */
    public List<CharacterSheet> getAlliesWithin(final Range maxRange) {
        return allies.stream().filter(ally -> isWithin(ally, maxRange)).collect(Collectors.toList());
    }

    /** Every enemy at maxRange or closer — same shape as {@link #getAlliesWithin}, for the enemy side. */
    public List<CharacterSheet> getEnemiesWithin(final Range maxRange) {
        return enemies.stream().filter(enemy -> isWithin(enemy, maxRange)).collect(Collectors.toList());
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

    /** Whether this Scene is currently a Cena de Combate. */
    public boolean isCombatScene() {
        return combatScene;
    }

    /** Which Round this Scene is currently on — see {@link Scene#getCurrentRound()}. */
    public int getCurrentRound() {
        return currentRound;
    }

    /** Whether the acting Character's own sub-group won initiative — see {@link Scene#wonInitiative}. */
    public boolean hasWonInitiative() {
        return wonInitiative;
    }

    /**
     * Whether this is a Cena de Combate currently in one of its first roundCount real
     * Rounds — e.g. {@code InitiativeAdvantage#IMPETO}'s "nas duas primeiras Rodadas de cada
     * Cena de Combate" is {@code isWithinFirstCombatRounds(2)}. Round 0 is {@link Scene}'s own
     * "before anyone has acted yet" starting value (see {@link Scene#getCurrentRound()}'s own
     * javadoc) and never counts as one of these — eligibility starts at Round 1, so {@code
     * roundCount=2} covers Rounds 1 and 2, not 0 and 1. Always {@code false} outside a Cena de
     * Combate, regardless of {@code currentRound}.
     */
    public boolean isWithinFirstCombatRounds(final int roundCount) {
        return combatScene && currentRound >= 1 && currentRound <= roundCount;
    }
}
