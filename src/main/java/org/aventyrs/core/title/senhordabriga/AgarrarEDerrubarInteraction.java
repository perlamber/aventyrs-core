package org.aventyrs.core.title.senhordabriga;

import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

/**
 * Agarrar e Derrubar's activation (2PD, 3PA) — "Como parte da Ativação desta Habilidade você deve
 * fazer um ataque com uma de suas Armas Naturais".
 *
 * <p>The attack comes <b>after</b> the activation, as every empowered attack in this core does, so
 * the activation grants a one-attack budget and the attack reads it: Vantagem through {@link
 * SenhorDaBriga#resolveAttackRollBonus}, and the {@code effect.AgarrarEDerrubar} Corrente through
 * {@link SenhorDaBriga#resolveAttackModifiers}, which the caller puts on the {@code
 * DeliveredAttack}. A budget rather than an {@code EmpoweredAttack} because the Vantagem then lands
 * inside the roll itself instead of being folded by hand. The caller spends it with {@code
 * TitleAttackModifiers#consumeCharges} once the attack resolves; only an Arma Natural attack does.
 */
public class AgarrarEDerrubarInteraction extends AbstractTitleAbilityInteraction {

    /** "você deve fazer um ataque" — exactly one. */
    static final int ATTACKS = 1;

    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    public AgarrarEDerrubarInteraction() {
        this(new DeterminationPointsServiceImpl());
    }

    public AgarrarEDerrubarInteraction(final DeterminationPointsService determinationPointsService) {
        super(PunhoInigualavelAbility.AGARRAR_E_DERRUBAR, determinationPointsService);
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        request.getActivator().grantEnhancedAttacks(PunhoInigualavelAbility.AGARRAR_E_DERRUBAR, ATTACKS);
        return InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(request.getActivator()))
                .build();
    }
}
