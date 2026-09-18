package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import java.util.List;

/**
 * Grito de Guerra Vulcano's own activation — an {@link AbstractTitleAbilityInteraction}, so the
 * shared gates run and its 3PD are paid first. Its effect is <b>reported, not applied</b>: every
 * grant goes out via {@link InteractionResult#getBlessings()} — this ability's own rules text grants
 * "a você e seus aliados adjacentes," the same self-plus-allies shape {@code
 * ArtesCompetencyAbility#DOM_BARDICO} already reports this way, just with more than one {@link
 * Blessing} at once. A caller resolves the concrete recipients (the actor itself, plus {@code
 * SceneContext#getAlliesWithin(Range.ADJACENTE)} for each {@link TargetScope#SELF_AND_ALLIES}
 * entry) and calls {@code CombatantSheet#grantBlessing} on each — the PD the activator pays is the
 * only thing this class changes. Activated via {@code AventyrTitle#activateAbility} (or {@link
 * Santo#activateGritoDeGuerraVulcano}), which checks the Habilidade is held first.
 *
 * <p>Every clause of this ability's own rules text is reported as a real {@link Blessing},
 * including the "+2 em Defesas" one ({@link ModifierType#DEFESAS}): {@code
 * character.services.DefenseService} sums every active {@code DEFESAS}-typed {@code
 * TemporaryBonus}, so that grant lands for real once a caller applies it.
 */
public class GritoDeGuerraVulcanoInteraction extends AbstractTitleAbilityInteraction {

    private static final int VANTAGEM_ROUNDS = 2;
    private static final int DEFESAS_BONUS = 2;

    private final HitPointsService hitPointsService;

    public GritoDeGuerraVulcanoInteraction() {
        this(new HitPointsServiceImpl(), new DeterminationPointsServiceImpl());
    }

    public GritoDeGuerraVulcanoInteraction(final HitPointsService hitPointsService) {
        this(hitPointsService, new DeterminationPointsServiceImpl());
    }

    public GritoDeGuerraVulcanoInteraction(final HitPointsService hitPointsService,
                                           final DeterminationPointsService determinationPointsService) {
        super(AbencoadoPelaLuzAbility.GRITO_DE_GUERRA_VULCANO, determinationPointsService);
        this.hitPointsService = hitPointsService;
    }

    /**
     * Activates with actor's own sceneContext. The context isn't consulted by this ability's
     * rules text — every Blessing is {@link TargetScope#SELF_AND_ALLIES} regardless, recipients
     * being the caller's job — but it reaches the shared Silêncio gate.
     */
    public InteractionResult applyTo(final CombatantSheet actor, final SceneContext sceneContext) {
        return activate(TitleAbilityActivationRequest.builder()
                .activator(actor)
                .sceneContext(sceneContext)
                .build());
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        String source = AbencoadoPelaLuzAbility.GRITO_DE_GUERRA_VULCANO.name();
        return InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(request.getActivator()))
                .blessings(List.of(
                        new Blessing(ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS, Skill.ADVANTAGE_BONUS, VANTAGEM_ROUNDS, TargetScope.SELF_AND_ALLIES, source),
                        new Blessing(ModifierType.ATAQUE_CORPO_A_CORPO_ROLL_BONUS, Skill.ADVANTAGE_BONUS, VANTAGEM_ROUNDS, TargetScope.SELF_AND_ALLIES, source),
                        new Blessing(ModifierType.DEFESAS, DEFESAS_BONUS, VANTAGEM_ROUNDS, TargetScope.SELF_AND_ALLIES, source)
                ))
                .build();
    }
}
