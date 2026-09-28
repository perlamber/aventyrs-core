package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.feat.CavalariaFeat;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Riding;

import static org.aventyrs.core.util.TranslatableMessages.ALREADY_RIDING;
import static org.aventyrs.core.util.TranslatableMessages.MOUNT_REACTION_NOT_PERMITTED;
import static org.aventyrs.core.util.TranslatableMessages.NOT_RIDING;

public class MountServiceImpl implements MountService {

    private final ActionPointsService actionPointsService;

    public MountServiceImpl() {
        this(new ActionPointsServiceImpl());
    }

    public MountServiceImpl(@NonNull final ActionPointsService actionPointsService) {
        this.actionPointsService = actionPointsService;
    }

    @Override
    public ActionCost mount(@NonNull final CombatantSheet rider, @NonNull final Riding riding) {
        if (rider.isRiding()) {
            throw new IllegalOperationException(ALREADY_RIDING);
        }
        ActionCost cost = useInstant(rider) ? ActionCost.FREE_ACTION : DEFAULT_MOUNT_COST;
        rider.startRiding(riding);
        return cost;
    }

    @Override
    public ActionCost dismount(@NonNull final CombatantSheet rider, final boolean asReaction) {
        if (!rider.isRiding()) {
            throw new IllegalOperationException(NOT_RIDING);
        }
        ActionCost cost;
        if (asReaction) {
            if (!useInstant(rider)) {
                throw new IllegalOperationException(MOUNT_REACTION_NOT_PERMITTED);
            }
            rider.spendReaction();
            cost = ActionCost.REACTION;
        } else {
            cost = useInstant(rider) ? ActionCost.FREE_ACTION : DEFAULT_MOUNT_COST;
        }
        rider.stopRiding();
        return cost;
    }

    @Override
    public int getMountMovementActionPointCost(@NonNull final CombatantSheet rider, final int turnNumber) {
        CombatantSheet steed = steedOf(rider);
        if (steed == null) {
            return 0;
        }
        int steedActionPoints = actionPointsService.getMaxActionPoints(steed, turnNumber);
        return holds(rider, CavalariaFeat.MONTARIA_DE_COMBATE)
                ? Math.min(COMBAT_MOUNT_MOVEMENT_COST, steedActionPoints) : steedActionPoints;
    }

    @Override
    public int getMountActionAllowance(@NonNull final CombatantSheet rider, final int turnNumber) {
        CombatantSheet steed = steedOf(rider);
        if (steed == null || !holds(rider, CavalariaFeat.MONTARIA_DE_COMBATE)) {
            return 0;
        }
        int remaining = actionPointsService.getMaxActionPoints(steed, turnNumber) - COMBAT_MOUNT_MOVEMENT_COST;
        return Math.max(0, Math.min(rider.getCharacter().getAllTitles().size(), remaining));
    }

    @Override
    public boolean mayInterruptMountMovement(@NonNull final CombatantSheet rider) {
        return rider.isRiding() && holds(rider, CavalariaFeat.DIRECAO_CAOTICA);
    }

    /**
     * Whether Montar e Desmontar Instantâneo applies now — held, and "apenas uma vez a cada Rodada":
     * ⚠️ counted per Turn (CombatantSheet#countActivationsThisTurn). Recorded when it does.
     */
    private static boolean useInstant(final CombatantSheet rider) {
        if (!holds(rider, CavalariaFeat.MONTAR_E_DESMONTAR_INSTANTANEO)
                || rider.countActivationsThisTurn(CavalariaFeat.MONTAR_E_DESMONTAR_INSTANTANEO) > 0) {
            return false;
        }
        rider.recordAbilityActivation(CavalariaFeat.MONTAR_E_DESMONTAR_INSTANTANEO);
        return true;
    }

    private static CombatantSheet steedOf(final CombatantSheet rider) {
        return rider.getRiding().map(Riding::steed).orElse(null);
    }

    private static boolean holds(final CombatantSheet rider, final CavalariaFeat talento) {
        return rider.getCharacter() != null
                && rider.getCharacter().getFeats().stream().anyMatch(feat -> feat.catalogEntry() == talento);
    }
}
