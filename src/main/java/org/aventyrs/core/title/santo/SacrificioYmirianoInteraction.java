package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

/**
 * Sacrifício Ymiriano's activation (1PA, and a PV cost equal to the activator's Vigor) — "Bônus
 * Variável de Força igual ao seu Vigor e sua Categoria de Tamanho aumenta em +2 por 1 Rodada".
 *
 * <p>The PV cost is the base class's {@link #resolveHitPointCost}, computed by {@code
 * AbracadoPelaEscuridaoAbility#resolveVigorPvCost}; an activation that would drop its holder to 0
 * PV is refused there. <b>The same Vigor total is both the price and the benefit</b> — the clause
 * pays Vigor in PV and grants Vigor in Força — so both read the one helper rather than restating
 * the lookup.
 *
 * <p>Both grants are self-scoped and applied here, for a flat 1 Rodada. They are one {@code
 * Blessing} each from the same source, so re-activating replaces rather than stacks — which under
 * V19 is simply how a repeat works: that revision dropped the previous "pode ser ativada até duas
 * vezes / seu efeito é cumulativo / se ativada duas vezes no mesmo Turno" clause entirely, and with
 * it this class's activation limit, its upgraded figures and its {@code validate} override.
 * Nothing here reads {@code CombatantSheet#countActivationsThisTurn} any more.
 *
 * <p><b>Reach:</b> a {@code <ATTR>_BONUS} {@code TemporaryBonus} is read on the Perícia-roll path
 * only ({@code AbstractSkillInteraction}), so this Força bonus reaches a Força-governed roll and
 * nothing else — not PV/PM/PD, and not the melee ½-Força dano term, both of which read {@code
 * Character#getEffectiveAttributeTotal}, which has no sheet. The Categoria de Tamanho shift is read
 * by {@code CharacterSizeService#getEffectiveSizeCategory(CombatantSheet)}.
 *
 * <p>TODO "a Margem Crítica Menor de seus ataques direcionados à inimigos que tenham infligido
 * danos aos seus aliados aumenta em +2" — nothing tracks which enemies have harmed the holder's
 * group. {@code Scene#recordAttack} records that an attack was declared, not that it landed, and
 * has no ally-scoped variant; see this constant's own comment in {@link
 * AbracadoPelaEscuridaoAbility}.
 *
 * <p>TODO "Pontos de Vida perdidos desta forma só podem ser recuperados com Descansos Verdadeiros
 * ou Roubo de Vida" — no locked-PV subtype exists, so a plain heal still restores these PV (the
 * same gap {@code SantoAbility#PROTETOR_DA_VIDA_E_DA_MORTE} cites).
 */
public class SacrificioYmirianoInteraction extends AbstractTitleAbilityInteraction {

    /** "por 1 Rodada". */
    static final int DURATION_IN_ROUNDS = 1;

    /** "sua Categoria de Tamanho aumenta em +2". */
    static final int SIZE_CATEGORY_INCREASE = 2;

    private final HitPointsService hitPointsService;

    public SacrificioYmirianoInteraction() {
        this(new DeterminationPointsServiceImpl());
    }

    public SacrificioYmirianoInteraction(final DeterminationPointsService determinationPointsService) {
        super(AbracadoPelaEscuridaoAbility.SACRIFICIO_YMIRIANO, determinationPointsService);
        this.hitPointsService = new HitPointsServiceImpl();
    }

    /** "O Custo de Ativação desta Habilidade é igual ao seu próprio Vigor em PV." */
    @Override
    protected int resolveHitPointCost(final TitleAbilityActivationRequest request) {
        return AbracadoPelaEscuridaoAbility.SACRIFICIO_YMIRIANO
                .resolveVigorPvCost(request.getActivator().getCharacter());
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet activator = request.getActivator();
        String source = AbracadoPelaEscuridaoAbility.SACRIFICIO_YMIRIANO.name();
        // "Bônus Variável de Força igual ao seu Vigor" — the same figure the PV cost above charges.
        int strengthBonus = AbracadoPelaEscuridaoAbility.SACRIFICIO_YMIRIANO
                .resolveVigorPvCost(activator.getCharacter());

        activator.grantBlessing(new Blessing(AttributeDomain.STRENGTH.getBonusModifierType(),
                strengthBonus, DURATION_IN_ROUNDS, TargetScope.SELF, source));
        activator.grantBlessing(new Blessing(ModifierType.SIZE_CATEGORY, SIZE_CATEGORY_INCREASE,
                DURATION_IN_ROUNDS, TargetScope.SELF, source));

        return InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(activator))
                .build();
    }
}
