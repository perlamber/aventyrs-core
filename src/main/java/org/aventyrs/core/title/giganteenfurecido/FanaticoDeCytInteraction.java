package org.aventyrs.core.title.giganteenfurecido;

import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Frenzy;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.LifeSteal;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.EgoCost;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import java.util.Optional;

import static org.aventyrs.core.util.TranslatableMessages.FRENZY_REQUIRED;

/**
 * Fanático de Cyt, the Reação taken as a hit is about to drop the holder to 0 PV or below: "gastar
 * temporariamente todos os seus pontos de Autocontrole atuais (mínimo 1), se o fizer você ficará com
 * 1PV". The caller activates it <em>before</em> applying that damage; the floor then catches it
 * ({@code CombatantSheet#floorNextDamageAt}). The Roubo de Vida lasts the rest of the Frenesi; the
 * fanatic mark on the Frenzy is what the "não poderá morrer", "vê todos os outros personagens como
 * inimigos" and hostile-Magia clauses read, each while the Autocontrole stays at zero.
 */
public class FanaticoDeCytInteraction extends AbstractTitleAbilityInteraction {

    /** "você ficará com 1PV". */
    static final int REMAINING_HIT_POINTS = 1;

    private int autocontrolePaid;

    public FanaticoDeCytInteraction() {
        super(BerserkerAbility.FANATICO_DE_CYT);
    }

    @Override
    protected void validate(final TitleAbilityActivationRequest request) {
        if (request.getActivator().getOwnFrenzy().isEmpty()) {
            throw new IllegalOperationException(FRENZY_REQUIRED);
        }
    }

    /** "todos os seus pontos de Autocontrole atuais (mínimo 1)". */
    @Override
    protected EgoCost resolveEgoCost(final TitleAbilityActivationRequest request) {
        return EgoCost.autocontrole(Math.max(1, request.getActivator().getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE)));
    }

    @Override
    protected void onEgoSpent(final TitleAbilityActivationRequest request, final EgoCost paid) {
        autocontrolePaid = paid.points();
        GiganteEnfurecido.requireHeldBy(request.getActivator()).recordFrenzySpend(request.getActivator(), paid.points());
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet activator = request.getActivator();
        Frenzy frenzy = activator.getOwnFrenzy().orElseThrow();
        frenzy.markFanatic();
        activator.floorNextDamageAt(REMAINING_HIT_POINTS);
        activator.applyEffect(new LifeSteal(autocontrolePaid, Optional.of(frenzy.getRemainingRounds())));
        return InteractionResult.builder().build();
    }
}
