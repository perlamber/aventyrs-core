package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.rest.RestService;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_CHOICE_REQUIRED;

/**
 * Abençoado pela Luz's own touch-heal-or-cure effect — an {@link AbstractTitleAbilityInteraction},
 * so the shared gates run and the activator's 1PD are paid first. It acts on the request's
 * {@link TitleAbilityActivationRequest#getEffectiveTarget() target} (the touched character — the
 * activator themself when none is named), and the player picks the branch through the request's
 * {@link Branch} choice. Activated via {@code AventyrTitle#activateAbility} (or {@link
 * Santo#activateAbencoadoPelaLuz}), which checks the Especialização is held first.
 *
 * <p>No Perícia roll is involved — no {@code skillRollBonus}, no dice: this is a direct effect.
 */
public class AbencoadoPelaLuzInteraction extends AbstractTitleAbilityInteraction {

    /** The two things Abençoado pela Luz can do with one touch. */
    public enum Branch {
        /** Heal the target as if they'd taken a Descanso Curto. */
        HEAL,
        /** Remove a Malefício — still inert, see {@link #resolve}. */
        REMOVE_MALEFICIO
    }

    private final RestService restService;
    private final HitPointsService hitPointsService;

    public AbencoadoPelaLuzInteraction() {
        this(new RestServiceImpl());
    }

    public AbencoadoPelaLuzInteraction(final RestService restService) {
        this(restService, new HitPointsServiceImpl());
    }

    public AbencoadoPelaLuzInteraction(final RestService restService, final HitPointsService hitPointsService) {
        this(restService, hitPointsService, new DeterminationPointsServiceImpl());
    }

    public AbencoadoPelaLuzInteraction(final RestService restService, final HitPointsService hitPointsService,
                                       final DeterminationPointsService determinationPointsService) {
        super(SantoSpecialization.ABENCOADO_PELA_LUZ, determinationPointsService);
        this.restService = restService;
        this.hitPointsService = hitPointsService;
    }

    /**
     * The branch is the player's call and there is no safe default between a heal and a cure, so
     * an activation without one is refused before its PD are spent.
     */
    @Override
    protected void validate(final TitleAbilityActivationRequest request) {
        if (request.getChoice(Branch.class).isEmpty()) {
            throw new IllegalOperationException(TITLE_ABILITY_CHOICE_REQUIRED);
        }
    }

    /**
     * {@link Branch#HEAL} heals the target as if they'd taken a Descanso Curto — real, via {@link
     * SantoSpecialization#resolveShortRestHealAmount} — reported as a {@link
     * InteractionResult#getResourceGainValue()}/{@link InteractionResult#getResourceGainType()}
     * pair. {@link Branch#REMOVE_MALEFICIO} is still TODO'd (see that same method's own javadoc for
     * the missing classification), so it applies nothing and reports an inert result — the PD are
     * still paid, since the activation itself did happen.
     */
    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet target = request.getEffectiveTarget();
        if (request.getChoice(Branch.class).orElseThrow() != Branch.HEAL) {
            // TODO: Malefício removal — see SantoSpecialization#resolveShortRestHealAmount's
            // own citation for the missing Malefício/Encantamento/Maldição/Doença
            // classification this branch needs before it can do anything real.
            return InteractionResult.builder()
                    .resultStatus(hitPointsService.getStatus(target))
                    .build();
        }
        Character character = target.getCharacter();
        int healed = SantoSpecialization.ABENCOADO_PELA_LUZ.resolveShortRestHealAmount(character, restService);
        target.heal(healed);
        return InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(target))
                .resourceGainValue(healed)
                .resourceGainType(ResourceType.HIT_POINTS)
                .build();
    }
}
