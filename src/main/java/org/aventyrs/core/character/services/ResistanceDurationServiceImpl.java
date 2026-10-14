package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.Resistance;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ContinuousNature;

public class ResistanceDurationServiceImpl implements ResistanceDurationService {

    private final DamageService damageService;

    public ResistanceDurationServiceImpl() {
        this(new DamageServiceImpl());
    }

    public ResistanceDurationServiceImpl(final DamageService damageService) {
        this.damageService = damageService;
    }

    @Override
    public int getDurationReduction(@NonNull final CombatantSheet target, final ContinuousNature nature,
                                    final SceneContext sceneContext) {
        if (nature == null) {
            return 0;
        }
        DamageType type = nature.type();
        boolean primordial = type != null && type.isPrimordial();
        int reduction = 0;
        if (type == DamageType.FISICO && nature.damaging()
                && damageService.holdsResistance(target, Resistance.RD, nature.descriptor(), sceneContext)) {
            reduction += Resistance.DURATION_REDUCTION;
        }
        if (type == DamageType.MAGICO && !primordial
                && damageService.holdsResistance(target, Resistance.RM, nature.descriptor(), sceneContext)) {
            reduction += Resistance.DURATION_REDUCTION;
        }
        if (nature.descriptor() != null && nature.descriptor().elementalType() != null
                && damageService.holdsResistance(target, Resistance.RE, nature.descriptor(), sceneContext)) {
            reduction += Resistance.DURATION_REDUCTION;
        }
        if (damageService.holdsResistance(target, Resistance.RA, nature.descriptor(), sceneContext)) {
            reduction += Resistance.DURATION_REDUCTION;
        }
        return reduction;
    }
}
