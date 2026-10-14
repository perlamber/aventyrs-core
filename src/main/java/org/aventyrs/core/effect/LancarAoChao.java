package org.aventyrs.core.effect;

import lombok.Getter;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * Lançar ao Chão — "Alvo recebe a Condição Caído." A generic Corrente de Efeitos (V19 20261012), carried by the
 * Encontrão manoeuvre of {@code V19 - Ações de Combate}, not yet imported.
 *
 * <p>{@link AgarrarEDerrubar} without its size gate: Caído is applied open-ended, naming the attacker as its source,
 * since the clause states no Duração and getting up is the target's own action ({@code StandUpService}).
 */
@Getter
public class LancarAoChao extends AbstractEffect implements EffectChain {

    /** Who knocked the target down — the Condição's source; may be {@code null}. */
    private final CombatantSheet attacker;

    public LancarAoChao(final CombatantSheet attacker) {
        this.attacker = attacker;
    }

    @Override
    public String getDescription() {
        return "Alvo recebe a Condição Caído.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        target.applyCondition(new Condition(ConditionType.CAIDO, null, attacker));
        return reportChain(InteractionResult.builder()
                .resultStatus(resolveStatus(target)))
                .build();
    }
}
