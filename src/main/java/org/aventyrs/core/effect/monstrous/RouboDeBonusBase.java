package org.aventyrs.core.effect.monstrous;

import lombok.NonNull;
import org.aventyrs.core.effect.AbstractEffect;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.effect.RouboDeDeterminacao;
import org.aventyrs.core.effect.RouboDeMana;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * The PM and PD halves of a Habilidade Monstruosa's "Roubo de Bônus Base N" (Dádiva de Gaea Maior's 2) — or of a
 * stated "Roubo de Mana N e Roubo de Determinação N" — riding the holder's attack as one Corrente. The PV half is the
 * holder's own {@code LifeSteal}, which {@code AttackDelivery} already chains.
 *
 * <p>Each steal is the existing stage ({@link RouboDeMana}/{@link RouboDeDeterminacao}): the target loses what it
 * has, the attacker regains what was taken.
 */
public class RouboDeBonusBase extends AbstractEffect implements EffectChain {

    private final CombatantSheet attacker;
    private final int mana;
    private final int determination;

    public RouboDeBonusBase(@NonNull final CombatantSheet attacker, final int mana, final int determination) {
        this.attacker = attacker;
        this.mana = mana;
        this.determination = determination;
    }

    @Override
    public String getDescription() {
        return "Roubo de Mana " + mana + " e Roubo de Determinação " + determination + ".";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        if (mana > 0) {
            new RouboDeMana(attacker, mana).applyTo(target);
        }
        if (determination > 0) {
            new RouboDeDeterminacao(attacker, determination).applyTo(target);
        }
        return reportChain(InteractionResult.builder().resultStatus(resolveStatus(target))).build();
    }
}
