package org.aventyrs.core.title.curandeiro;

import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_ALREADY_USED_THIS_ROUND;

/**
 * Curandeiro Veloz's activation (2PD, Ação Livre) — banks one charge that takes -2PA off the
 * Curandeiro's next healing Habilidade de Curandeiro or healing Magia Natural/Divina ({@link
 * Curandeiro#resolveActivationActionPointReduction}, {@link Curandeiro#resolveCastingActionPointReduction}),
 * spent by whichever comes first. "Apenas 1 vez a cada Rodada" is a one-Rodada activation window
 * opened here and refused while it is still open.
 */
public class CurandeiroVelozInteraction extends AbstractTitleAbilityInteraction {

    /** The -2PA the next qualifying heal takes off. */
    static final int ACTION_POINT_REDUCTION = 2;

    /** The once-per-Rodada mark — distinct from the charge, which outlives the Rodada. */
    record RoundMark() {
    }

    static final RoundMark ROUND_MARK = new RoundMark();

    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    public CurandeiroVelozInteraction() {
        this(new DeterminationPointsServiceImpl());
    }

    public CurandeiroVelozInteraction(final DeterminationPointsService determinationPointsService) {
        super(CurandeiroAbility.CURANDEIRO_VELOZ, determinationPointsService);
    }

    @Override
    protected void validate(final TitleAbilityActivationRequest request) {
        if (request.getActivator().hasActivationWindow(ROUND_MARK)) {
            throw new IllegalOperationException(TITLE_ABILITY_ALREADY_USED_THIS_ROUND);
        }
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        request.getActivator().openActivationWindow(ROUND_MARK, 1);
        request.getActivator().grantCharge(CurandeiroAbility.CURANDEIRO_VELOZ);
        return InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(request.getActivator()))
                .build();
    }
}
