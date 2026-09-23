package org.aventyrs.core.title.senhordabriga;

import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_CHOICE_REQUIRED;

/**
 * Impacto Elemental's activation (1PD, 2PA). Grants the activator a <b>combat-scoped budget of
 * attacks</b> — "A Duração de Impacto Elemental é igual à 2+ o número de outras Habilidades e
 * Supremas de Punho Inigualável … é cancelada ao final da Cena mesmo se ainda houver quantidade de
 * ataques disponíveis" names attacks, not Rodadas, and an end that {@code Scene#endCombat} marks.
 *
 * <p>What each budgeted attack gains is not granted here but <i>scanned off the budget</i>, the way
 * Furor de Sylph's +1PA is: {@link SenhorDaBriga#resolveAttackRollBonus} (Vantagem) and {@link
 * SenhorDaBriga#resolveAttackModifiers} (the element, and Grande Mestre's DM and Cataclismo) read
 * {@code getRemainingEnhancedAttacks}, so the effect lasts exactly while charges remain. The caller
 * spends one per Arma Natural attack through {@code TitleAttackModifiers#consumeCharges}.
 *
 * <p>Refused ({@code TITLE_ABILITY_CHOICE_REQUIRED}) until the element has been chosen — the choice
 * is made once, on the Título, not per activation.
 */
public class ImpactoElementalInteraction extends AbstractTitleAbilityInteraction {

    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    public ImpactoElementalInteraction() {
        this(new DeterminationPointsServiceImpl());
    }

    public ImpactoElementalInteraction(final DeterminationPointsService determinationPointsService) {
        super(PunhoInigualavelAbility.IMPACTO_ELEMENTAL, determinationPointsService);
    }

    @Override
    protected void validate(final TitleAbilityActivationRequest request) {
        SenhorDaBriga title = SenhorDaBriga.requireHeldBy(request.getActivator());
        if (title.getImpactoElementalElement().isEmpty()) {
            throw new IllegalOperationException(TITLE_ABILITY_CHOICE_REQUIRED);
        }
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet activator = request.getActivator();
        activator.grantEnhancedAttacksForCombat(PunhoInigualavelAbility.IMPACTO_ELEMENTAL,
                SenhorDaBriga.requireHeldBy(activator).resolveImpactoElementalAttacks());
        return InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(activator))
                .build();
    }
}
