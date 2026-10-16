package org.aventyrs.core.effect.monstrous;

import lombok.NonNull;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.effect.AbstractEffect;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TemporaryEgoPenalty;

/**
 * "O Alvo perde 1 ponto temporário de Autocontrole … recuperados após Descanso" — the shape two Correntes de Efeitos
 * Monstruosas share (core 0.1.7.3): Possessão's <b>Enfraquecer o Espírito</b> ("efeito cumulativo … recuperados cada
 * após Descansos Longos") and the Nascido do Sangue de Lacerto's <b>Carnivoria</b> ("efeito de Veneno … recuperados
 * sempre que o alvo passar por qualquer efeito de Descanso"; Predador: "Apenas Descansos Verdadeiros").
 *
 * <p>The point is a {@link TemporaryEgoPenalty} on Autocontrole — a lowered ceiling, never a spend — lasting until the
 * Descanso the text names. Each landing is its own penalty, so they stack. A Veneno-classified one is refused by a
 * target immune to Envenenado.
 */
public class AutocontroleLoss extends AbstractEffect implements EffectChain {

    private final String name;
    private final RestType recoveredBy;
    private final boolean trueRestOnly;
    private final boolean poison;
    private final boolean torporAtZero;

    private AutocontroleLoss(@NonNull final String name, @NonNull final RestType recoveredBy,
                             final boolean trueRestOnly, final boolean poison, final boolean torporAtZero) {
        this.name = name;
        this.recoveredBy = recoveredBy;
        this.trueRestOnly = trueRestOnly;
        this.poison = poison;
        this.torporAtZero = torporAtZero;
    }

    /** Enfraquecer o Espírito — "recuperados cada após Descansos Longos". */
    public static AutocontroleLoss enfraquecerOEspirito() {
        return new AutocontroleLoss("Enfraquecer o Espírito", RestType.LONGO, false, false, false);
    }

    /**
     * Carnivoria's Autocontrole half — "qualquer efeito de Descanso", from Predador "Apenas Descansos Verdadeiros" —
     * and "Personagens que tenham o Autocontrole reduzido à zero desta forma sofrem obrigatoriamente o efeito Torpor":
     * the Autocontrole-a-Zero face Torpor is recorded rather than rolled. Its Roubo de Vida +1d6 is the active's.
     */
    public static AutocontroleLoss carnivoria(final boolean trueRestOnly) {
        return new AutocontroleLoss("Carnivoria", RestType.MINIMO, trueRestOnly, true, true);
    }

    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return name + " — o alvo perde 1 ponto temporário de Autocontrole até um Descanso.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        if (!poison || !target.isImmuneToCondition(ConditionType.ENVENENADO)) {
            TemporaryEgoPenalty loss = new TemporaryEgoPenalty(EgoDomain.AUTOCONTROLE, 1, null);
            if (trueRestOnly) {
                target.applyEffectUntilTrueRest(loss, recoveredBy);
            } else {
                target.applyEffectUntilRest(loss, recoveredBy);
            }
            if (torporAtZero && target.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE) <= 0) {
                target.recordEgoSetback(org.aventyrs.core.ego.EgoSetback.TORPOR);
            }
        }
        return reportChain(InteractionResult.builder().resultStatus(resolveStatus(target))).build();
    }
}
