package org.aventyrs.core.title.giganteenfurecido;

import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import static org.aventyrs.core.util.TranslatableMessages.FRENZY_REQUIRED;

/** Prolongar Descontrole: "Você pode aumentar a duração de seu Frenesi em +2 Rodadas". */
public class ProlongarDescontroleInteraction extends AbstractTitleAbilityInteraction {

    /** "+2 Rodadas". */
    static final int EXTRA_ROUNDS = 2;

    public ProlongarDescontroleInteraction() {
        super(GiganteEnfurecidoAbility.PROLONGAR_DESCONTROLE);
    }

    /** "Esta Habilidade só pode ser ativada durante o efeito de Frenesi do Gigante Enfurecido." */
    @Override
    protected void validate(final TitleAbilityActivationRequest request) {
        if (request.getActivator().getOwnFrenzy().isEmpty()) {
            throw new IllegalOperationException(FRENZY_REQUIRED);
        }
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        request.getActivator().getOwnFrenzy().orElseThrow().extend(EXTRA_ROUNDS);
        return InteractionResult.builder().build();
    }
}
