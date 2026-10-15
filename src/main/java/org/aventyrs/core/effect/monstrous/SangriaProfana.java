package org.aventyrs.core.effect.monstrous;

import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.effect.AbstractEffect;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.effect.RouboDeDeterminacao;
import org.aventyrs.core.sheet.Bleeding;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.DeterminationDrain;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.SourcedState;

import java.util.Optional;

/**
 * Sangria Profana — "Alvo perde 2PV e 1PD a cada Turno por Vigor Rodadas, este é um efeito de Sangramento. Espectrais
 * recebem Roubo de Determinação 2 contra alvos afetados pela Sangria Profana" ({@code
 * EscolhidoDeMacariaAbility#ESPECTRAL}'s Garras Espectrais, core 0.1.7.3).
 *
 * <p>The loss is a {@link Bleeding} and a {@link DeterminationDrain} for the target's Vigor in Rodadas; a {@link
 * SourcedState} of the same Duração marks the target so {@link #roubo} can tell it is bleeding.
 */
public class SangriaProfana extends AbstractEffect implements EffectChain {

    public static final String SOURCE = "Sangria Profana";

    @Override
    public String getDescription() {
        return "Alvo perde 2PV e 1PD a cada Turno por Vigor Rodadas, este é um efeito de Sangramento.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        int rounds = Math.max(1, target.getAttributeTotal(AttributeDomain.VIGOR));
        target.applyEffect(new Bleeding(2, Optional.of(rounds)));
        target.applyEffect(new DeterminationDrain(1, rounds));
        target.applyEffect(new SourcedState(SOURCE, rounds));
        return reportChain(InteractionResult.builder().resultStatus(resolveStatus(target))).build();
    }

    /** "Espectrais recebem Roubo de Determinação 2 contra alvos afetados pela Sangria Profana" — judged on landing. */
    public static EffectChain roubo(@NonNull final CombatantSheet attacker) {
        return new Roubo(attacker);
    }

    private static final class Roubo extends AbstractEffect implements EffectChain {

        private final CombatantSheet attacker;

        private Roubo(final CombatantSheet attacker) {
            this.attacker = attacker;
        }

        @Override
        public String getDescription() {
            return "Contra alvos afetados pela Sangria Profana: Roubo de Determinação 2.";
        }

        @Override
        public InteractionResult applyTo(final CombatantSheet target) {
            if (target.hasEffectFrom(SOURCE)) {
                new RouboDeDeterminacao(attacker, 2).applyTo(target);
            }
            return reportChain(InteractionResult.builder().resultStatus(resolveStatus(target))).build();
        }
    }
}
