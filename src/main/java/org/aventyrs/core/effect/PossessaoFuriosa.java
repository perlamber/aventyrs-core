package org.aventyrs.core.effect;

import lombok.Getter;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * Possessão Furiosa — the Abantesma's Corrente ({@code ReanimarSpell#INVOCAR_ABANTESMA}, core 0.1.6.25): "O alvo é
 * possuído (efeito de possessão) por 2 Rodadas e sempre ataca o aliado mais próximo." Applied as {@link
 * ConditionType#POSSESSAO}, whose own text carries the rest (no Magias but Raciais, no Habilidades Aventyr).
 */
@Getter
public class PossessaoFuriosa extends AbstractEffect implements EffectChain {

    /** "por 2 Rodadas". */
    public static final int ROUNDS = 2;

    /** The Abantesma possessing the target — the Condição's source; may be {@code null}. */
    private final CombatantSheet attacker;

    public PossessaoFuriosa(final CombatantSheet attacker) {
        this.attacker = attacker;
    }

    @Override
    public String getDescription() {
        return "O alvo é possuído por 2 Rodadas e sempre ataca o aliado mais próximo.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        target.applyCondition(new Condition(ConditionType.POSSESSAO, ROUNDS, attacker));
        return reportChain(InteractionResult.builder()
                .resultStatus(resolveStatus(target)))
                .build();
    }
}
