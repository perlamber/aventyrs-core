package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.AttackerGuard;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * Prevenir — the <b>attacker</b> "Recebe Bônus de +5 / +2 em suas Defesas para evitar ataques do alvo
 * por 2 Rodadas": an {@code sheet.AttackerGuard} on the attacker against this target.
 *
 * <p>TODO "Se o alvo for um objeto ou magia": this core targets combatants only.
 */
public class Prevenir extends AbstractCriticalEffect {

    static final int ROUNDS = 2;


    public Prevenir(final CriticalEffectContext context) {
        super(context);
    }

    @Override
    public CriticalEffectType getType() {
        return CriticalEffectType.PREVENIR;
    }

    @Override
    protected String majorDescription() {
        return "Recebe Bônus de +5 em suas Defesas para evitar ataques do alvo por 2 Rodadas.";
    }

    @Override
    protected String minorDescription() {
        return "Recebe Bônus de +2 em suas Defesas para evitar ataques do alvo por 2 Rodadas.";
    }

    @Override
    protected InteractionResult.InteractionResultBuilder resolve(final CombatantSheet target) {
        if (attacker() != null) {
            attacker().applyEffect(AttackerGuard.defesas(target, pick(5, 2), ROUNDS));
        }
        return InteractionResult.builder();
    }
}
