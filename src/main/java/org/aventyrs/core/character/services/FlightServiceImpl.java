package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.Dice;
import org.aventyrs.core.character.FlightActivation;
import org.aventyrs.core.character.MovementMode;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.FlightEffect;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.aventyrs.core.util.TranslatableMessages.ALREADY_FLYING;
import static org.aventyrs.core.util.TranslatableMessages.CANNOT_FLY;
import static org.aventyrs.core.util.TranslatableMessages.FLIGHT_ACTIVATION_NOT_AVAILABLE;
import static org.aventyrs.core.util.TranslatableMessages.FLIGHT_ACTIVATION_REQUIRED;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_ACTION_POINTS;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_DETERMINATION_POINTS;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_FREE_ACTIONS;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_MAGIC_POINTS;

public class FlightServiceImpl implements FlightService {

    private final MovementService movementService = new MovementServiceImpl();
    private final ActionPointsService actionPointsService = new ActionPointsServiceImpl();
    private final FreeActionsService freeActionsService = new FreeActionsServiceImpl();
    private final MagicPointsService magicPointsService = new MagicPointsServiceImpl();
    private final DeterminationPointsService determinationPointsService = new DeterminationPointsServiceImpl();

    @Override
    public List<FlightActivation> getFlightActivations(@NonNull final CombatantSheet sheet) {
        Character character = sheet.getCharacter();
        List<FlightActivation> sources = new ArrayList<>();
        Race race = character.getRace();
        if (race != null && !sheet.getRacialTraitSuppression().suppressesPhysicalTraits()) {
            race.resolveFlightActivation(character, sheet).ifPresent(sources::add);
        }
        character.getFeats().forEach(feat -> feat.resolveFlightActivation(character, sheet).ifPresent(sources::add));
        return sources.stream().map(source -> adjusted(source, character, sheet)).toList();
    }

    /**
     * Every held Talento's adjustment, in a fixed order: the PM reduction; then a Duração replacement
     * (Benção de Flora), so a die added after it (Coração Alado) still lands; then no limit at all
     * (Eterno Viajante), which outranks both.
     */
    private FlightActivation adjusted(final FlightActivation activation, final Character character,
                                      final CombatantSheet sheet) {
        List<Feat> feats = character.getFeats();
        int magicReduction = feats.stream().mapToInt(feat -> feat.resolveFlightMagicPointReduction(character)).sum();
        FlightActivation result = magicReduction == 0 || activation.magicPointCost() == 0
                ? activation
                : activation.withMagicPointCost(activation.magicPointCost() - magicReduction);
        Dice duration = feats.stream()
                .map(feat -> feat.resolveFlightDurationOverride(character, sheet))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(result.duration());
        int extraDice = feats.stream().mapToInt(feat -> feat.resolveFlightExtraDurationDice(character)).sum();
        if (duration != null && extraDice > 0) {
            duration = new Dice(duration.count() + extraDice, duration.bonus());
        }
        if (feats.stream().anyMatch(feat -> feat.removesFlightDurationLimit(character))) {
            duration = null;
        }
        return result.withDuration(duration);
    }

    @Override
    public boolean isStressful(final SceneContext sceneContext) {
        return sceneContext != null && sceneContext.isCombatScene();
    }

    @Override
    public boolean requiresActivation(@NonNull final CombatantSheet sheet, final SceneContext sceneContext) {
        return isStressful(sceneContext) && !sheet.isFlying() && !hasUnpricedFlight(sheet)
                && !getFlightActivations(sheet).isEmpty();
    }

