package org.aventyrs.core.title.curandeiro;

import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

/**
 * Curar os Mortos' activation (2PD, +1PA) — banks one revival charge on the activator, which the
 * next qualifying heal on a dead target spends ({@link Curandeiro#claimRevival}).
 *
 * <p><b>One activation, one heal.</b> A charge is not a Rodada-long window: a Curandeiro reaching
 * two corpses pays twice, and one whose first heal left the target still dead (short of negative
 * max PV) activates again to finish it. Once the target is back — in Coma — nothing further is
 * needed from this Suprema; Levantar os Caídos takes over, the revival being a Coma begun in this
 * Cena. Unspent charges are dropped at {@code CombatantSheet#startNewScene()}.
 */
public class CurarOsMortosInteraction extends AbstractTitleAbilityInteraction {

    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    public CurarOsMortosInteraction() {
        this(new DeterminationPointsServiceImpl());
    }

    public CurarOsMortosInteraction(final DeterminationPointsService determinationPointsService) {
        super(CurandeiroAbility.CURAR_OS_MORTOS, determinationPointsService);
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        request.getActivator().grantCharge(CurandeiroAbility.CURAR_OS_MORTOS);
        return InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(request.getActivator()))
                .build();
    }
}
