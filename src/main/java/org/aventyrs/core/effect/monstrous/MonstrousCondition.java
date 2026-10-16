package org.aventyrs.core.effect.monstrous;

import lombok.NonNull;
import org.aventyrs.core.effect.AbstractEffect;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.Disease;
import org.aventyrs.core.sheet.InteractionResult;

import java.util.List;

/**
 * A Corrente de Efeitos Monstruosa whose whole effect is Condições on its target (core 0.1.7.3):
 * <ul>
 *   <li>{@link #titere} — Títere's "personagens afetados são Possuídos por 2 Rodada" ({@code
 *   EscolhidoDeMacariaAbility#MESTRE_DAS_MARIONETES});</li>
 *   <li>{@link #pragaZumbi} — "Alvo recebe o Estado Doente …, esta Doença pode ser Propagar" for 3 Rodadas, plus
 *   Silêncio at Abominação ({@code #CUSPIDOR_DE_PRAGAS}).</li>
 * </ul>
 * A target immune to the Condição takes none of it.
 */
public final class MonstrousCondition extends AbstractEffect implements EffectChain {

    private final String name;
    private final CombatantSheet attacker;
    private final int rounds;
    private final boolean disease;
    private final List<ConditionType> conditions;

    private MonstrousCondition(@NonNull final String name, @NonNull final CombatantSheet attacker, final int rounds,
                               final boolean disease, @NonNull final List<ConditionType> conditions) {
        this.name = name;
        this.attacker = attacker;
        this.rounds = rounds;
        this.disease = disease;
        this.conditions = List.copyOf(conditions);
    }

    /** Títere: Possessão for 2 Rodadas. */
    public static MonstrousCondition titere(final CombatantSheet attacker) {
        return new MonstrousCondition("Títere", attacker, 2, false, List.of(ConditionType.POSSESSAO));
    }

    /** Praga Zumbi: a propagating Doença for 3 Rodadas; Silêncio too at Abominação. */
    public static MonstrousCondition pragaZumbi(final CombatantSheet attacker, final boolean silences) {
        return new MonstrousCondition("Praga Zumbi", attacker, 3, true,
                silences ? List.of(ConditionType.SILENCIO) : List.of());
    }

    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return name + " — " + (disease ? "Doença propagável" : String.join(", ", conditions.stream().map(Enum::name).toList()))
                + " por " + rounds + " Rodadas.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        if (disease && !target.isImmuneToCondition(ConditionType.DOENTE)) {
            target.applyCondition(new Disease(rounds, attacker, true, List.of()));
        }
        for (ConditionType type : conditions) {
            if (!target.isImmuneToCondition(type)) {
                target.applyCondition(new Condition(type, rounds, attacker));
            }
        }
        return reportChain(InteractionResult.builder().resultStatus(resolveStatus(target))).build();
    }
}
