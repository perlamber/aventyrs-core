package org.aventyrs.core.title.senhordabriga;

import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_REQUIRES_TARGET;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_TARGET_OUT_OF_RANGE;

/**
 * Rolamento Ofensivo's activation (1PD, Ação Livre) — "Você pode rolar 2UD em direção a um inimigo
 * em Distância Curta."
 *
 * <p>Requires the request's {@code target} to be an enemy of the activator within {@link
 * #ENEMY_RANGE}, read off the activator's own {@code sceneContext}; the roll itself is
 * <b>reported</b> as {@link InteractionResult#getMovementTowardTarget()} — this core holds no
 * positions, so the caller moves the token and rebuilds its distances, the same contract a {@code
 * Teleportation} carries. Under Grande Mestre das Brigas the roll carries {@link
 * #GRANDE_MESTRE_DISTANCE} and opens a one-Rodada window granting Vantagem on Arma Natural attacks
 * ({@link SenhorDaBriga#resolveAttackRollBonus}).
 *
 * <p>TODO the "+3 em suas Defesas para resistir aos ataques do inimigo que você se aproximou" — see
 * {@link PunhoInigualavelAbility#ROLAMENTO_OFENSIVO}.
 */
public class RolamentoOfensivoInteraction extends AbstractTitleAbilityInteraction {

    /** "em direção a um inimigo em Distância Curta". */
    public static final Range ENEMY_RANGE = Range.DISTANCIA_CURTA;

    /** "Você pode rolar 2UD". */
    public static final int DISTANCE = 2;

    /** Grande Mestre das Brigas: "A distância de seu Rolamento Ofensivo muda para 3UD". */
    public static final int GRANDE_MESTRE_DISTANCE = 3;

    /** Grande Mestre das Brigas: "Vantagem … por 1 Rodada". */
    static final int GRANDE_MESTRE_VANTAGEM_ROUNDS = 1;

    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    public RolamentoOfensivoInteraction() {
        this(new DeterminationPointsServiceImpl());
    }

    public RolamentoOfensivoInteraction(final DeterminationPointsService determinationPointsService) {
        super(PunhoInigualavelAbility.ROLAMENTO_OFENSIVO, determinationPointsService);
    }

    @Override
    protected void validate(final TitleAbilityActivationRequest request) {
        CombatantSheet target = request.getTarget();
        SceneContext context = request.getSceneContext();
        if (target == null || target.getId().equals(request.getActivator().getId())) {
            throw new IllegalOperationException(TITLE_ABILITY_REQUIRES_TARGET);
        }
        Range distance = context == null ? null : context.getDistanceTo(target);
        if (context == null || !context.getEnemies().contains(target) || distance == null
                || !distance.isWithin(ENEMY_RANGE)) {
            throw new IllegalOperationException(TITLE_ABILITY_TARGET_OUT_OF_RANGE);
        }
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet activator = request.getActivator();
        boolean grandeMestre = SenhorDaBriga.requireHeldBy(activator).holdsGrandeMestreDasBrigas();
        if (grandeMestre) {
            activator.openActivationWindow(PunhoInigualavelAbility.ROLAMENTO_OFENSIVO, GRANDE_MESTRE_VANTAGEM_ROUNDS);
        }
        return InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(activator))
                .movementTowardTarget(grandeMestre ? GRANDE_MESTRE_DISTANCE : DISTANCE)
                .build();
    }
}
