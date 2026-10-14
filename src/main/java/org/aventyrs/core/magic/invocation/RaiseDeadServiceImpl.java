package org.aventyrs.core.magic.invocation;

import lombok.NonNull;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import static org.aventyrs.core.util.TranslatableMessages.CORPSE_EXHAUSTED;
import static org.aventyrs.core.util.TranslatableMessages.RAISE_REQUIRES_CORPSE;

public class RaiseDeadServiceImpl implements RaiseDeadService {

    private final HitPointsService hitPointsService;

    public RaiseDeadServiceImpl() {
        this(new HitPointsServiceImpl());
    }

    public RaiseDeadServiceImpl(final HitPointsService hitPointsService) {
        this.hitPointsService = hitPointsService;
    }

    @Override
    public boolean isRaisable(@NonNull final Scene scene, @NonNull final CombatantSheet corpse) {
        return isBody(scene, corpse) && corpse.getTimesRaised() < MAX_RAISES;
    }

    @Override
    public void requireRaisable(@NonNull final Scene scene, @NonNull final CombatantSheet corpse) {
        if (!isBody(scene, corpse)) {
            throw new IllegalOperationException(RAISE_REQUIRES_CORPSE);
        }
        if (corpse.getTimesRaised() >= MAX_RAISES) {
            throw new IllegalOperationException(CORPSE_EXHAUSTED);
        }
    }

    private boolean isBody(final Scene scene, final CombatantSheet corpse) {
        // ⚠️ Reading: a body marked beyond revival (Execução Real, Explosão Pós-Mortis) is past raising too.
        if (corpse.isBeyondRevival() || scene.getSummon(corpse).isPresent()) {
            return false;
        }
        if (corpse.isCorpse() || hitPointsService.getStatus(corpse) == CharacterStatus.DEAD) {
            return true;
        }
        boolean undead = corpse.hasUndeadAnatomy() || corpse instanceof MonsterSheet monster && monster.isUndead();
        return undead && hitPointsService.getCurrentHitPoints(corpse.getCharacter(), corpse) <= 0;
    }
}
