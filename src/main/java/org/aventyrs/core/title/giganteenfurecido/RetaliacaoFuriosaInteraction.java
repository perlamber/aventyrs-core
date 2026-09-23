package org.aventyrs.core.title.giganteenfurecido;

import lombok.NonNull;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import java.util.UUID;

import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_REQUIRES_TARGET;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_TARGET_OUT_OF_RANGE;

/**
 * Retaliação Furiosa, the Reação: the request's target is the adjacent enemy who just attacked someone
 * else. It grants one attack against that enemy — the caller makes it next, like any attack — whose dano
 * {@link GiganteEnfurecido#resolveAttackModifiers} fixes whatever the weapon.
 */
public class RetaliacaoFuriosaInteraction extends AbstractTitleAbilityInteraction {

    /** Which enemy a pending retaliation is against, for the rest of the reactor's Turn. */
    record RetaliationTarget(UUID enemyId) {
    }

    public RetaliacaoFuriosaInteraction() {
        super(BerserkerAbility.RETALIACAO_FURIOSA);
    }

    /**
     * Whether reactor may retaliate against attacker now: an enemy, adjacent, and not already
     * retaliated against this Rodada — "apenas 1 vez por Rodada para cada inimigo".
     */
    static boolean isAvailableAgainst(final CombatantSheet reactor, final CombatantSheet attacker,
                                      final SceneContext context) {
        return attacker != null && context != null
                && context.getEnemies().contains(attacker)
                && context.getDistanceTo(attacker) == Range.ADJACENTE
                && !reactor.isAffectedThisCombat(roundMark(attacker, context));
    }

    private static GiganteEnfurecido.OncePerRound roundMark(final CombatantSheet attacker, final SceneContext context) {
        return new GiganteEnfurecido.OncePerRound(BerserkerAbility.RETALIACAO_FURIOSA,
                GiganteEnfurecido.currentRound(context), attacker.getId());
    }

    /** Whether holder's next attack against target is a retaliation — read by the Título's attack scan. */
    static boolean hasRetaliationAgainst(@NonNull final CombatantSheet holder, @NonNull final CombatantSheet target) {
        return holder.getRemainingEnhancedAttacks(BerserkerAbility.RETALIACAO_FURIOSA) > 0
                && holder.hasActivationWindow(new RetaliationTarget(target.getId()));
    }

    @Override
    protected void validate(final TitleAbilityActivationRequest request) {
        if (request.getTarget() == null) {
            throw new IllegalOperationException(TITLE_ABILITY_REQUIRES_TARGET);
        }
        if (!isAvailableAgainst(request.getActivator(), request.getTarget(), request.getSceneContext())) {
            throw new IllegalOperationException(TITLE_ABILITY_TARGET_OUT_OF_RANGE);
        }
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet reactor = request.getActivator();
        CombatantSheet enemy = request.getTarget();
        reactor.markAffectedThisCombat(roundMark(enemy, request.getSceneContext()));
        reactor.openActivationWindow(new RetaliationTarget(enemy.getId()), 1);
        reactor.grantEnhancedAttacks(BerserkerAbility.RETALIACAO_FURIOSA, 1);
        return InteractionResult.builder().build();
    }
}
