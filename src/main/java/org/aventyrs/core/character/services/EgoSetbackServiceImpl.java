package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.ego.EgoSetback;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_DIE_ROLL;
import static org.aventyrs.core.util.TranslatableMessages.NO_EGO_SETBACK_OWED;

public class EgoSetbackServiceImpl implements EgoSetbackService {

    @Override
    public EgoSetback rollSetback(@NonNull final CombatantSheet sheet, @NonNull final EgoDomain domain, final int face) {
        if (!sheet.getOwedEgoSetbacks().contains(domain)) {
            throw new IllegalOperationException(NO_EGO_SETBACK_OWED);
        }
        EgoSetback setback = EgoSetback.of(domain, face)
                .orElseThrow(() -> new IllegalOperationException(INVALID_DIE_ROLL));
        sheet.recordEgoSetback(setback);
        return setback;
    }
}
