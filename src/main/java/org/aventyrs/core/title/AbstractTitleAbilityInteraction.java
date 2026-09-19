package org.aventyrs.core.title;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;

import static org.aventyrs.core.util.TranslatableMessages.ABILITY_ACTIVATION_PREVENTED;
import static org.aventyrs.core.util.TranslatableMessages.INVALID_PD_AMOUNT;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_DETERMINATION_POINTS;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_HIT_POINTS;

/**
 * The shared activation of a Título ability — what every activated Habilidade, Suprema or
 * Especialização does before its own effect, written once. A concrete ability extends this,
 * implements {@link #resolve} (and {@link #validate} if it needs more from the request), and names
 * its class in its catalog constant's {@code interactionClass}, which is how {@link
 * AventyrTitle#activateAbility} finds it. It needs a no-argument constructor for that.
 *
 * <p>{@link #activate} runs the gates in the order {@code ActiveAbilityServiceImpl#activate} does,
 * <b>all before anything is spent</b>, so a refused activation costs nothing: Silêncio, the PD
 * amount against the ability's {@link PDCost}, PD affordability, the PV cost a trait may charge
 * instead ({@link #resolveHitPointCost} — refused when paying it would be self-fatal), then the
 * ability's own {@link #validate}. Only then are the costs spent, the activation counted on the
 * activator's sheet ({@code CombatantSheet#recordAbilityActivation}, so a trait's own {@code
 * countActivationsThisTurn} includes the activation in progress) and the effect resolved. Whether the ability is actually
 * held is {@link AventyrTitle#activateAbility}'s check, not this class's — the Título is what
 * knows that.
 *
 * <p>The Tempo de Ativação (PA / Reação / Ação Livre) stays reported by the ability and is never
 * deducted, like everywhere else in this core: nothing keeps a spent-this-Turn PA pool. What the
 * effect grants is the subclass's business — some report {@code Blessing}s for the caller to grant
 * ({@code GritoDeGuerraVulcanoInteraction}), some act directly (a heal, an Aura on the Scene).
 */
public abstract class AbstractTitleAbilityInteraction implements Interaction<CombatantSheet> {

    @Getter
    private final AventyrTitleAbility ability;
    private final DeterminationPointsService determinationPointsService;
    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    protected AbstractTitleAbilityInteraction(@NonNull final AventyrTitleAbility ability,
                                              @NonNull final DeterminationPointsService determinationPointsService) {
        this.ability = ability;
        this.determinationPointsService = determinationPointsService;
    }

    protected AbstractTitleAbilityInteraction(final AventyrTitleAbility ability) {
        this(ability, new DeterminationPointsServiceImpl());
    }

    /** Activates with activator as the only fact supplied — a self-cast at the stated cost. */
    @Override
    public InteractionResult applyTo(final CombatantSheet activator) {
        return activate(TitleAbilityActivationRequest.builder().activator(activator).build());
    }

    /**
     * Gates, pays and resolves one activation; see this class's javadoc for the order.
     *
     * @return the effect's result, with {@link InteractionResult#getDeterminationPointsSpent()} set
     * @throws IllegalOperationException {@code ABILITY_ACTIVATION_PREVENTED} under Silêncio,
     *         {@code INVALID_PD_AMOUNT} for an amount the cost refuses (or a Variável cost with none),
     *         {@code NOT_ENOUGH_DETERMINATION_POINTS} if the activator can't pay, or whatever
     *         {@link #validate} throws
     */
    public final InteractionResult activate(@NonNull final TitleAbilityActivationRequest request) {
        CombatantSheet activator = request.getActivator();
        if (activator.isAbilityActivationPrevented(request.getSceneContext())) {
            throw new IllegalOperationException(ABILITY_ACTIVATION_PREVENTED);
        }
        int determinationPoints = resolveDeterminationPoints(request);
        if (determinationPointsService.getCurrentDeterminationPoints(activator.getCharacter(), activator)
                < determinationPoints) {
            throw new IllegalOperationException(NOT_ENOUGH_DETERMINATION_POINTS);
        }
        // A PV cost can never be paid down to 0 PV — the same refusal ActiveAbilityService applies
        // to a Poder Vampírico's "consomem 3PV cada".
        int hitPoints = resolveHitPointCost(request);
        if (hitPoints > 0
                && hitPointsService.getCurrentHitPoints(activator.getCharacter(), activator) <= hitPoints) {
            throw new IllegalOperationException(NOT_ENOUGH_HIT_POINTS);
        }
        validate(request);

        if (determinationPoints > 0) {
            activator.spendDeterminationPoints(determinationPoints);
        }
        if (hitPoints > 0) {
            // The bare sheet mutator, not DamageService: a self-inflicted activation cost is not an
            // attack, so no RD/RA/Meio-Dano stage applies to it.
            activator.applyDamage(hitPoints);
        }
        activator.recordAbilityActivation(ability);

        InteractionResult.InteractionResultBuilder result = resolve(request, determinationPoints).toBuilder()
                .determinationPointsSpent(determinationPoints);
        if (hitPoints > 0) {
            result.resourceLossValue(hitPoints).resourceLossType(ResourceType.HIT_POINTS);
        }
        return result.build();
    }

    /**
     * Refuses a request this ability can't act on — a missing Scene, target or choice, or an
     * activation limit already reached. Runs after the shared gates and before anything is spent.
     * Does nothing by default.
     */
    protected void validate(final TitleAbilityActivationRequest request) {
    }

    /**
     * PV this activation costs its activator — Santo's Abraçado pela Escuridão traits pay in PV
     * rather than PD ("gastar uma quantidade de pontos de vida igual ao seu Vigor"). Zero by
     * default. Paid as plain damage, and refused above when paying it would drop the activator to
     * 0 PV or below.
     */
    protected int resolveHitPointCost(final TitleAbilityActivationRequest request) {
        return 0;
    }

    /**
     * The ability's own effect, once the activation has been paid for.
     *
     * @param determinationPoints the PD actually spent — what a Variável ability scales by
     */
    protected abstract InteractionResult resolve(TitleAbilityActivationRequest request, int determinationPoints);

    private int resolveDeterminationPoints(final TitleAbilityActivationRequest request) {
        PDCost cost = ability.getPDCost();
        Integer requested = request.getDeterminationPoints();
        if (requested == null) {
            if (cost.isVariable()) {
                throw new IllegalOperationException(INVALID_PD_AMOUNT);
            }
            return cost.minimum();
        }
        if (!cost.accepts(requested)) {
            throw new IllegalOperationException(INVALID_PD_AMOUNT);
        }
        return requested;
    }
}
