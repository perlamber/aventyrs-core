package org.aventyrs.core.title.curandeiro;

import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.EgoPointType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_EGO_POINTS;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_CHOICE_REQUIRED;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_TARGET_REQUIRED;

/**
 * The shared shape of Transferir Determinação and Transferir Essência (1PD, 3PA, "ao toque"): the
 * request's {@link Mode} choice picks between moving a quantity of a resource pool (PD or PM) and
 * lending 1 temporary Ego point (Autocontrole or Sorte).
 *
 * <ul>
 *   <li>{@link Mode#POINTS}: the amount is the one {@code Integer} among the request's choices —
 *       "transferir qualquer quantidade … que você possua". The activator gives all of it; the ally
 *       recovers what it is missing, up to that amount, and whatever it cannot hold is lost. Giving
 *       rather than "spending what landed" is what lets a transfer to an ally whose real sheet lives in
 *       another client work: the giver cannot know what the ally is missing.</li>
 *   <li>{@link Mode#EGO_POINT}: the activator spends 1 temporary Ego point, and the ally receives it
 *       as a loan ({@code CombatantSheet#receiveEgoLoan}), settled at the end of the Cena — "perdidos
 *       ou devolvidos se não forem utilizados".</li>
 * </ul>
 */
public abstract class TransferenciaInteraction extends AbstractTitleAbilityInteraction {

    /** Which of the two transfers this activation makes. */
    public enum Mode { POINTS, EGO_POINT }

    private final HitPointsService hitPointsService = new HitPointsServiceImpl();
    private final EgoDomain egoDomain;
    private final ResourceType resourceType;

    protected TransferenciaInteraction(final AventyrTitleAbility ability,
                                       final DeterminationPointsService determinationPointsService,
                                       final EgoDomain egoDomain, final ResourceType resourceType) {
        super(ability, determinationPointsService);
        this.egoDomain = egoDomain;
        this.resourceType = resourceType;
    }

    /** What the activator holds of the pool right now — before this activation pays its cost. */
    protected abstract int currentPoints(CombatantSheet sheet);

    /** How much of the pool sheet is missing, and so can receive. */
    protected abstract int missingPoints(CombatantSheet sheet);

    protected abstract void spend(CombatantSheet sheet, int amount);

    protected abstract void recover(CombatantSheet sheet, int amount);

    /** The pool the activation's own cost comes out of, so the amount is judged after it (PD). */
    protected int activationCostFromPool(final TitleAbilityActivationRequest request) {
        return 0;
    }

    @Override
    protected void validate(final TitleAbilityActivationRequest request) {
        if (request.getTarget() == null || request.getTarget() == request.getActivator()) {
            throw new IllegalOperationException(TITLE_ABILITY_TARGET_REQUIRED);
        }
        Mode mode = request.getChoice(Mode.class)
                .orElseThrow(() -> new IllegalOperationException(TITLE_ABILITY_CHOICE_REQUIRED));
        if (mode == Mode.POINTS) {
            int amount = requestedAmount(request);
            if (amount <= 0 || amount > currentPoints(request.getActivator()) - activationCostFromPool(request)) {
                throw new IllegalOperationException(TITLE_ABILITY_CHOICE_REQUIRED);
            }
        } else if (request.getActivator().getTemporaryEgoPoints(egoDomain) < 1) {
            throw new IllegalOperationException(NOT_ENOUGH_EGO_POINTS);
        }
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet activator = request.getActivator();
        CombatantSheet target = request.getTarget();
        InteractionResult.InteractionResultBuilder result = InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(activator));
        if (request.getChoice(Mode.class).orElseThrow() == Mode.POINTS) {
            int given = requestedAmount(request);
            spend(activator, given);
            return result.resourceGainValue(receivePoints(target, given)).resourceGainType(resourceType).build();
        }
        activator.spendEgoPoints(egoDomain, EgoPointType.TEMPORARY, 1);
        target.receiveEgoLoan(egoDomain, activator);
        return result.egoLossValue(1).egoLossDomain(egoDomain).build();
    }

    /**
     * The ally's half of a {@link Mode#POINTS} transfer — recovers up to amount, returning what landed.
     * Public so the client holding the ally's real sheet can apply a transfer made elsewhere.
     */
    public int receivePoints(final CombatantSheet target, final int amount) {
        int landed = Math.min(amount, missingPoints(target));
        recover(target, landed);
        return landed;
    }

    /** The Ego domain this transfer lends in {@link Mode#EGO_POINT} — Autocontrole or Sorte. */
    public EgoDomain getEgoDomain() {
        return egoDomain;
    }

    private static int requestedAmount(final TitleAbilityActivationRequest request) {
        return request.getChoices(Integer.class).stream().findFirst().orElse(0);
    }
}
