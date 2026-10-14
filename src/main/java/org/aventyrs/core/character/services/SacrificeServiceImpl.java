package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.action.ActionKind;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.Optional;

import static org.aventyrs.core.util.TranslatableMessages.NO_REACTION_AVAILABLE;
import static org.aventyrs.core.util.TranslatableMessages.SACRIFICE_ALLY_NOT_ADJACENT;
import static org.aventyrs.core.util.TranslatableMessages.SACRIFICE_ALREADY_USED;

public class SacrificeServiceImpl implements SacrificeService {

    private final ReactionsService reactionsService;

    public SacrificeServiceImpl() {
        this(new ReactionsServiceImpl());
    }

    public SacrificeServiceImpl(final ReactionsService reactionsService) {
        this.reactionsService = reactionsService;
    }

    @Override
    public Optional<String> refusalToSacrifice(@NonNull final CombatantSheet interceptor,
                                               @NonNull final CombatantSheet ally,
                                               final SceneContext interceptorContext, final int turnNumber) {
        if (interceptor.isAffectedThisCombat(SACRIFICE_USE)) {
            return Optional.of(SACRIFICE_ALREADY_USED);
        }
        if (interceptorContext == null || interceptorContext.getDistanceTo(ally) != Range.ADJACENTE) {
            return Optional.of(SACRIFICE_ALLY_NOT_ADJACENT);
        }
        if (reactionsService.getRemainingReactions(interceptor, turnNumber, interceptorContext) < 1
                || interceptor.isActionPrevented(ActionKind.REACTION, interceptorContext)) {
            return Optional.of(NO_REACTION_AVAILABLE);
        }
        return Optional.empty();
    }

    @Override
    public void sacrifice(@NonNull final CombatantSheet interceptor, @NonNull final CombatantSheet ally,
                          final SceneContext interceptorContext, final int turnNumber) {
        refusalToSacrifice(interceptor, ally, interceptorContext, turnNumber).ifPresent(refusal -> {
            throw new IllegalOperationException(refusal);
        });
        interceptor.spendReaction();
        interceptor.markAffectedThisCombat(SACRIFICE_USE);
    }
}
