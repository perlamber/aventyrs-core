package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.scene.ActiveAura;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_REQUIRES_SCENE;

/**
 * Orgulho Elduriano's activation — an {@link AbstractTitleAbilityInteraction} whose cost is
 * {@code PDCost.variable(1)}: the player names the PD on the request, the shared gates check and
 * spend them, and this class registers the provoking {@link ActiveAura} on the request's {@link
 * Scene}, lasting one Rodada per PD spent. Everything the Aura does afterwards (binding foes,
 * forcing their first attack, the Desvantagem on the rest) lives on {@link Scene} and the attack
 * orchestrators, not here. Activated via {@code AventyrTitle#activateAbility} (or {@link
 * Santo#activateOrgulhoElduriano}), which checks the Habilidade is held first.
 *
 * <p>When the request carries the holder's {@code sceneContext}, foes already within range are
 * bound straight away through {@link Scene#refreshAura}; without one, that is the caller's next
 * step. The registered Aura is read back from {@link Scene#getActiveAuras()}.
 */
public class OrgulhoEldurianoInteraction extends AbstractTitleAbilityInteraction {

    /** "Todos os inimigos em Distância Curta". */
    static final Range AURA_RADIUS = Range.DISTANCIA_CURTA;

    private final HitPointsService hitPointsService;

    public OrgulhoEldurianoInteraction() {
        this(new DeterminationPointsServiceImpl());
    }

    public OrgulhoEldurianoInteraction(final DeterminationPointsService determinationPointsService) {
        super(AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO, determinationPointsService);
        this.hitPointsService = new HitPointsServiceImpl();
    }

    /** The Aura is registered on the live Scene, so an activation without one is refused before paying. */
    @Override
    protected void validate(final TitleAbilityActivationRequest request) {
        if (request.getScene() == null) {
            throw new IllegalOperationException(TITLE_ABILITY_REQUIRES_SCENE);
        }
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet holder = request.getActivator();
        Scene scene = request.getScene();
        scene.addAura(new ActiveAura(holder, getAbility(), AURA_RADIUS, determinationPoints));
        if (request.getSceneContext() != null) {
            scene.refreshAura(holder, request.getSceneContext());
        }
        return InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(holder))
                .build();
    }
}
