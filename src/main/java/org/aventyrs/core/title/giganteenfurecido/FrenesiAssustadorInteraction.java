package org.aventyrs.core.title.giganteenfurecido;

import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.FrightfulCondition;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InflictedCondition;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import java.util.ArrayList;
import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_ACTIVATION_LIMIT_REACHED;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_REQUIRES_SCENE;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_TRIGGER_NOT_MET;

/**
 * Frenesi Assustador. Only after a kill or a critical — {@link GiganteEnfurecido#recordTriumph} — and
 * once a Rodada. Each enemy in Distância Curta whose current temporary Autocontrole exceeds the
 * activator's climbs one rung of the fear ladder ({@link ConditionType#escalateFear}) for 2 Rodadas,
 * "renovando a Duração", as an Encantamento ({@link FrightfulCondition}); a foe with no Autocontrole
 * pool has 0 and is never scared. Applied to the sheets in hand and reported per target.
 */
public class FrenesiAssustadorInteraction extends AbstractTitleAbilityInteraction {

    /** "recebem a Condição Abalado por 2 Rodadas". */
    static final int FEAR_ROUNDS = 2;

    public FrenesiAssustadorInteraction() {
        super(BerserkerAbility.FRENESI_ASSUSTADOR);
    }

    @Override
    protected void validate(final TitleAbilityActivationRequest request) {
        CombatantSheet activator = request.getActivator();
        if (!GiganteEnfurecido.hasTriumph(activator)) {
            throw new IllegalOperationException(TITLE_ABILITY_TRIGGER_NOT_MET);
        }
        if (activator.isAffectedThisCombat(roundMark(request))) {
            throw new IllegalOperationException(TITLE_ABILITY_ACTIVATION_LIMIT_REACHED);
        }
        if (request.getSceneContext() == null) {
            throw new IllegalOperationException(TITLE_ABILITY_REQUIRES_SCENE);
        }
    }

    private static GiganteEnfurecido.OncePerRound roundMark(final TitleAbilityActivationRequest request) {
        return new GiganteEnfurecido.OncePerRound(BerserkerAbility.FRENESI_ASSUSTADOR,
                GiganteEnfurecido.currentRound(request.getSceneContext()), null);
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet activator = request.getActivator();
        activator.markAffectedThisCombat(roundMark(request));
        int own = activator.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE);
        List<InflictedCondition> inflicted = new ArrayList<>();
        for (CombatantSheet enemy : request.getSceneContext().getEnemiesWithin(Range.DISTANCIA_CURTA)) {
            if (enemy.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE) <= own) {
                continue;
            }
            ConditionType next = ConditionType.escalateFear(currentFear(enemy));
            FrightfulCondition fear = new FrightfulCondition(next, FEAR_ROUNDS, activator);
            if (enemy.applyEnchantment(fear)) {
                enemy.removeCondition(ConditionType.ABALADO);
                enemy.removeCondition(ConditionType.ASSUSTADO);
                enemy.removeCondition(ConditionType.APAVORADO);
                enemy.applyCondition(fear);
                inflicted.add(new InflictedCondition(enemy, next, FEAR_ROUNDS));
            }
        }
        return InteractionResult.builder()
                .inflictedConditions(inflicted.isEmpty() ? null : List.copyOf(inflicted))
                .build();
    }

    /** The highest fear rung enemy is under, or {@code null}. */
    static ConditionType currentFear(final CombatantSheet enemy) {
        for (ConditionType rung : List.of(ConditionType.APAVORADO, ConditionType.ASSUSTADO, ConditionType.ABALADO)) {
            if (enemy.hasCondition(rung, null)) {
                return rung;
            }
        }
        return null;
    }
}
