package org.aventyrs.core.title.curandeiro;

import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.HealingSource;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import java.util.UUID;

import static org.aventyrs.core.util.TranslatableMessages.TARGET_ALREADY_AFFECTED_UNTIL_REST;
import static org.aventyrs.core.util.TranslatableMessages.TARGET_NOT_WOUNDED;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_CHOICE_NOT_PERMITTED;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_CHOICE_REQUIRED;

/**
 * Os dons de um Curandeiro — the Despertar's touch (3PA). <b>The roll is the caller's</b>: this core
 * throws no dice, so the caller rolls Medicina e Cura against {@link #DIFFICULTY} (the Despertar's
 * own Vantagem already reaches that roll, {@link Curandeiro#resolveSkillRollBonus}) and passes the
 * {@link Outcome} as the request's choice. {@link Corrente#BEIJO_DE_BOROS} among the choices asks for
 * the Descanso Longo Corrente, refused in a combat Cena — or with no {@code SceneContext}, since "fora
 * de combate" cannot be told without one.
 *
 * <p>"Você não pode afetar um mesmo personagem desta forma até que você passe por um Descanso Longo,
 * mesmo que você não tenha sido bem-sucedido" — the mark is kept on the <b>Curandeiro's</b> sheet
 * ({@code markAffectedUntilRest}, keyed by the target), so it is the Curandeiro's own Descanso Longo
 * that lifts it, and a failed roll sets it too.
 */
public class DonsDoCurandeiroInteraction extends AbstractTitleAbilityInteraction {

    /** "com GD Difícil". */
    public static final DifficultyLevel DIFFICULTY = DifficultyLevel.HARD;

    /** How the caller's Medicina e Cura roll against {@link #DIFFICULTY} came out. */
    public enum Outcome { SUCCEEDED, FAILED }

    /** The Corrente de Efeitos this activation may carry. */
    public enum Corrente { BEIJO_DE_BOROS }

    /** The "already touched" mark, one per target, held on the Curandeiro's sheet. */
    record TouchMark(UUID targetId) {
    }

    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    public DonsDoCurandeiroInteraction() {
        this(new DeterminationPointsServiceImpl());
    }

    public DonsDoCurandeiroInteraction(final DeterminationPointsService determinationPointsService) {
        super(CurandeiroDespertar.OS_DONS_DE_UM_CURANDEIRO, determinationPointsService);
    }

    /** The heal this touch is, by healer — relayed ({@code HealingSource#relay}) for a target elsewhere. */
    public static HealingSource source(final CombatantSheet healer) {
        return HealingSource.titleAbility(CurandeiroDespertar.OS_DONS_DE_UM_CURANDEIRO, healer);
    }

    /**
     * The target's half of a successful touch — "recupera PV como se passasse por um Descanso Curto"
     * (Longo with Beijo de Boros), read off the <b>target's</b> own Vigor. Public so the client holding
     * the target's real sheet can apply a touch made elsewhere. Returns the PV actually recovered.
     */
    public static int healTarget(final CombatantSheet target, final HealingSource source, final boolean beijoDeBoros) {
        RestType equivalent = beijoDeBoros ? RestType.LONGO : RestType.CURTO;
        int damageBefore = target.getDamageTaken();
        target.heal(new RestServiceImpl().getRecoveredHitPoints(target.getCharacter(), equivalent), source);
        return damageBefore - target.getDamageTaken();
    }

    @Override
    protected void validate(final TitleAbilityActivationRequest request) {
        CombatantSheet target = request.getEffectiveTarget();
        if (request.getChoice(Outcome.class).isEmpty()) {
            throw new IllegalOperationException(TITLE_ABILITY_CHOICE_REQUIRED);
        }
        if (target.getDamageTaken() <= 0) {
            throw new IllegalOperationException(TARGET_NOT_WOUNDED);
        }
        if (request.getActivator().isAffectedUntilRest(new TouchMark(target.getId()))) {
            throw new IllegalOperationException(TARGET_ALREADY_AFFECTED_UNTIL_REST);
        }
        if (request.getChoices(Corrente.class).contains(Corrente.BEIJO_DE_BOROS)
                && (request.getSceneContext() == null || request.getSceneContext().isCombatScene())) {
            throw new IllegalOperationException(TITLE_ABILITY_CHOICE_NOT_PERMITTED);
        }
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet activator = request.getActivator();
        CombatantSheet target = request.getEffectiveTarget();
        activator.markAffectedUntilRest(new TouchMark(target.getId()), RestType.LONGO);
        InteractionResult.InteractionResultBuilder result = InteractionResult.builder();
        if (request.getChoice(Outcome.class).orElseThrow() == Outcome.SUCCEEDED) {
            int recovered = healTarget(target, source(activator),
                    request.getChoices(Corrente.class).contains(Corrente.BEIJO_DE_BOROS));
            result.resourceGainValue(recovered)
                    .resourceGainType(ResourceType.HIT_POINTS)
                    .succeeded(true);
        } else {
            result.succeeded(false);
        }
        return result.resultStatus(hitPointsService.getStatus(target)).build();
    }
}
