package org.aventyrs.core.title.giganteenfurecido;

import org.aventyrs.core.magic.SpellEmpowerment;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.FrenzyMode;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import static org.aventyrs.core.util.TranslatableMessages.TITA_ENLOUQUECIDO_REQUIRED;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_ACTIVATION_LIMIT_REACHED;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_CHOICE_REQUIRED;

/**
 * Frenesi Arcano: the chosen {@link SpellEmpowerment} as a one-cast budget, spent — with its "+3PA" —
 * by the next Magia {@code SpellCastingServiceImpl#castSpell} resolves for the holder.
 */
public class FrenesiArcanoInteraction extends AbstractTitleAbilityInteraction {

    public FrenesiArcanoInteraction() {
        super(TitaEnlouquecidoAbility.FRENESI_ARCANO);
    }

    @Override
    protected void validate(final TitleAbilityActivationRequest request) {
        CombatantSheet activator = request.getActivator();
        // "apenas enquanto Titã Enlouquecido estiver Ativo".
        boolean titaActive = activator.getOwnFrenzy()
                .map(frenzy -> frenzy.hasMode(FrenzyMode.TITA_ENLOUQUECIDO)).orElse(false);
        if (!titaActive) {
            throw new IllegalOperationException(TITA_ENLOUQUECIDO_REQUIRED);
        }
        if (request.getChoice(SpellEmpowerment.class).isEmpty()) {
            throw new IllegalOperationException(TITLE_ABILITY_CHOICE_REQUIRED);
        }
        // "pode ser ativado apenas uma vez a cada Rodada".
        if (activator.isAffectedThisCombat(roundMark(request))) {
            throw new IllegalOperationException(TITLE_ABILITY_ACTIVATION_LIMIT_REACHED);
        }
    }

    private static GiganteEnfurecido.OncePerRound roundMark(final TitleAbilityActivationRequest request) {
        return new GiganteEnfurecido.OncePerRound(TitaEnlouquecidoAbility.FRENESI_ARCANO,
                GiganteEnfurecido.currentRound(request.getSceneContext()), null);
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet activator = request.getActivator();
        activator.markAffectedThisCombat(roundMark(request));
        activator.grantEnhancedAttacks(request.getChoice(SpellEmpowerment.class).orElseThrow(), 1);
        return InteractionResult.builder().build();
    }
}
