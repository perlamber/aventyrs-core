package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_CHOICE_REQUIRED;

/**
 * Abençoado pela Luz's own touch-heal-or-cure effect — an {@link AbstractTitleAbilityInteraction},
 * so the shared gates run and the activator's <b>3PV</b> are paid first. It acts on the request's
 * {@link TitleAbilityActivationRequest#getEffectiveTarget() target} (the touched character — the
 * activator themself when none is named), and the player picks the branch through the request's
 * {@link Branch} choice. Activated via {@code AventyrTitle#activateAbility} (or {@link
 * Santo#activateAbencoadoPelaLuz}), which checks the Especialização is held first.
 *
 * <p>V19 changed both halves of the cost/effect: this Especialização is priced in PV rather than
 * PD (so its {@code PDCost} is genuinely 0, and {@link #resolveHitPointCost} charges the 3PV — an
 * activation that would drop the Santo to 0 PV is refused by the base class), and the heal is a
 * flat "3+ Quantidade de Habilidades de Abençoado pela Luz" rather than a Descanso Curto's worth,
 * so nothing here reads {@code RestService} any more.
 *
 * <p>No Perícia roll is involved — no {@code skillRollBonus}, no dice: this is a direct effect.
 *
 * <p>TODO "Pontos de Vida utilizados para ativar esta Habilidade só podem ser recuperados com
 * Descansos Verdadeiros" — no locked-PV subtype exists, the same gap {@code
 * SantoAbility#PROTETOR_DA_VIDA_E_DA_MORTE} and {@link SacrificioYmirianoInteraction} both cite.
 */
public class AbencoadoPelaLuzInteraction extends AbstractTitleAbilityInteraction {

    /** "Custo de Ativação: 3PV" — this Especialização is priced in PV, not PD. */
    static final int TOUCH_HIT_POINT_COST = 3;

    /** The two things Abençoado pela Luz can do with one touch. */
    public enum Branch {
        /** Heal the target 3 + the Santo's count of Abençoado pela Luz Habilidades. */
        HEAL,
        /** Remove a Malefício — still inert, see {@link #resolve}. */
        REMOVE_MALEFICIO
    }

    private final HitPointsService hitPointsService;

    public AbencoadoPelaLuzInteraction() {
        this(new HitPointsServiceImpl());
    }

    public AbencoadoPelaLuzInteraction(final HitPointsService hitPointsService) {
        this(hitPointsService, new DeterminationPointsServiceImpl());
    }

    public AbencoadoPelaLuzInteraction(final HitPointsService hitPointsService,
                                       final DeterminationPointsService determinationPointsService) {
        super(SantoSpecialization.ABENCOADO_PELA_LUZ, determinationPointsService);
        this.hitPointsService = hitPointsService;
    }

    /** "Custo de Ativação: 3PV" — paid by the activator, never by the touched target. */
    @Override
    protected int resolveHitPointCost(final TitleAbilityActivationRequest request) {
        return TOUCH_HIT_POINT_COST;
    }

    /**
     * The branch is the player's call and there is no safe default between a heal and a cure, so
     * an activation without one is refused before anything is spent.
     */
    @Override
    protected void validate(final TitleAbilityActivationRequest request) {
        if (request.getChoice(Branch.class).isEmpty()) {
            throw new IllegalOperationException(TITLE_ABILITY_CHOICE_REQUIRED);
        }
    }

    /**
     * {@link Branch#HEAL} heals the target 3 + the <em>activator's</em> count of Abençoado pela Luz
     * Habilidades — real, via {@link SantoSpecialization#resolveTouchHealAmount} — reported as a
     * {@link InteractionResult#getResourceGainValue()}/{@link InteractionResult#getResourceGainType()}
     * pair. Note the count is the toucher's, not the touched character's: it is the Santo's own
     * mastery that makes the touch stronger.
     *
     * <p>{@link Branch#REMOVE_MALEFICIO} is still TODO'd — V19 widened its list to four Malefícios
     * (Doença, Encantamento, Maldição, Veneno) but the blocker is unchanged: "Encantamento" is not
     * a {@code ConditionType} at all, and this activation has no entry point for which one to
     * remove. It applies nothing and reports an inert result; the PV are still paid, since the
     * activation itself did happen.
     */
    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet target = request.getEffectiveTarget();
        if (request.getChoice(Branch.class).orElseThrow() != Branch.HEAL) {
            return InteractionResult.builder()
                    .resultStatus(hitPointsService.getStatus(target))
                    .build();
        }
        int healed = SantoSpecialization.ABENCOADO_PELA_LUZ
                .resolveTouchHealAmount(grantingTitleOf(request.getActivator()));
        target.heal(healed);
        return InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(target))
                .resourceGainValue(healed)
                .resourceGainType(ResourceType.HIT_POINTS)
                .build();
    }

    /**
     * The activator's own Título carrying this Especialização — what "Quantidade de Habilidades de
     * Abençoado pela Luz" counts against. {@code null} when the activator holds none, which {@link
     * SantoSpecialization#resolveTouchHealAmount} reads as zero Habilidades rather than failing:
     * {@code AventyrTitle#activateAbility} has already established the trait is held by the time
     * this runs, so a miss here can only mean a caller built the Interaction directly.
     */
    private AventyrTitle grantingTitleOf(final CombatantSheet activator) {
        return activator.getCharacter().getAllTitles().stream()
                .filter(title -> title.getSpecializations().contains(SantoSpecialization.ABENCOADO_PELA_LUZ))
                .findFirst()
                .orElse(null);
    }
}
