package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

/**
 * Furor de Sylph's activation (Suprema, 2PD as an Ação Livre, plus a PV cost equal to the
 * activator's Vigor) — "Você recebe Bônus de +1PA e seus ataques recebem … O Furor de Sylph
 * aprimora uma quantidade de ataques igual à 1+ metade dos PV gastos com esta Habilidade."
 *
 * <p><b>What this clause needed, and now has, is a budget rather than a Duração.</b> Everything it
 * grants is scoped to a <i>count of attacks</i> and the text names no Rodadas at all, so a {@code
 * TemporaryBonus} — which only ever counts down in Rodadas — could not carry it without inventing
 * a figure. The activation instead grants {@code CombatantSheet#grantEnhancedAttacks}, and the
 * caller spends one with {@code consumeEnhancedAttack} as it resolves each attack it means to
 * enhance. The budget is {@code AbracadoPelaEscuridaoAbility
 * #resolveEnhancedAttackCountFromPvSpent} over the PV actually paid.
 *
 * <p>The <b>+1PA rides on that same budget</b> rather than being granted: {@code
 * AventyrTitleAbility#resolveActionPointBonus} reports 1 while attacks remain, and {@code
 * ActionPointsServiceImpl} scans it. Recomputed from the budget, it is correct by construction as
 * the attacks are spent and needs nothing revoked — the same reasoning the Bastião dos
 * Necessitados scans use.
 *
 * <p>Reactivating <b>replaces</b> the remaining budget rather than banking onto it, the same way a
 * {@code Blessing} from one source replaces its predecessor.
 *
 * <p>TODO the per-attack half is still three separate gaps, and none of them is this clause's:
 * "o Aprimoramento de Obra-Prima <i>Alcance Estendido</i>" is not in the Obra-Prima catalogue at
 * all; "seu alvo é empurrado 1UD para trás e você pode se Reposicionar" needs forced movement (the
 * Reposicionar is modelled — {@code RepositionService} — the push is not); and "+1d6" of dano would be reportable
 * (see {@code FuriaDosDeusesInteraction}, which does exactly that) but belongs on the attack the
 * caller builds, which this activation — an Ação Livre taken once for several later attacks — is
 * not adjacent to. That last one is the difference from Fúria dos Deuses and Placidez: those
 * empower <em>one</em> named attack, this one funds several.
 *
 * <p>TODO "PV perdidos desta forma só podem ser recuperados com Descansos ou Roubo de Vida" — no
 * locked-PV subtype exists, the same gap {@code SantoAbility#PROTETOR_DA_VIDA_E_DA_MORTE} cites.
 */
public class FurorDeSylphInteraction extends AbstractTitleAbilityInteraction {

    /** "Você recebe Bônus de +1PA", for as long as enhanced attacks remain. */
    static final int ACTION_POINT_BONUS = 1;

    private final HitPointsService hitPointsService;

    public FurorDeSylphInteraction() {
        this(new DeterminationPointsServiceImpl());
    }

    public FurorDeSylphInteraction(final DeterminationPointsService determinationPointsService) {
        super(AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH, determinationPointsService);
        this.hitPointsService = new HitPointsServiceImpl();
    }

    /** "você deve gastar uma quantidade de pontos de vida igual ao seu Vigor". */
    @Override
    protected int resolveHitPointCost(final TitleAbilityActivationRequest request) {
        return AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH
                .resolveVigorPvCost(request.getActivator().getCharacter());
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet activator = request.getActivator();
        // The PV paid are the Vigor charged above — the same figure the budget is derived from.
        int pvSpent = AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH.resolveVigorPvCost(activator.getCharacter());
        activator.grantEnhancedAttacks(AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH,
                AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH.resolveEnhancedAttackCountFromPvSpent(pvSpent));

        return InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(activator))
                .build();
    }
}
