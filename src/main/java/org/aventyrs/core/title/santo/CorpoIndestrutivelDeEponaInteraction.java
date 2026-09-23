package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.PeleDePedra;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

/**
 * Corpo Indestrutível de Epona's activation (4PD, 1PA) — "Sua pele é transformada em pedra por 1
 * Rodada. O primeiro ataque que lhe causaria Danos é reduzido à zero, após isso você recebe RDS 5.
 * A Redução de Danos Sofridos é reduzida em -2 para cada dano sofrido."
 *
 * <p>Applied rather than reported, since the recipient is the activator and nobody else: it
 * registers a {@link PeleDePedra} on the holder's own sheet, which {@code DamageServiceImpl}
 * consults as it computes each incoming hit.
 *
 * <p><b>Two things the ordinary machinery could not carry</b>, which is why that effect exists
 * rather than a {@code Blessing}: the first hit is <em>negated</em>, not reduced, and no {@code
 * ModifierType} expresses "reduce to zero"; and what follows is a figure that changes after every
 * hit, where a {@code TemporaryBonus} holds one figure for its whole Duração. See {@link
 * PeleDePedra}.
 *
 * <p>The Duração and the stone are independent limits and both are real — the effect ends at
 * whichever runs out first, the Rodada count or the reduction wearing to nothing.
 *
 * <p>Re-activating replaces the existing stone rather than layering a second one
 * ({@code PeleDePedra#isCumulative()} is false): re-entering a state is not stacking it.
 */
public class CorpoIndestrutivelDeEponaInteraction extends AbstractTitleAbilityInteraction {

    /** "Sua pele é transformada em pedra por 1 Rodada." */
    static final int DURATION_IN_ROUNDS = 1;

    private final HitPointsService hitPointsService;

    public CorpoIndestrutivelDeEponaInteraction() {
        this(new DeterminationPointsServiceImpl());
    }

    public CorpoIndestrutivelDeEponaInteraction(final DeterminationPointsService determinationPointsService) {
        super(AbencoadoPelaLuzAbility.CORPO_INDESTRUTIVEL_DE_EPONA, determinationPointsService);
        this.hitPointsService = new HitPointsServiceImpl();
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet activator = request.getActivator();
        activator.applyEffect(new PeleDePedra(DURATION_IN_ROUNDS));
        return InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(activator))
                .build();
    }
}
