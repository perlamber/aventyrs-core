package org.aventyrs.core.effect;

import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TargetScope;

/**
 * Atordoante, for 1 Rodada. Maior — "não pode realizar nenhuma ação": PA, Reações and Ações Livres
 * all driven to nothing by {@link #NO_ACTIONS} Blessings (every counter floors at 0). Menor — "não
 * pode realizar Ações Livres e Reações e suas ações utilizam 1PA a mais": Reações and Ações Livres
 * to nothing, plus {@link ConditionType#CONFUSO}, whose rules text is exactly "O tempo de todas as
 * ações aumentam em 1PA".
 */
public class Atordoante extends AbstractCriticalEffect {

    /** Enough to take any counter to its floor of 0. */
    static final int NO_ACTIONS = -100;
    static final int ROUNDS = 1;


    public Atordoante(final CriticalEffectContext context) {
        super(context);
    }

    @Override
    public CriticalEffectType getType() {
        return CriticalEffectType.ATORDOANTE;
    }

    @Override
    protected String majorDescription() {
        return "o alvo não pode realizar nenhuma ação por 1 Rodada.";
    }

    @Override
    protected String minorDescription() {
        return "o alvo não pode realizar Ações Livres e Reações e suas ações utilizam 1PA a mais serem executadas por 1 Rodada.";
    }

    @Override
    protected InteractionResult.InteractionResultBuilder resolve(final CombatantSheet target) {
        String source = getType().name();
        target.grantBlessing(new Blessing(ModifierType.REACTIONS, NO_ACTIONS, ROUNDS, TargetScope.SELF, source));
        target.grantBlessing(new Blessing(ModifierType.FREE_ACTIONS, NO_ACTIONS, ROUNDS, TargetScope.SELF, source));
        if (getContext().isMajor()) {
            target.grantBlessing(new Blessing(ModifierType.ACTION_POINTS, NO_ACTIONS, ROUNDS, TargetScope.SELF, source));
        } else {
            target.applyCondition(new Condition(ConditionType.CONFUSO, ROUNDS, attacker()));
        }
        return InteractionResult.builder();
    }
}
