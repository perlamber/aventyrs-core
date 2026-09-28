package org.aventyrs.core.title.curandeiro;

import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ResourceType;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

/** Transferir Determinação — PD, or 1 temporary Autocontrole. See {@link TransferenciaInteraction}. */
public class TransferirDeterminacaoInteraction extends TransferenciaInteraction {

    private final DeterminationPointsService determinationPointsService;

    public TransferirDeterminacaoInteraction() {
        this(new DeterminationPointsServiceImpl());
    }

    public TransferirDeterminacaoInteraction(final DeterminationPointsService determinationPointsService) {
        super(MartirAltruistaAbility.TRANSFERIR_DETERMINACAO, determinationPointsService, EgoDomain.AUTOCONTROLE,
                ResourceType.DETERMINATION_POINTS);
        this.determinationPointsService = determinationPointsService;
    }

    @Override
    protected int currentPoints(final CombatantSheet sheet) {
        return determinationPointsService.getCurrentDeterminationPoints(sheet.getCharacter(), sheet);
    }

    @Override
    protected int missingPoints(final CombatantSheet sheet) {
        return sheet.getDeterminationSpent();
    }

    @Override
    protected void spend(final CombatantSheet sheet, final int amount) {
        sheet.spendDeterminationPoints(amount);
    }

    @Override
    protected void recover(final CombatantSheet sheet, final int amount) {
        sheet.recoverDeterminationPoints(amount);
    }

    /** The activation's own 1PD comes out of the same pool the transfer draws on. */
    @Override
    protected int activationCostFromPool(final TitleAbilityActivationRequest request) {
        return MartirAltruistaAbility.TRANSFERIR_DETERMINACAO.getPDCost().minimum();
    }
}
