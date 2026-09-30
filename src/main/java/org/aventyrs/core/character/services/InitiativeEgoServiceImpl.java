package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.ego.InitiativeRollCharge;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.EgoPointType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;

import java.util.OptionalInt;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_INITIATIVE_CHANGE;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_EGO_POINTS;
import static org.aventyrs.core.util.TranslatableMessages.NO_INITIATIVE_CHARGE_BANKED;

public class InitiativeEgoServiceImpl implements InitiativeEgoService {

    private final EgoPointsService egoPointsService;

    public InitiativeEgoServiceImpl() {
        this(new EgoPointsServiceImpl());
    }

    public InitiativeEgoServiceImpl(final EgoPointsService egoPointsService) {
        this.egoPointsService = egoPointsService;
    }

    @Override
    public void lowerInitiative(@NonNull final Scene scene, @NonNull final CombatantSheet sheet, final int newValue,
                                @NonNull final Span span) {
        int current = currentOf(scene, sheet);
        if (newValue >= current) {
            throw new IllegalOperationException(INVALID_INITIATIVE_CHANGE);
        }
        pay(sheet, EgoPointType.TEMPORARY);
        sheet.overrideInitiative(newValue, rodadas(span));
    }

    @Override
    public void rerollInitiative(@NonNull final Scene scene, @NonNull final CombatantSheet sheet, final int newTotal) {
        currentOf(scene, sheet);
        pay(sheet, EgoPointType.TEMPORARY);
        sheet.overrideInitiative(newTotal + Skill.ADVANTAGE_BONUS, null);
    }

    @Override
    public void gainActionPoint(@NonNull final CombatantSheet sheet) {
        pay(sheet, EgoPointType.TEMPORARY);
        sheet.grantTemporaryBonus(ModifierType.ACTION_POINTS, 1, 1);
    }

    @Override
    public void gainReaction(@NonNull final CombatantSheet sheet) {
        pay(sheet, EgoPointType.TEMPORARY);
        sheet.grantTemporaryBonus(ModifierType.REACTIONS, 1, 1);
    }

    @Override
    public void bankRollCharges(@NonNull final CombatantSheet sheet, @NonNull final InitiativeRollCharge charge) {
        pay(sheet, charge.getPointType());
        for (int use = 0; use < InitiativeRollCharge.USES_PER_POINT; use++) {
            sheet.grantCharge(charge);
        }
    }

    @Override
    public SkillRoll useRollCharge(@NonNull final CombatantSheet sheet, @NonNull final SkillRoll roll,
                                   @NonNull final InitiativeRollCharge charge) {
        if (!sheet.consumeCharge(charge)) {
            throw new IllegalOperationException(NO_INITIATIVE_CHARGE_BANKED);
        }
        return roll.withInitiativeCharge(charge);
    }

    @Override
    public void setInitiative(@NonNull final Scene scene, @NonNull final CombatantSheet sheet, final int newValue,
                              @NonNull final Span span) {
        currentOf(scene, sheet);
        pay(sheet, EgoPointType.PERMANENT);
        sheet.overrideInitiative(newValue, rodadas(span));
    }

    @Override
    public void setOpponentInitiative(@NonNull final Scene scene, @NonNull final CombatantSheet payer,
                                      @NonNull final CombatantSheet opponent, final int newValue) {
        currentOf(scene, opponent);
        pay(payer, EgoPointType.PERMANENT);
        opponent.overrideInitiative(newValue, OPPONENT_RODADAS);
    }

    @Override
    public void gainActionSurge(@NonNull final CombatantSheet sheet) {
        pay(sheet, EgoPointType.PERMANENT);
        sheet.grantTemporaryBonus(ModifierType.ACTION_POINTS, SURGE_ACTION_POINTS, SURGE_RODADAS);
        sheet.grantTemporaryBonus(ModifierType.REACTIONS, SURGE_REACTIONS, SURGE_RODADAS);
    }

    private static int currentOf(final Scene scene, final CombatantSheet sheet) {
        OptionalInt current = scene.effectiveInitiativeOf(sheet);
        if (current.isEmpty()) {
            throw new IllegalOperationException(INVALID_INITIATIVE_CHANGE);
        }
        return current.getAsInt();
    }

    private static Integer rodadas(final Span span) {
        return span == Span.RODADA ? 1 : null;
    }

    /** One Iniciativa point of type, or a refusal with nothing spent. */
    private void pay(final CombatantSheet sheet, final EgoPointType type) {
        int held = type == EgoPointType.PERMANENT
                ? sheet.getPermanentEgoPoints(EgoDomain.INICIATIVA)
                : sheet.getTemporaryEgoPoints(EgoDomain.INICIATIVA);
        if (held < 1) {
            throw new IllegalOperationException(NOT_ENOUGH_EGO_POINTS);
        }
        // No Vantagem de Iniciativa reacts to a spend with a die, so any legal face.
        egoPointsService.useEgoPointsForEffect(sheet, EgoDomain.INICIATIVA, type, 1, EgoPointsService.MIN_DIE_FACE);
    }
}
