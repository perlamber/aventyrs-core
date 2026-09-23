package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.EmpoweredAttack;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

/**
 * Placidez de Undine, Rancor de Haloi's activation (2PD, 3PA) — "como parte da ativação desta
 * Habilidade você deve desferir um ataque físico", and everything the Habilidade grants is scoped
 * to <b>that one attack</b>.
 *
 * <p><b>This reports; it never applies.</b> The activation resolves before the attack exists, so
 * the caller activates this, reads {@link InteractionResult#getEmpoweredAttack()}, and builds its
 * attack with those figures folded in — the same ordering that lets {@link GuardaVidasInteraction}
 * intercept without {@code AttackDelivery}/{@code AttackReceiver} changing at all. Nothing here
 * needs "the one attack being made right now" to be expressible inside the roll machinery, which
 * is what this clause was previously blocked on.
 *
 * <p>Two of the four figures could technically be applied and deliberately are not:
 * <ul>
 *   <li><b>Roubo de Vida 3</b> — {@code org.aventyrs.core.sheet.LifeSteal} is a {@code
 *       TemporaryEffect} counting down in <em>Rodadas</em>, and the shortest one lasts the whole
 *       Rodada. Granting it would keep stealing on every later attack that Rodada, which the text
 *       scopes to one attack ("Este ataque você recebe Roubo de Vida 3"). Same over-grant CLAUDE.md
 *       records for "sua próxima rolagem".</li>
 *   <li><b>Margem Crítica Menor +2</b> — has no {@code ModifierType} and no {@code TemporaryBonus}
 *       path at all; every existing source is a continuously-scanned {@code
 *       resolveCriticalMarginIncrease} hook, which would widen every attack rather than one.</li>
 * </ul>
 *
 * <p>No {@code validate}: the report is target-independent, and the one clause that would need a
 * target is TODO'd below. No {@code resolveHitPointCost} either — unlike its three siblings in
 * {@link AbracadoPelaEscuridaoAbility} this Habilidade is PD-priced, not Vigor-PV-priced.
 *
 * <p>TODO: "Roubo de Determinação 2" has no mechanism — only Roubo de Vida exists, via {@code
 * LifeStealService} (CLAUDE.md's "Roubo de Mana / de Determinação" gap row). It is computed and
 * reported on {@link EmpoweredAttack#determinationSteal()} and applied by nobody. V19 dropped this
 * clause's former "Roubo de Mana 2" half, so Determinação is now the only missing one.
 *
 * <p>TODO: "Inimigos que tenham sofrido danos desta Habilidade se tornam imunes a ela por 2
 * Rodadas" — {@code CombatantSheet#markAffectedUntilRest}/{@code #isAffectedUntilRest} is the right
 * <em>shape</em> (a source-keyed, per-target ledger, already used by {@code
 * AbencoadoPelaLuzAbility#ORGULHO_ELDURIANO}) but is scoped until a Descanso, not for a Rodada
 * count. It would also need the damaged target, which this activation never sees — the attack is
 * the caller's.
 */
public class PlacidezDeUndineRancorDeHaloiInteraction extends AbstractTitleAbilityInteraction {

    /** "sua Margem Crítica Menor para este ataque aumenta em +2 números." */
    static final int CRITICAL_MARGIN_INCREASE = 2;

    /** "Este ataque você recebe Roubo de Vida 3". */
    static final int LIFE_STEAL = 3;

    /** "Roubo de Determinação 2" — reported only; no mechanism exists (see class javadoc). */
    static final int DETERMINATION_STEAL = 2;

    /** "a Corrente de Efeitos – Rancor de Haloi: ... Oferenda Maldita como um Efeito Crítico adicional." */
    static final CriticalEffectType ADDITIONAL_CRITICAL_EFFECT = CriticalEffectType.OFERENDA_MALDITA;

    private final HitPointsService hitPointsService;

    public PlacidezDeUndineRancorDeHaloiInteraction() {
        this(new DeterminationPointsServiceImpl());
    }

    public PlacidezDeUndineRancorDeHaloiInteraction(final DeterminationPointsService determinationPointsService) {
        super(AbracadoPelaEscuridaoAbility.PLACIDEZ_DE_UNDINE_RANCOR_DE_HALOI, determinationPointsService);
        this.hitPointsService = new HitPointsServiceImpl();
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet activator = request.getActivator();
        return InteractionResult.builder()
                .empoweredAttack(EmpoweredAttack.ofCriticalAndSteal(CRITICAL_MARGIN_INCREASE, LIFE_STEAL,
                        DETERMINATION_STEAL, ADDITIONAL_CRITICAL_EFFECT))
                .resultStatus(hitPointsService.getStatus(activator))
                .build();
    }
}
