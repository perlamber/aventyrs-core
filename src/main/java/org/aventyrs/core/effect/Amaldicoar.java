package org.aventyrs.core.effect;

import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TargetScope;

/**
 * Amaldiçoar — the target is {@link ConditionType#AMALDICOADO} for 1d6 Rodadas, and while it lasts
 * takes −5 (Maior) / −2 (Menor) to its Defesas, a sourced {@code DEFESAS} Blessing of the same
 * Duração. A target warded by Imunizar refuses the Maldição, and then takes no penalty either.
 */
public class Amaldicoar extends AbstractCriticalEffect {

    static final int MAJOR_DEFESAS_PENALTY = 5;
    static final int MINOR_DEFESAS_PENALTY = 2;


    public Amaldicoar(final CriticalEffectContext context) {
        super(context);
    }

    @Override
    public CriticalEffectType getType() {
        return CriticalEffectType.AMALDICOAR;
    }

    @Override
    protected String majorDescription() {
        return "Alvo recebe a condição Amaldiçoado por 1d6 rodadas, enquanto estiver amaldiçoado recebe Redutor de -5 em suas Defesas.";
    }

    @Override
    protected String minorDescription() {
        return "Alvo recebe a condição Amaldiçoado por 1d6 rodadas, enquanto estiver amaldiçoado recebe Redutor de -2 em suas Defesas.";
    }

    @Override
    protected InteractionResult.InteractionResultBuilder resolve(final CombatantSheet target) {
        int rounds = roll(1);
        target.applyCondition(new Condition(ConditionType.AMALDICOADO, rounds, attacker()));
        if (target.hasCondition(ConditionType.AMALDICOADO, null)) {
            target.grantBlessing(new Blessing(ModifierType.DEFESAS,
                    -pick(MAJOR_DEFESAS_PENALTY, MINOR_DEFESAS_PENALTY), rounds, TargetScope.SELF, getType().name()));
        }
        return InteractionResult.builder();
    }
}
