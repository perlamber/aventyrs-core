package org.aventyrs.core.title.giganteenfurecido;

import org.aventyrs.core.sheet.FrenzyMode;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.EgoCost;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import java.util.Set;

import static org.aventyrs.core.util.TranslatableMessages.FRENZY_ALREADY_ACTIVE;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_PREREQUISITE_NOT_MET;

/**
 * The Despertar: "Você pode entrar em um estado de Frenesi profundo". The request names the
 * Especializações activated "em conjunto" with it as {@link FrenzyMode} choices; each must be held, and
 * each adds its "+1 Ponto temporário de Autocontrole" to the Ego cost (its "+1PA" is the caller's to
 * add to the reported Tempo, see {@link GiganteEnfurecidoSpecialization#getExtraActionPoints()}).
 * Frenesi Esmeralda and Cataclismo Elemental are not modes a Frenesi starts with — each is activated
 * while it runs.
 */
public class FrenesiInteraction extends AbstractTitleAbilityInteraction {

    private int autocontrolePaid;

    public FrenesiInteraction() {
        this(GiganteEnfurecidoDespertar.FRENESI);
    }

    protected FrenesiInteraction(final AventyrTitleAbility ability) {
        super(ability);
    }

    static Set<FrenzyMode> requestedModes(final TitleAbilityActivationRequest request) {
        return request.getChoices(FrenzyMode.class);
    }

    @Override
    protected void validate(final TitleAbilityActivationRequest request) {
        GiganteEnfurecido title = GiganteEnfurecido.requireHeldBy(request.getActivator());
        if (request.getActivator().getOwnFrenzy().isPresent()) {
            throw new IllegalOperationException(FRENZY_ALREADY_ACTIVE);
        }
        for (FrenzyMode mode : requestedModes(request)) {
            boolean held = GiganteEnfurecidoSpecialization.of(mode).map(title::holds).orElse(false);
            if (!held) {
                throw new IllegalOperationException(TITLE_ABILITY_PREREQUISITE_NOT_MET);
            }
        }
    }

    @Override
    protected EgoCost resolveEgoCost(final TitleAbilityActivationRequest request) {
        return getAbility().getEgoCost().plus(requestedModes(request).size());
    }

    @Override
    protected void onEgoSpent(final TitleAbilityActivationRequest request, final EgoCost paid) {
        autocontrolePaid = paid.points();
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        GiganteEnfurecido.requireHeldBy(request.getActivator())
                .startFrenzy(request.getActivator(), requestedModes(request), autocontrolePaid);
        return InteractionResult.builder().build();
    }
}
