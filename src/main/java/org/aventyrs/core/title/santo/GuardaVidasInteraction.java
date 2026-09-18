package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.Teleportation;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import static org.aventyrs.core.util.TranslatableMessages.TELEPORT_TARGET_OUT_OF_RANGE;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_REQUIRES_SCENE;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_REQUIRES_TARGET;

/**
 * Guarda-Vidas' activation — the Santo teleports in front of a threatened ally and takes the attack
 * in their place. The request's {@code target} is that ally; the shared gates spend the 2PD, and
 * this class reports the interception on {@link InteractionResult#getRedirectedAttackTarget()}.
 *
 * <p><b>The attack is never touched, because it does not exist yet.</b> This is a Reação taken
 * before the defender rolls, so the client activates this first and only <em>then</em> builds its
 * {@code IncomingAttack}/{@code DeliveredAttack} naming the Santo as the defender. {@code
 * AttackDelivery} and {@code AttackReceiver} resolve an ordinary attack and never learn a Reação
 * happened — which is also what makes "o ataque ainda deve superar as suas Defesas" true by
 * construction, since the attack is rolled against the Santo's own sheet from the start. Nothing
 * here needs to interject into a resolution already in flight, and nothing here does.
 *
 * <p>The teleport is likewise reported, not applied: {@code
 * SantoAbility#GUARDA_VIDAS.resolveTeleportation()} states the reach, this class refuses an ally
 * beyond it, and the caller updates the distances it feeds the next {@code Scene#buildContext} —
 * this core holds no positions. See {@link Teleportation}.
 *
 * <p>Re-checks reach here even though {@code ReactionOptionsService} already filtered on it: a
 * listed option is a snapshot, and the player commits later than they looked.
 */
public class GuardaVidasInteraction extends AbstractTitleAbilityInteraction {

    private final HitPointsService hitPointsService;

    public GuardaVidasInteraction() {
        this(new DeterminationPointsServiceImpl());
    }

    public GuardaVidasInteraction(final DeterminationPointsService determinationPointsService) {
        super(SantoAbility.GUARDA_VIDAS, determinationPointsService);
        this.hitPointsService = new HitPointsServiceImpl();
    }

    /**
     * Refuses an activation that names no threatened ally, names the activator themselves, or names
     * one further away than the teleport reaches. Runs before a single PD is spent, per the base
     * class's contract.
     */
    @Override
    protected void validate(final TitleAbilityActivationRequest request) {
        SceneContext sceneContext = request.getSceneContext();
        if (sceneContext == null) {
            throw new IllegalOperationException(TITLE_ABILITY_REQUIRES_SCENE);
        }
        CombatantSheet ally = request.getTarget();
        CombatantSheet activator = request.getActivator();
        // getEffectiveTarget() is deliberately not used: "um aliado" is somebody else, so an
        // omitted target must refuse rather than quietly default to the Santo saving themselves.
        if (ally == null || ally.getId().equals(activator.getId()) || !sceneContext.getAllies().contains(ally)) {
            throw new IllegalOperationException(TITLE_ABILITY_REQUIRES_TARGET);
        }
        Teleportation teleportation = getAbility().resolveTeleportation();
        if (!teleportation.reaches(sceneContext.getDistanceTo(ally))) {
            throw new IllegalOperationException(TELEPORT_TARGET_OUT_OF_RANGE);
        }
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet santo = request.getActivator();
        return InteractionResult.builder()
                .redirectedAttackTarget(santo)
                .resultStatus(hitPointsService.getStatus(santo))
                .build();
    }
}
