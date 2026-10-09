package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.action.ActionKind;
import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.Optional;

import static org.aventyrs.core.util.TranslatableMessages.ACTION_PREVENTED_BY_CONDITION;
import static org.aventyrs.core.util.TranslatableMessages.NOT_PRONE;

public class StandUpServiceImpl implements StandUpService {

    private final MovementReactionService movementReactionService;

    public StandUpServiceImpl() {
        this(new MovementReactionServiceImpl());
    }

    public StandUpServiceImpl(final MovementReactionService movementReactionService) {
        this.movementReactionService = movementReactionService;
    }

    @Override
    public ActionCost getStandUpCost(@NonNull final Character character) {
        boolean free = character.getFeats().stream().anyMatch(feat -> feat.standsUpAsFreeAction(character));
        return free ? ActionCost.FREE_ACTION : STAND_UP_COST;
    }

    @Override
    public Optional<String> refusalToStandUp(@NonNull final CombatantSheet sheet, final SceneContext sceneContext) {
        // Only a Caído held for real — a Desacordado's still lies on the sheet and refuses below.
        if (sheet.getHeldConditions().stream().noneMatch(held -> held.getType() == ConditionType.CAIDO)) {
            return Optional.of(NOT_PRONE);
        }
        if (sheet.isActionPrevented(ActionKind.STAND_UP, sceneContext)) {
            return Optional.of(ACTION_PREVENTED_BY_CONDITION);
        }
        ActionKind priced = ActionKind.ofCost(getStandUpCost(sheet.getCharacter()));
        if (priced != null && sheet.isActionPrevented(priced, sceneContext)) {
            return Optional.of(ACTION_PREVENTED_BY_CONDITION);
        }
        return Optional.empty();
    }

    @Override
    public StandUpResult standUp(@NonNull final CombatantSheet sheet, final SceneContext sceneContext) {
        refusalToStandUp(sheet, sceneContext).ifPresent(refusal -> {
            throw new IllegalOperationException(refusal);
        });
        sheet.removeCondition(ConditionType.CAIDO);
        return new StandUpResult(
                getStandUpCost(sheet.getCharacter()).plusSurcharge(sheet.getActionPointSurcharge(sceneContext)),
                movementReactionService.getProvokedReactors(sheet, sceneContext, Manoeuvre.LEVANTAR_SE));
    }
}
