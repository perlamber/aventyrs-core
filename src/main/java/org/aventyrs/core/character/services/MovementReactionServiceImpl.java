package org.aventyrs.core.character.services;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.action.MovementProgress;
import org.aventyrs.core.action.ReactionCheckpoint;
import org.aventyrs.core.scene.grid.BoardPosition;
import org.aventyrs.core.scene.grid.HexGrid;
import java.util.Map;
import java.util.TreeMap;

import lombok.NonNull;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.ego.EgoAdvantage;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.SkillType;

import java.util.ArrayList;
import java.util.List;

public class MovementReactionServiceImpl implements MovementReactionService {

    private final AttackRangeService attackRangeService;
    private final CharacterSizeService sizeService = new CharacterSizeServiceImpl();

    public MovementReactionServiceImpl() {
        this(new AttackRangeServiceImpl());
    }

    public MovementReactionServiceImpl(final AttackRangeService attackRangeService) {
        this.attackRangeService = attackRangeService;
    }

    @Override
    public List<CombatantSheet> getProvokedReactors(@NonNull final CombatantSheet mover,
                                                     final SceneContext sceneContext,
                                                     final Manoeuvre manoeuvre,
                                                     final CombatantSheet chargeTarget,
                                                     final MovementProgress progress) {
        if (sceneContext == null || isExempt(mover.getCharacter(), manoeuvre, sceneContext)
                || reactionFreeSteps(mover.getCharacter(), manoeuvre, progress) > 0) {
            return List.of();
        }
        List<CombatantSheet> reactors = new ArrayList<>();
        for (CombatantSheet enemy : sceneContext.getEnemies()) {
            Range distance = sceneContext.getDistanceTo(enemy);
            if (distance != null && threatens(enemy, distance)
                    && !(manoeuvre == Manoeuvre.INVESTIDA && enemy == chargeTarget && !mayDefendPerimeterAsTarget(enemy))) {
                reactors.add(enemy);
            }
        }
        return List.copyOf(reactors);
    }

    @Override
    public List<ReactionCheckpoint> getRouteCheckpoints(@NonNull final CombatantSheet mover,
                                                        @NonNull final List<BoardPosition> route,
                                                        final Manoeuvre manoeuvre, final SceneContext moverContext,
                                                        @NonNull final Map<CombatantSheet, BoardPosition> positions,
                                                        final CombatantSheet chargeTarget) {
        return getRouteCheckpoints(mover, route, manoeuvre, moverContext, positions, chargeTarget, Map.of());
    }

    @Override
    public java.util.OptionalInt getMeleeReachUd(@NonNull final CombatantSheet sheet) {
        Character character = sheet.getCharacter();
        List<Weapon> inHand = new ArrayList<>(character.getDrawnWeapons());
        inHand.addAll(sheet.getNaturalWeapons());
        return inHand.stream()
                .filter(weapon -> weapon.getAttackSkillType() == SkillType.ATAQUE_CORPO_A_CORPO)
                .mapToInt(weapon -> attackRangeService.getEffectiveRangeInUnidadesDeDistancia(character, weapon))
                .max();
    }

    @Override
    public List<ReactionCheckpoint> getRouteCheckpoints(@NonNull final CombatantSheet mover,
                                                        @NonNull final List<BoardPosition> route,
                                                        final Manoeuvre manoeuvre, final SceneContext moverContext,
                                                        @NonNull final Map<CombatantSheet, BoardPosition> positions,
                                                        final CombatantSheet chargeTarget,
                                                        @NonNull final Map<CombatantSheet, Integer> knownReachUd,
                                                        final MovementProgress progress) {
        if (moverContext == null || route.isEmpty() || isExempt(mover.getCharacter(), manoeuvre, moverContext)) {
            return List.of();
        }
        return checkpoints(mover, route, manoeuvre, moverContext, positions, chargeTarget, knownReachUd,
                firstProvokingIndex(route, reactionFreeSteps(mover.getCharacter(), manoeuvre, progress)));
    }

