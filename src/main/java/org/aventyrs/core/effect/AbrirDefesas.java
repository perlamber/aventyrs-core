package org.aventyrs.core.effect;

import lombok.NonNull;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * {@code AtaqueCorpoACorpoCompetencyAbility#ABRIR_DEFESAS}: "Após um acerto crítico seu alvo recebe o
 * Malefício Desprevenido por 1 Rodada". Chained behind a critical hit's damage by {@code AttackDelivery},
 * so it lands wherever the chain is drained — the defender's own client for a remote target.
 *
 * <p>Not an Efeito Crítico of the catalog ({@code CriticalEffectType}): an anatomy immune to those does
 * not shrug this off. One Rodada, counted like {@link Atordoante}'s.
 */
public class AbrirDefesas extends AbstractEffect {

    /** "por 1 Rodada". */
    public static final int ROUNDS = 1;

    private final CombatantSheet attacker;

    public AbrirDefesas(@NonNull final CombatantSheet attacker) {
        this.attacker = attacker;
    }

    @Override
    public String getDescription() {
        return "Abrir Defesas: o alvo recebe o Malefício Desprevenido por 1 Rodada.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        target.applyCondition(new Condition(ConditionType.DESPREVENIDO, ROUNDS, attacker));
        return InteractionResult.builder().resultStatus(resolveStatus(target)).build();
    }
}
