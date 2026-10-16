package org.aventyrs.core.effect.monstrous;

import lombok.NonNull;
import org.aventyrs.core.effect.AbstractEffect;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.InteractionResult;

import java.util.Optional;

/**
 * Terror da Forma Verdadeira — "Alvos deste Ataque recebem a Condição Assustado por 2 Rodadas, novas aplicações desta
 * Corrente de Efeitos aumenta a Duração em +1 Rodada e progredem a Condição para Apavorado" ({@code
 * AspectoHumanoideAbility#FORMA_VERDADEIRA}, 20261015).
 *
 * <p>A target already frightened by this same monster (Assustado or Apavorado from it) is moved to Apavorado for what
 * its fear had left plus 1 Rodada; anyone else becomes Assustado for 2. The fear's origin is the monster, so its
 * range bands measure from it.
 */
public class TerrorDaFormaVerdadeira extends AbstractEffect implements EffectChain {

    /** "Assustado por 2 Rodadas". */
    public static final int ROUNDS = 2;

    private final CombatantSheet attacker;

    public TerrorDaFormaVerdadeira(@NonNull final CombatantSheet attacker) {
        this.attacker = attacker;
    }

    @Override
    public String getDescription() {
        return "Alvos deste Ataque recebem a Condição Assustado por 2 Rodadas, novas aplicações desta Corrente de "
                + "Efeitos aumenta a Duração em +1 Rodada e progredem a Condição para Apavorado.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        Optional<Condition> standing = target.getHeldConditions().stream()
                .filter(condition -> condition.getSource() == attacker)
                .filter(condition -> condition.getType() == ConditionType.ASSUSTADO
                        || condition.getType() == ConditionType.APAVORADO)
                .findFirst();
        if (standing.isPresent()) {
            Integer left = standing.get().getRemainingRounds();
            int rounds = (left == null ? ROUNDS : left) + 1;
            target.removeCondition(standing.get().getType());
            target.applyCondition(new Condition(ConditionType.APAVORADO, rounds, attacker));
        } else {
            target.applyCondition(new Condition(ConditionType.ASSUSTADO, ROUNDS, attacker));
        }
        return reportChain(InteractionResult.builder().resultStatus(resolveStatus(target))).build();
    }
}
