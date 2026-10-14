package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.action.ActionKind;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.DefensiveStance;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.Optional;

import static org.aventyrs.core.util.TranslatableMessages.ACTION_PREVENTED_BY_CONDITION;

public class DefensiveStanceServiceImpl implements DefensiveStanceService {

    @Override
    public Optional<String> refusal(@NonNull final CombatantSheet sheet, final SceneContext sceneContext) {
        ActionKind priced = ActionKind.ofCost(STANCE_COST);
        return priced != null && sheet.isActionPrevented(priced, sceneContext)
                ? Optional.of(ACTION_PREVENTED_BY_CONDITION) : Optional.empty();
    }

    @Override
    public DefensiveStance take(@NonNull final CombatantSheet sheet, final SceneContext sceneContext) {
        refusal(sheet, sceneContext).ifPresent(refusal -> {
            throw new IllegalOperationException(refusal);
        });
        DefensiveStance stance = new DefensiveStance();
        sheet.applyEffect(stance);
        return stance;
    }
}