    @Override
    public List<CombatantSheet> getHostilesInReach(@NonNull final CombatantSheet subject, @NonNull final BoardPosition at,
                                                   final SceneContext subjectContext,
                                                   @NonNull final Map<CombatantSheet, BoardPosition> positions,
                                                   @NonNull final Map<CombatantSheet, Integer> knownReachUd) {
        if (subjectContext == null) {
            return List.of();
        }
        return checkpoints(subject, List.of(at), null, subjectContext, positions, null, knownReachUd, 0).stream()
                .flatMap(checkpoint -> checkpoint.reactors().stream())
                .toList();
    }

    /**
     * The route's first position that may provoke once freeSteps of its steps are exempt: step k enters position k,
     * and a position provokes for the step into it or the step out of it. So index freeSteps provokes unless the
     * route ends there, and a route no longer than the window yields an index past its end — nobody reacts.
     */
    private static int firstProvokingIndex(final List<BoardPosition> route, final int freeSteps) {
        if (freeSteps <= 0) {
            return 0;
        }
        int last = route.size() - 1;
        return freeSteps < last ? freeSteps : route.size();
    }

    /**
     * How many of this movement's steps still sit inside the mover's Perfil de Ação window ({@code
     * ActionProfile#resolveReactionFreeSteps}), less what progress says was travelled already; 0 when not told, when
     * the act is not a movement, or for a combatant with no Perfil de Ação.
     */
    private static int reactionFreeSteps(final Character mover, final Manoeuvre manoeuvre,
                                         final MovementProgress progress) {
        ActionProfile profile = mover.getActionProfile();
        if (progress == null || profile == null || !Manoeuvre.isMovement(manoeuvre)) {
            return 0;
        }
        return Math.max(0, profile.resolveReactionFreeSteps(progress.movementIndex()) - progress.stepsTravelled());
    }

    private List<ReactionCheckpoint> checkpoints(final CombatantSheet mover, final List<BoardPosition> route,
                                                 final Manoeuvre manoeuvre, final SceneContext moverContext,
                                                 final Map<CombatantSheet, BoardPosition> positions,
                                                 final CombatantSheet chargeTarget,
                                                 final Map<CombatantSheet, Integer> knownReachUd,
                                                 final int fromIndex) {
        int moverRadius = sizeService.getEffectiveSizeCategory(mover).getBodyRadius();
        Map<Integer, List<CombatantSheet>> byIndex = new TreeMap<>();
        for (CombatantSheet enemy : moverContext.getEnemies()) {
            BoardPosition at = positions.get(enemy);
            if (at == null || manoeuvre == Manoeuvre.INVESTIDA && enemy == chargeTarget
                    && !mayDefendPerimeterAsTarget(enemy)) {
                continue;
            }
            int enemyRadius = sizeService.getEffectiveSizeCategory(enemy).getBodyRadius();
            for (int i = fromIndex; i < route.size(); i++) {
                int gap = Math.max(1, HexGrid.distance3d(route.get(i), at) - moverRadius - enemyRadius);
                // Correr: "terá sua Área de Ataque Corpo-a-Corpo reduzida para adjacente".
                if (manoeuvre == Manoeuvre.CORRER && gap > 1) {
                    continue;
                }
                Integer known = knownReachUd.get(enemy);
                if (known != null ? gap <= known : threatens(enemy, Range.fromUnidadesDeDistancia(gap))) {
                    byIndex.computeIfAbsent(i, index -> new ArrayList<>()).add(enemy);
                    break;
                }
            }
        }
        List<ReactionCheckpoint> checkpoints = new ArrayList<>();
        byIndex.forEach((index, reactors) -> checkpoints.add(new ReactionCheckpoint(index, route.get(index), reactors)));
        return List.copyOf(checkpoints);
    }

    /** Whether a held Talento lets the charge's own target Defender o Perímetro (Mestre Escudeiro). */
    private static boolean mayDefendPerimeterAsTarget(final CombatantSheet target) {
        return target.getCharacter().getFeats().stream()
                .anyMatch(feat -> feat.permitsPerimeterDefenceAsChargeTarget(target));
    }

