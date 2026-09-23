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
 * {@code PDCost.variable(2)}: the player names the PD on the request, the shared gates check and
 * spend them, and this class registers the provoking {@link ActiveAura} on the request's {@link
 * Scene}. Everything the Aura does afterwards lives elsewhere, and deliberately so. Activated via
 * {@code AventyrTitle#activateAbility} (or {@link Santo#activateOrgulhoElduriano}), which checks
 * the Habilidade is held first.
 *
 * <p><b>"Esta Habilidade é um Efeito de Encantamento" is real, and it is what shapes the design.</b>
 * The Aura is only an emitter: as enemies enter its radius — through movement or a teleport, on
 * the next {@link Scene#refreshAura} — each is <em>cast</em> a {@code sheet.ForcedTargeting}, an
 * {@code sheet.Enchantment} that lives on their own sheet and compels them to spend their first
 * attack of each Rodada on the Santo.
 *
 * <p>This Interaction therefore builds the effect and nothing more; <b>what lands is resolved by
 * the recipient, never by the caster</b>. A Fada, Fúria or Górgona is immune and simply takes
 * nothing; an enemy warded by an Armadura and an Escudo Ungido takes it at half Duração. Neither
 * is this class's business, and neither needs a line here — {@code
 * CombatantSheet#applyEnchantment} is the single door both are decided at.
 *
 * <p><b>The PD buy foes, not Rodadas</b> — "2PD para 1 único inimigo, então de +1PD para cada
 * inimigo alvo adicional" — while the Duração is a flat 2 Rodadas. Under the previous rules
 * revision it was the other way round, the PD naming the Duração and the number of foes being
 * unbounded.
 *
 * <p>When the request carries the holder's {@code sceneContext}, foes already within range are
 * bound straight away through {@link Scene#refreshAura}; without one, that is the caller's next
 * step. The registered Aura is read back from {@link Scene#getActiveAuras()}.
 */
public class OrgulhoEldurianoInteraction extends AbstractTitleAbilityInteraction {

    /** "Todos os inimigos em Distância Curta". */
    static final Range AURA_RADIUS = Range.DISTANCIA_CURTA;

    /** "tem Duração de 2 Rodadas" — fixed, no longer scaled by the PD spent. */
    static final int DURATION_IN_ROUNDS = 2;

    /** The PD that buy the first foe; every PD beyond this one buys one more. */
    static final int BASE_DETERMINATION_COST = 2;

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
        scene.addAura(new ActiveAura(holder, getAbility(), AURA_RADIUS, DURATION_IN_ROUNDS,
                resolveMaxTargets(determinationPoints)));
        if (request.getSceneContext() != null) {
            scene.refreshAura(holder, request.getSceneContext());
        }
        return InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(holder))
                .build();
    }

    /**
     * "2PD para 1 único inimigo, então de +1PD para cada inimigo alvo adicional" — so 2PD binds 1,
     * 3PD binds 2, and so on. Floored at 1 because {@code PDCost.variable(2)} already refuses
     * anything below the base cost.
     */
    static int resolveMaxTargets(final int determinationPoints) {
        return Math.max(1, 1 + determinationPoints - BASE_DETERMINATION_COST);
    }
}
