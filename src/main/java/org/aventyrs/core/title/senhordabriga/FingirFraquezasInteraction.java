package org.aventyrs.core.title.senhordabriga;

import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ForcedTargeting;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_PD_AMOUNT;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_REQUIRES_NATURAL_WEAPONS_ONLY;

/**
 * Fingir Fraquezas' activation (Variável PD, 1PA) — "Você recebe Bônus de 1+ Número de Inimigos em
 * Distância Muito Curta em suas Defesas, o Custo de Ativação desta Habilidade é igual a este
 * valor."
 *
 * <p>The PD are not the player's to pick: {@link #validate} refuses any amount other than {@link
 * #resolveCost} ({@code INVALID_PD_AMOUNT}), which counts enemies at {@link #ENEMY_RANGE} off the
 * activator's own {@code sceneContext} (none when there is no context — a cost of 1). It also
 * refuses while any weapon but an Arma Natural is drawn ("apenas se você estiver desarmado (exceto
 * armas naturais)").
 *
 * <p>Then, applied: a {@code DEFESAS} Blessing of that same figure on the activator for {@link
 * #ROUNDS} Rodadas — the clause names no Duração of its own; 2 is the table's ruling, matching the
 * compulsion's; and on each of those enemies not already caught since their last Descanso, a
 * {@link ForcedTargeting} for {@link #ROUNDS} Rodadas cast through {@code
 * CombatantSheet#applyEnchantment} — "este é um efeito de Encantamento", so an immune enemy takes
 * nothing — and marked with {@code markAffectedUntilRest} at {@link RestType#MINIMO}, the weakest
 * Descanso, since "até passarem por um Descanso" names none in particular.
 *
 * <p>TODO "Inimigos inteligentes": see {@link FantasmaDoRingueAbility#FINGIR_FRAQUEZAS}.
 */
public class FingirFraquezasInteraction extends AbstractTitleAbilityInteraction {

    /** "Inimigos em Distância Muito Curta". */
    public static final Range ENEMY_RANGE = Range.DISTANCIA_MUITO_CURTA;

    /** "vão sempre desferir o primeiro ataque das próximas 2 Rodadas contra você". */
    static final int ROUNDS = 2;

    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    public FingirFraquezasInteraction() {
        this(new DeterminationPointsServiceImpl());
    }

    public FingirFraquezasInteraction(final DeterminationPointsService determinationPointsService) {
        super(FantasmaDoRingueAbility.FINGIR_FRAQUEZAS, determinationPointsService);
    }

    /** "1+ Número de Inimigos em Distância Muito Curta" — both the price in PD and the Defesas bonus. */
    public static int resolveCost(final SceneContext sceneContext) {
        return 1 + (sceneContext == null ? 0 : sceneContext.countEnemiesWithin(ENEMY_RANGE));
    }

    @Override
    protected void validate(final TitleAbilityActivationRequest request) {
        if (!request.getActivator().getCharacter().isArmedOnlyWithNaturalWeapons()) {
            throw new IllegalOperationException(TITLE_ABILITY_REQUIRES_NATURAL_WEAPONS_ONLY);
        }
        if (request.getDeterminationPoints() == null
                || request.getDeterminationPoints() != resolveCost(request.getSceneContext())) {
            throw new IllegalOperationException(INVALID_PD_AMOUNT);
        }
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet activator = request.getActivator();
        activator.grantBlessing(new Blessing(ModifierType.DEFESAS, determinationPoints, ROUNDS, TargetScope.SELF,
                FantasmaDoRingueAbility.FINGIR_FRAQUEZAS.name()));

        SceneContext context = request.getSceneContext();
        List<CombatantSheet> enemies = context == null ? List.of() : context.getEnemiesWithin(ENEMY_RANGE);
        for (CombatantSheet enemy : enemies) {
            if (enemy.isAffectedUntilRest(FantasmaDoRingueAbility.FINGIR_FRAQUEZAS)) {
                continue;
            }
            if (enemy.applyEnchantment(new ForcedTargeting(activator, ROUNDS))) {
                enemy.markAffectedUntilRest(FantasmaDoRingueAbility.FINGIR_FRAQUEZAS, RestType.MINIMO);
            }
        }
        return InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(activator))
                .build();
    }
}
