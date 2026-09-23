package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * Toque do AEther — "Alvo não pode Conjurar Magias e Ativar Habilidades de Títulos ou Monstruosas"
 * for 2 Rodadas (Maior) / 1 (Menor): {@link ConditionType#SILENCIO}, whose two prohibitions are
 * exactly those.
 *
 * <p>TODO "Magias e Habilidades do alvo, ou que estejam encantando o alvo, são interrompidas":
 * nothing tracks which Magias are running or affecting a combatant, so there is nothing to end.
 */
public class ToqueDoAether extends AbstractCriticalEffect {


    public ToqueDoAether(final CriticalEffectContext context) {
        super(context);
    }

    @Override
    public CriticalEffectType getType() {
        return CriticalEffectType.TOQUE_DO_AETHER;
    }

    @Override
    protected String majorDescription() {
        return "Alvo não pode Conjurar Magias e Ativar Habilidades de Títulos ou Monstruosas por 2 Rodadas. Magias e Habilidades do alvo, ou que estejam encantando o alvo, são interrompidas.";
    }

    @Override
    protected String minorDescription() {
        return "Alvo não pode Conjurar Magias e Ativar Habilidades de Títulos ou Monstruosas por 1 Rodada. Magias e Habilidades do alvo, ou que estejam encantando o alvo, conjuradas ou ativadas na última Rodada são interrompidas.";
    }

    @Override
    protected InteractionResult.InteractionResultBuilder resolve(final CombatantSheet target) {
        target.applyCondition(new Condition(ConditionType.SILENCIO, pick(2, 1), attacker()));
        return InteractionResult.builder();
    }
}
