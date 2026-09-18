package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_ACTIVATION_LIMIT_REACHED;

/**
 * Sacrifício Ymiriano's activation (1PA, and a PV cost equal to the activator's Vigor) — "+2 em
 * Força por 2 Rodadas", becoming "+3, Categoria de Tamanho +1, 3 Rodadas" when it is activated
 * <b>twice in the same Turno</b>.
 *
 * <p>The PV cost is the base class's {@link #resolveHitPointCost}, computed by {@code
 * AbracadoPelaEscuridaoAbility#resolveVigorPvCost}; an activation that would drop its holder to 0
 * PV is refused there. "Até duas vezes" is enforced here against {@code
 * CombatantSheet#countActivationsThisTurn}, which the base advances before {@link #resolve} runs —
 * so the second activation reads 2 and a third is refused.
 *
 * <p>Both grants are self-scoped and applied here. They are one {@code Blessing} each <em>from the
 * same source</em>, so the second activation's +3 <b>replaces</b> the +2 and renews its Duração
 * rather than totalling +5 — which is exactly what "o Bônus em Força muda para +3" says. That is
 * why "seu efeito é cumulativo" needs no accumulation of its own: what the second activation
 * changes is the figure, the Duração and the Categoria de Tamanho.
 *
 * <p><b>Reach:</b> a {@code <ATTR>_BONUS} {@code TemporaryBonus} is read on the Perícia-roll path
 * only ({@code AbstractSkillInteraction}), so this Força bonus reaches a Força-governed roll and
 * nothing else — not PV/PM/PD, and not the melee ½-Força dano term, both of which read {@code
 * Character#getEffectiveAttributeTotal}, which has no sheet. The Categoria de Tamanho shift is read
 * by {@code CharacterSizeService#getEffectiveSizeCategory(CombatantSheet)}.
 *
 * <p>TODO "Pontos de Vida perdidos desta forma só podem ser recuperados com Descansos Verdadeiros
 * ou Roubo de Vida" — no locked-PV subtype exists, so a plain heal still restores these PV (the
 * same gap {@code SantoAbility#PROTETOR_DA_VIDA_E_DA_MORTE} cites).
 */
public class SacrificioYmirianoInteraction extends AbstractTitleAbilityInteraction {

    static final int MAXIMUM_ACTIVATIONS_PER_TURN = 2;
    static final int STRENGTH_BONUS = 2;
    static final int UPGRADED_STRENGTH_BONUS = 3;
    static final int DURATION_IN_ROUNDS = 2;
    static final int UPGRADED_DURATION_IN_ROUNDS = 3;
    static final int SIZE_CATEGORY_INCREASE = 1;

    private final HitPointsService hitPointsService;

    public SacrificioYmirianoInteraction() {
        this(new DeterminationPointsServiceImpl());
    }

    public SacrificioYmirianoInteraction(final DeterminationPointsService determinationPointsService) {
        super(AbracadoPelaEscuridaoAbility.SACRIFICIO_YMIRIANO, determinationPointsService);
        this.hitPointsService = new HitPointsServiceImpl();
    }

    /** "gastar uma quantidade de pontos de vida igual ao seu Vigor". */
    @Override
    protected int resolveHitPointCost(final TitleAbilityActivationRequest request) {
        return AbracadoPelaEscuridaoAbility.SACRIFICIO_YMIRIANO
                .resolveVigorPvCost(request.getActivator().getCharacter());
    }

    /** "Esta Habilidade pode ser ativada até duas vezes" — counted within one Turno. */
    @Override
    protected void validate(final TitleAbilityActivationRequest request) {
        if (request.getActivator().countActivationsThisTurn(getAbility()) >= MAXIMUM_ACTIVATIONS_PER_TURN) {
            throw new IllegalOperationException(TITLE_ABILITY_ACTIVATION_LIMIT_REACHED);
        }
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet activator = request.getActivator();
        boolean secondThisTurn = activator.countActivationsThisTurn(getAbility()) >= MAXIMUM_ACTIVATIONS_PER_TURN;
        int rounds = secondThisTurn ? UPGRADED_DURATION_IN_ROUNDS : DURATION_IN_ROUNDS;
        String source = AbracadoPelaEscuridaoAbility.SACRIFICIO_YMIRIANO.name();

        activator.grantBlessing(new Blessing(AttributeDomain.STRENGTH.getBonusModifierType(),
                secondThisTurn ? UPGRADED_STRENGTH_BONUS : STRENGTH_BONUS, rounds, TargetScope.SELF, source));
        if (secondThisTurn) {
            activator.grantBlessing(new Blessing(ModifierType.SIZE_CATEGORY, SIZE_CATEGORY_INCREASE, rounds,
                    TargetScope.SELF, source));
        }
        return InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(activator))
                .build();
    }
}
