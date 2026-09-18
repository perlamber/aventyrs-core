package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import java.util.List;

/**
 * Glória Relampejante de Tesla's activation (Suprema, 2PD, Ação Livre) — "Você e seus aliados em
 * Distância Curta recebem Bônus de +1PA e RA por 1 Rodada."
 *
 * <p>Both halves are real now and both are <b>reported</b> rather than applied, the same
 * "compute what, caller applies who" shape {@code GritoDeGuerraVulcanoInteraction} uses: the
 * recipients are a set this class cannot resolve, so the caller grants each {@link
 * TargetScope#SELF_AND_ALLIES} Blessing to the activator plus {@code
 * SceneContext#getAlliesWithin(Range#DISTANCIA_CURTA)} — note <em>Curta</em>, not the adjacency
 * Grito de Guerra Vulcano names.
 *
 * <p>The +1PA lands through {@code ActionPointsService#getMaxActionPoints}'s {@code
 * CombatantSheet} overloads, and the RA through {@code DamageServiceImpl}'s timed-RA branch —
 * which exists <em>because</em> of this clause. A numberless "recebem RA" grants one instance,
 * {@code DamageService#DEFAULT_DAMAGE_REDUCTION}, the same convention {@code
 * SantoAbility#BASTIAO_DOS_NECESSITADOS} already reads it by.
 */
public class GloriaRelampejanteDeTeslaInteraction extends AbstractTitleAbilityInteraction {

    static final int DURATION_IN_ROUNDS = 1;
    static final int ACTION_POINT_BONUS = 1;
    /** Who the reported Blessings are for, beyond the activator — the caller resolves them. */
    public static final Range ALLY_RANGE = Range.DISTANCIA_CURTA;

    private final HitPointsService hitPointsService;

    public GloriaRelampejanteDeTeslaInteraction() {
        this(new DeterminationPointsServiceImpl());
    }

    public GloriaRelampejanteDeTeslaInteraction(final DeterminationPointsService determinationPointsService) {
        super(AbencoadoPelaLuzAbility.GLORIA_RELAMPEJANTE_DE_TESLA, determinationPointsService);
        this.hitPointsService = new HitPointsServiceImpl();
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        String source = AbencoadoPelaLuzAbility.GLORIA_RELAMPEJANTE_DE_TESLA.name();
        return InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(request.getActivator()))
                .blessings(List.of(
                        new Blessing(ModifierType.ACTION_POINTS, ACTION_POINT_BONUS, DURATION_IN_ROUNDS,
                                TargetScope.SELF_AND_ALLIES, source),
                        new Blessing(ModifierType.ABSOLUTE_DAMAGE_REDUCTION, DamageService.DEFAULT_DAMAGE_REDUCTION,
                                DURATION_IN_ROUNDS, TargetScope.SELF_AND_ALLIES, source)
                ))
                .build();
    }
}
