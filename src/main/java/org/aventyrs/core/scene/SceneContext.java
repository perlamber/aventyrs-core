package org.aventyrs.core.scene;

import org.aventyrs.core.sheet.CombatantSheet;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * What a Character can currently perceive about nearby allies and enemies — a resolved
 * snapshot, not a live view: {@code allies}/{@code enemies} are the actual lists (typically
 * {@link Scene#getAllies}/{@link Scene#getEnemies}'s output at the moment this was built —
 * see {@link Scene#buildContext} for that common case — but this class itself doesn't hold or
 * query a {@code Scene}, so it stays a plain, cheap-to-construct value object: a test or a
 * caller without a live {@code Scene} can build one directly from any two lists). {@code
 * distances} is always supplied already-resolved by a caller too (same as {@link
 * InitiativeEntry}'s own {@code initiativeValue} isn't rolled by this core either): no
 * participant carries a position, so although {@code org.aventyrs.core.scene.grid} can convert
 * a hex distance into a {@link Range}, nothing here does it for a caller. The map only needs
 * entries for whichever
 * participants are actually close enough to matter; anyone missing is treated as out of range
 * by every {@code *Within} method here.
 *
 * <p>Passed downstream into an Interaction (see {@code AbstractSkillInteraction
 * #applyTo(CombatantSheet, SceneContext)}) so a bonus conditioned on proximity — e.g.
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
    private final List<CombatantSheet> allies;
    private final List<CombatantSheet> enemies;
    private final Map<CombatantSheet, Range> distances;
    private final TerrainType terrainType;
    private final boolean combatScene;
    private final int currentRound;
    private final boolean wonInitiative;
    /**
     * The combatant on the <b>other side</b> of the roll this context is built for, or {@code
     * null} when the roll opposes nobody (a plain Perícia check, or no active Scene).
     *
     * <p><b>Which side it is depends on the Perícia being rolled</b>, and that is deliberate
     * rather than two fields: on a Perícia de Ataque it is the <i>target</i> being attacked, and
     * on an Esquiva e Aparar roll it is the <i>attacker</i> being defended against. One
     * exchange has exactly one opponent, so a single reference describes it from either side —
     * a reader disambiguates with {@code SkillType#isAttackSkill()}, which every consumer
     * already has in hand.
     *
     * <p>This is what makes a clause conditioned on <i>who</i> is opposite expressible from a
     * {@code Feat}, which carries no per-roll parameters of its own — e.g. {@code
     * AnaoFeat#VANTAGEM_DE_TAMANHO}'s Defesa bonus against larger attackers and {@code
     * AnaoFeat#GLORIA_YMIRIANA}'s Vantagem against targets that are not smaller. It overlaps
     * with, but does not replace, {@code AbstractSkillInteraction}'s own {@code attackTarget}
     * parameter: that one reaches {@code SkillCompetencyAbility#resolveAttackRollBonus} on the
     * attack side only, while this reaches every hook that already takes a {@code SceneContext},
     * in both directions.
     */
    private final CombatantSheet opposedCharacter;
    private final UUID sceneId;

    public SceneContext(final List<CombatantSheet> allies, final List<CombatantSheet> enemies, final Map<CombatantSheet, Range> distances) {
        this(allies, enemies, distances, null);
    }

    /** Same as the 3-arg constructor, but also carrying the Scene's current {@code terrainType}. */
    public SceneContext(final List<CombatantSheet> allies, final List<CombatantSheet> enemies, final Map<CombatantSheet, Range> distances, final TerrainType terrainType) {
        this(allies, enemies, distances, terrainType, false, 0, false, null, null);
    }

    /**
     * Same as the 4-arg constructor, but also carrying combatScene/currentRound/wonInitiative —
     * for a roll that opposes nobody. Delegates down with a {@code null} {@code
     * opposedCharacter}, the same cascading shape the 3- and 4-arg constructors already use, so
     * a caller that does not care about the opposed combatant needs no placeholder argument.
     */
    public SceneContext(final List<CombatantSheet> allies, final List<CombatantSheet> enemies, final Map<CombatantSheet, Range> distances,
                         final TerrainType terrainType, final boolean combatScene, final int currentRound, final boolean wonInitiative) {
        this(allies, enemies, distances, terrainType, combatScene, currentRound, wonInitiative, null, null);
    }

    /**
     * The full form: everything the 7-arg constructor carries, plus {@link #opposedCharacter} —
     * the combatant on the other side of the roll this context is for. See {@link
     * Scene#buildContext} for the common case of resolving all of it from a live {@link Scene}.
     * A caller building a {@code SceneContext} directly (e.g. a test, or no active Scene at all)
     * that doesn't care about these gets sensible non-combat defaults from the shorter
     * constructors instead.
     */
    public SceneContext(final List<CombatantSheet> allies, final List<CombatantSheet> enemies, final Map<CombatantSheet, Range> distances,
                         final TerrainType terrainType, final boolean combatScene, final int currentRound, final boolean wonInitiative, final CombatantSheet opposedCharacter) {
        this(allies, enemies, distances, terrainType, combatScene, currentRound, wonInitiative, opposedCharacter, null);
    }

    /** The full snapshot form, including the identity of the Scene that produced it. */
    public SceneContext(final List<CombatantSheet> allies, final List<CombatantSheet> enemies, final Map<CombatantSheet, Range> distances,
                        final TerrainType terrainType, final boolean combatScene, final int currentRound,
                        final boolean wonInitiative, final CombatantSheet opposedCharacter, final UUID sceneId) {
        this.allies = allies;
        this.enemies = enemies;
        this.distances = distances;
        this.terrainType = terrainType;
        this.combatScene = combatScene;
        this.currentRound = currentRound;
        this.wonInitiative = wonInitiative;
        this.opposedCharacter = opposedCharacter;
        this.sceneId = sceneId;
    }

    public List<CombatantSheet> getAllies() {
        return allies;
    }

    public List<CombatantSheet> getEnemies() {
        return enemies;
    }

    /** How far other is from the acting Character, or {@code null} if that distance wasn't supplied. */
    public Range getDistanceTo(final CombatantSheet other) {
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
     * #applyTo(CombatantSheet, SceneContext)} as {@code countAlliesWithin(Range.ADJACENTE) *
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
    public List<CombatantSheet> getAlliesWithin(final Range maxRange) {
        return allies.stream().filter(ally -> isWithin(ally, maxRange)).collect(Collectors.toList());
    }

    /** Every enemy at maxRange or closer — same shape as {@link #getAlliesWithin}, for the enemy side. */
    public List<CombatantSheet> getEnemiesWithin(final Range maxRange) {
        return enemies.stream().filter(enemy -> isWithin(enemy, maxRange)).collect(Collectors.toList());
    }

    private boolean isWithin(final CombatantSheet sheet, final Range maxRange) {
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

    /** See {@link #opposedCharacter} — {@code null} when the roll opposes nobody. */
    public CombatantSheet getOpposedCharacter() {
        return opposedCharacter;
    }

    /** Identity of the Scene that produced this snapshot, or {@code null} for a direct context. */
    public UUID getSceneId() {
        return sceneId;
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
