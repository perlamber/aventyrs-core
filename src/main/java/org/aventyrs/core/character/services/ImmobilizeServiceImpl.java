package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Immobilization;

import java.util.Optional;

import static org.aventyrs.core.util.TranslatableMessages.IMMOBILIZE_TARGET_NOT_HELPLESS;
import static org.aventyrs.core.util.TranslatableMessages.TARGET_NOT_ADJACENT;

public class ImmobilizeServiceImpl implements ImmobilizeService {

    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    @Override
    public Optional<String> refusal(@NonNull final CombatantSheet captor, @NonNull final CombatantSheet target,
                                    final SceneContext captorContext) {
        if (captorContext == null || captorContext.getDistanceTo(target) != Range.ADJACENTE) {
            return Optional.of(TARGET_NOT_ADJACENT);
        }
        CharacterStatus status = hitPointsService.getStatus(target);
        boolean unconscious = status == CharacterStatus.FALLEN || status == CharacterStatus.COMMA;
        if (!unconscious && !target.hasCondition(ConditionType.RENDIDO, null)) {
            return Optional.of(IMMOBILIZE_TARGET_NOT_HELPLESS);
        }
        return Optional.empty();
    }

    @Override
    public void immobilize(@NonNull final CombatantSheet captor, @NonNull final CombatantSheet target,
                           final SceneContext captorContext) {
        refusal(captor, target, captorContext).ifPresent(refusal -> {
            throw new IllegalOperationException(refusal);
        });
        target.applyCondition(Immobilization.byCaptor(captor, null));
    }
}
