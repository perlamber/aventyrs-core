package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.action.ActionKind;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.PreparedAction;
import org.aventyrs.core.sheet.PreparedActionKind;

import java.util.Optional;

import static org.aventyrs.core.util.TranslatableMessages.ACTION_PREVENTED_BY_CONDITION;
import static org.aventyrs.core.util.TranslatableMessages.NO_PREPARED_ACTION;
import static org.aventyrs.core.util.TranslatableMessages.PREPARED_ACTION_NOT_PREPARABLE;
import static org.aventyrs.core.util.TranslatableMessages.PREPARED_ACTION_TOO_COSTLY;

public class PreparationServiceImpl implements PreparationService {

    @Override
    public Optional<String> refusalToPrepare(@NonNull final CombatantSheet sheet, @NonNull final ActionCost preparedCost,
                                             final SceneContext sceneContext) {
        if (preparedCost.kind() == ActionCost.Kind.REACTION || preparedCost.kind() == ActionCost.Kind.NONE) {
            return Optional.of(PREPARED_ACTION_NOT_PREPARABLE);
        }
        if ((preparedCost.kind() == ActionCost.Kind.FIXED || preparedCost.kind() == ActionCost.Kind.DYNAMIC)
                && preparedCost.minimum() > MAXIMUM_PREPARED_COST) {
            return Optional.of(PREPARED_ACTION_TOO_COSTLY);
        }
        ActionKind priced = ActionKind.ofCost(PREPARATION_COST);
        if (priced != null && sheet.isActionPrevented(priced, sceneContext)) {
            return Optional.of(ACTION_PREVENTED_BY_CONDITION);
        }
        return Optional.empty();
    }

    @Override
    public PreparedAction prepare(@NonNull final CombatantSheet sheet, @NonNull final PreparedActionKind kind,
                                  @NonNull final String key, @NonNull final String label,
                                  @NonNull final ActionCost preparedCost, final SceneContext sceneContext) {
        refusalToPrepare(sheet, preparedCost, sceneContext).ifPresent(refusal -> {
            throw new IllegalOperationException(refusal);
        });
        PreparedAction prepared = new PreparedAction(kind, key, label, preparedCost);
        sheet.applyEffect(prepared);
        return prepared;
    }

    @Override
    public boolean canFire(@NonNull final CombatantSheet sheet, final SceneContext sceneContext) {
        return sheet.getPreparedAction().isPresent()
                && !sheet.isActionPrevented(ActionKind.REACTION, sceneContext);
    }

    @Override
    public PreparedAction fire(@NonNull final CombatantSheet sheet, final SceneContext sceneContext) {
        if (!canFire(sheet, sceneContext)) {
            throw new IllegalOperationException(NO_PREPARED_ACTION);
        }
        PreparedAction prepared = sheet.getPreparedAction().orElseThrow();
        sheet.firePreparedAction();
        return prepared;
    }
}