    /**
     * Whether the manoeuvre itself, a Habilidade de Atributo the mover holds, or one of their Vantagens de Ego exempts
     * <em>this</em> movement. Habilidades are scanned across {@code Character#getAttributeAbilities()}, which folds in
     * the ones a Talento granted; Vantagens through {@link EgoAdvantage#exemptsFromMovementReactions}, which may read
     * the Scene ({@code InitiativeAdvantage#POSICIONAMENTO_ESTRATEGICO}'s first two Rodadas, core 0.1.6.4).
     *
     * The mover's Perfil de Ação exempts a whole movement through {@link ActionProfile#exemptsMovementFromReactions}
     * (Consciência Defensiva's odd Rodadas); its window over a movement's first hexes is {@link #reactionFreeSteps}.
     *
     * <p>There is no {@code Feat} pass: {@code MobilidadeFeat#MOVIMENTO_ACROBATICO}'s "sem provocar Reações" is its
     * own manoeuvre, {@link Manoeuvre#MOVIMENTO_ACROBATICO}, exempt here like the Reposicionar it replaces.
     */
    private boolean isExempt(final Character mover, final Manoeuvre manoeuvre, final SceneContext sceneContext) {
        // A Reposicionar never provokes: the manoeuvre's own rule, held by nobody.
        if (manoeuvre == Manoeuvre.REPOSICIONAR || manoeuvre == Manoeuvre.MOVIMENTO_ACROBATICO
                || manoeuvre == Manoeuvre.RECUO_DEFENSIVO) {
            return true;
        }
        ActionProfile profile = mover.getActionProfile();
        if (profile != null && profile.exemptsMovementFromReactions(manoeuvre, sceneContext)) {
            return true;
        }
        return mover.getAttributeAbilities().stream()
                .anyMatch((AttributeAbility ability) -> ability.exemptsFromMovementReactions(manoeuvre))
                || mover.getAllEgoAdvantages().stream()
                .anyMatch(advantage -> advantage.exemptsFromMovementReactions(manoeuvre, sceneContext));
    }

    /**
     * Whether this enemy threatens a combatant standing distance away: do they have anything in
     * hand that is swung as an Ataque Corpo-a-Corpo and reaches that far?
     *
     * <p>Drawn weapons <b>plus</b> Armas Naturais, and the split matters. A sheathed blade threatens
     * nobody — "utilizando uma arma" means drawn throughout this core ({@code
     * Character#drawnWeapons} is the in-hand subset of the equipment list) — while an Arma Natural
     * cannot be sheathed at all, so it always counts. Read through {@code
     * CombatantSheet#getNaturalWeapons()} rather than the {@code Character} view, so a Forma that
     * suppresses its holder's natural weapons or grants its own is already reflected.
     */
    private boolean threatens(final CombatantSheet enemy, final Range distance) {
        Character character = enemy.getCharacter();
        List<Weapon> inHand = new ArrayList<>(character.getDrawnWeapons());
        inHand.addAll(enemy.getNaturalWeapons());
        return inHand.stream().anyMatch(weapon -> reaches(character, weapon, distance));
    }

    /**
     * The Ataque-à-Distância exclusion and the reach test, in that order.
     *
     * <p>The reach comes from {@link AttackRangeService#getEffectiveRange(Character, Weapon)} — the
     * <em>band</em> form, which is the right one here twice over: this is an {@code isWithin}
     * question rather than a measurement (see that service's own note on the two forms), and a
     * {@code SceneContext} holds distances as bands to begin with. So a Lança outreaches an adaga
     * for this purpose, and a larger enemy outreaches a smaller one holding the same weapon.
     */
    private boolean reaches(final Character enemy, final Weapon weapon, final Range distance) {
        return weapon.getAttackSkillType() == SkillType.ATAQUE_CORPO_A_CORPO
                && distance.isWithin(attackRangeService.getEffectiveRange(enemy, weapon));
    }
}
