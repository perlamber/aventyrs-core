package org.aventyrs.core.title.santo;

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
 * Glória Relampejante de Tesla's activation (Suprema, 3PD, Ação Livre) — "Você e seus aliados em
 * Distância Curta recebem Bônus de +2PA por 1 Rodada."
 *
 * <p><b>Reported</b> rather than applied, the same "compute what, caller applies who" shape {@code
 * GritoDeGuerraVulcanoInteraction} uses: the recipients are a set this class cannot resolve, so the
 * caller grants the {@link TargetScope#SELF_AND_ALLIES} Blessing to the activator plus {@code
 * SceneContext#getAlliesWithin(Range#DISTANCIA_CURTA)} — note <em>Curta</em>, not the adjacency
 * Grito de Guerra Vulcano names.
 *
 * <p>The PA bonus lands through {@code ActionPointsService#getMaxActionPoints}'s {@code
 * CombatantSheet} overloads; a caller reading PA through the {@code Character}-only overload still
 * won't see it, that overload having no sheet to ask.
 *
 * <p><b>V19 dropped this clause's second half.</b> The previous revision also granted RA, which is
 * why {@code DamageServiceImpl}'s timed-RA branch exists; that branch stays real and used
 * elsewhere, but this Suprema no longer grants any Redução Absoluta, and {@link #resolve} reports
 * one Blessing rather than two.
 */
public class GloriaRelampejanteDeTeslaInteraction extends AbstractTitleAbilityInteraction {

    static final int DURATION_IN_ROUNDS = 1;
    static final int ACTION_POINT_BONUS = 2;
    /** Who the reported Blessing is for, beyond the activator — the caller resolves them. */
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
                                TargetScope.SELF_AND_ALLIES, source)
                ))
                .build();
    }
}
