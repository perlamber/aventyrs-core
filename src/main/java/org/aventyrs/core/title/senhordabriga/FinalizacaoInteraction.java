package org.aventyrs.core.title.senhordabriga;

import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

/**
 * Finalização's activation (1PD, 1PA) — "Nesta Rodada, ataques bem-sucedidos com Armas Naturais
 * recebem a Corrente de Efeitos – Finalização". Opens a one-Rodada activation window on the
 * activator, which {@link SenhorDaBriga#isFinalizacaoActive} reads.
 *
 * <p>What the window enables is applied by {@code AttackDelivery}/{@code AttackReceiver}: while it
 * is open, {@link SenhorDaBriga#resolveExtraNaturalCriticalEffectApplications} asks for one extra
 * application of the Arma Natural's own Efeito Crítico — at Menor on a hit that isn't critical, once
 * more on one that is.
 */
public class FinalizacaoInteraction extends AbstractTitleAbilityInteraction {

    /** "Nesta Rodada". */
    static final int WINDOW_ROUNDS = 1;

    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    public FinalizacaoInteraction() {
        this(new DeterminationPointsServiceImpl());
    }

    public FinalizacaoInteraction(final DeterminationPointsService determinationPointsService) {
        super(SenhorDaBrigaAbility.FINALIZACAO, determinationPointsService);
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        request.getActivator().openActivationWindow(SenhorDaBrigaAbility.FINALIZACAO, WINDOW_ROUNDS);
        return InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(request.getActivator()))
                .build();
    }
}
