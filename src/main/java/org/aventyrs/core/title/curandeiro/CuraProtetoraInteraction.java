package org.aventyrs.core.title.curandeiro;

import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;


/**
 * Cura Protetora's activation (2PD, +1PA) — "concedem a você e ao alvo Bônus de +4 em suas Defesas
 * por 1 Rodada, efeito não cumulativo", granted to the activator and the request's target.
 *
 * <p><b>It rides on a heal the caller makes.</b> "+1PA" is a surcharge on a Magia (Broto ou superior)
 * or Habilidade de Curandeiro that recovers PV, so the caller activates this beside that heal, with
 * the heal's target — the same "em conjunto" shape Encanto Regenerativo has. That pairing, and the
 * Broto rung, are the caller's to honour. "Não cumulativo" is the Blessing's own source: a second
 * activation renews the bonus rather than stacking it.
 */
public class CuraProtetoraInteraction extends AbstractTitleAbilityInteraction {

    /** "+4 em suas Defesas". */
    static final int DEFESAS_BONUS = 4;

    /** "por 1 Rodada". */
    static final int ROUNDS = 1;

    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    public CuraProtetoraInteraction() {
        this(new DeterminationPointsServiceImpl());
    }

    public CuraProtetoraInteraction(final DeterminationPointsService determinationPointsService) {
        super(MedicoDeGuerraAbility.CURA_PROTETORA, determinationPointsService);
    }

    /**
     * Grants sheet Cura Protetora's +4 Defesas for 1 Rodada — the same sourced Blessing each time, so
     * it renews rather than stacks. Public so the client holding the target's real sheet can apply an
     * activation made elsewhere. Applied, not reported: nothing is left for a caller to grant.
     */
    public static void protect(final CombatantSheet sheet) {
        sheet.grantBlessing(new Blessing(ModifierType.DEFESAS, DEFESAS_BONUS, ROUNDS, TargetScope.SINGLE_TARGET,
                MedicoDeGuerraAbility.CURA_PROTETORA.name()));
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet activator = request.getActivator();
        CombatantSheet target = request.getEffectiveTarget();
        protect(activator);
        if (target != activator) {
            protect(target);
        }
        return InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(activator))
                .build();
    }
}
