package org.aventyrs.core.title.curandeiro;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.HealingSource;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_TARGET_REQUIRED;

/**
 * Encanto Regenerativo's activation (2PD, +1PA) — "adicionalmente recuperam Vigor PV do alvo": the
 * ally the request names heals its own Vigor in PV, sourced as this Habilidade (so the Coma cap and
 * Médico de Guerra's +2 both apply).
 *
 * <p>"Esta Habilidade deve ser utilizada em conjunto com a Conjuração ou Ativação do Efeito" — the
 * caller activates it with the Magia or Efeito de Encantamento it rides on, naming that effect's ally
 * target. An ally other than the activator is required ("sobre um personagem aliado").
 */
public class EncantoRegenerativoInteraction extends AbstractTitleAbilityInteraction {

    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    public EncantoRegenerativoInteraction() {
        this(new DeterminationPointsServiceImpl());
    }

    public EncantoRegenerativoInteraction(final DeterminationPointsService determinationPointsService) {
        super(MedicoDeGuerraAbility.ENCANTO_REGENERATIVO, determinationPointsService);
    }

    /** The heal this Habilidade is, by healer — relayed ({@code HealingSource#relay}) for a target elsewhere. */
    public static HealingSource source(final CombatantSheet healer) {
        return HealingSource.titleAbility(MedicoDeGuerraAbility.ENCANTO_REGENERATIVO, healer);
    }

    /**
     * The target's half — "recuperam Vigor PV do alvo", the <b>target's</b> Vigor. Public so the client
     * holding the target's real sheet can apply an activation made elsewhere. Returns the PV recovered.
     */
    public static int healTarget(final CombatantSheet target, final HealingSource source) {
        int damageBefore = target.getDamageTaken();
        target.heal(target.getCharacter().getEffectiveAttributeTotal(AttributeDomain.VIGOR), source);
        return damageBefore - target.getDamageTaken();
    }

    @Override
    protected void validate(final TitleAbilityActivationRequest request) {
        if (request.getTarget() == null || request.getTarget() == request.getActivator()) {
            throw new IllegalOperationException(TITLE_ABILITY_TARGET_REQUIRED);
        }
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet target = request.getTarget();
        int recovered = healTarget(target, source(request.getActivator()));
        return InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(target))
                .resourceGainValue(recovered)
                .resourceGainType(ResourceType.HIT_POINTS)
                .build();
    }
}