    /**
     * Voo held through something that states no price — a timed {@code GRANTS_FLIGHT} Blessing, a
     * Habilidade that {@code keepsFlying()}, a Raça or Talento whose Voo has no Efeito de Voo (a Forma
     * Metamórfica's, an Abantesma's), or a Perícia trait.
     */
    private boolean hasUnpricedFlight(final CombatantSheet sheet) {
        Character character = sheet.getCharacter();
        if (sheet.getTemporaryBonus(ModifierType.GRANTS_FLIGHT) > 0
                || character.getAttributeAbilities().stream().anyMatch(AttributeAbility::keepsFlying)) {
            return true;
        }
        Race race = character.getRace();
        if (race != null && !sheet.getRacialTraitSuppression().suppressesPhysicalTraits()
                && race.grantsMovementMode(MovementMode.FLIGHT)
                && race.resolveFlightActivation(character, sheet).isEmpty()) {
            return true;
        }
        boolean unpricedFeat = character.getFeats().stream()
                .anyMatch(feat -> (feat.grantsMovementMode(MovementMode.FLIGHT, character, sheet)
                        || feat.resolveMovementBaseOverride(MovementMode.FLIGHT, character) != null)
                        && feat.resolveFlightActivation(character, sheet).isEmpty());
        return unpricedFeat || org.aventyrs.core.skill.SkillCompetencyAbility.allFor(character, sheet).stream()
                .anyMatch(ability -> ability.grantsMovementMode(MovementMode.FLIGHT));
    }

    @Override
    public void takeOffFreely(@NonNull final CombatantSheet sheet, final SceneContext sceneContext) {
        if (!canFly(sheet)) {
            throw new IllegalOperationException(CANNOT_FLY);
        }
        if (requiresActivation(sheet, sceneContext)) {
            throw new IllegalOperationException(FLIGHT_ACTIVATION_REQUIRED);
        }
        sheet.setFlying(true);
    }

    private boolean canFly(final CombatantSheet sheet) {
        return sheet.isFlying() || movementService.hasMovementMode(sheet, MovementMode.FLIGHT)
                || sheet.getCharacter().getAttributeAbilities().stream().anyMatch(AttributeAbility::keepsFlying);
    }

    @Override
    public Integer takeOff(@NonNull final CombatantSheet sheet, @NonNull final FlightActivation activation,
                           final List<Integer> durationFaces, final int turnNumber) {
        if (sheet.isFlying()) {
            throw new IllegalOperationException(ALREADY_FLYING);
        }
        if (!getFlightActivations(sheet).contains(activation)) {
            throw new IllegalOperationException(FLIGHT_ACTIVATION_NOT_AVAILABLE);
        }
        // The dice are checked before a single cost: a wrong roll must refuse cleanly.
        Integer rounds = Optional.ofNullable(activation.duration())
                .map(dice -> Math.max(1, dice.total(durationFaces == null ? List.of() : durationFaces)))
                .orElse(null);
        checkActionCost(sheet, activation.actionCost(), turnNumber);
        Character character = sheet.getCharacter();
        if (magicPointsService.getCurrentMagicPoints(character, sheet) < activation.magicPointCost()) {
            throw new IllegalOperationException(NOT_ENOUGH_MAGIC_POINTS);
        }
        if (determinationPointsService.getCurrentDeterminationPoints(character, sheet)
                < activation.determinationPointCost()) {
            throw new IllegalOperationException(NOT_ENOUGH_DETERMINATION_POINTS);
        }
        sheet.spendMagicPoints(activation.magicPointCost());
        sheet.spendDeterminationPoints(activation.determinationPointCost());
        sheet.setFlying(true);
        sheet.applyEffect(new FlightEffect(activation.source(), rounds));
        return rounds;
    }

    private void checkActionCost(final CombatantSheet sheet, final ActionCost cost, final int turnNumber) {
        switch (cost.kind()) {
            case FIXED, DYNAMIC -> {
                if (actionPointsService.getMaxActionPoints(sheet, turnNumber) < cost.minimum()) {
                    throw new IllegalOperationException(NOT_ENOUGH_ACTION_POINTS);
                }
            }
            case FREE_ACTION -> {
                if (freeActionsService.getTotalFreeActions(sheet, turnNumber) < 1) {
                    throw new IllegalOperationException(NOT_ENOUGH_FREE_ACTIONS);
                }
            }
            case NONE, REACTION -> {
                // Nothing a take-off is priced in.
            }
        }
    }
}
