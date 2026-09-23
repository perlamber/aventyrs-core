package org.aventyrs.core.effect;

import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.HalvedHealing;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TemporaryBonus;

/**
 * Ferida Profunda — the target loses 3 (Maior) / 2 (Menor) Multiplicadores de PV ("não cumulativo":
 * one sourced {@code LIFE_MULTIPLIER} bonus, which a second Ferida Profunda replaces). Maior adds
 * {@link ConditionType#FERIDAS_DOLOROSAS} ("não pode ser alvo de efeitos de cura até o final da
 * Cena"); Menor adds a cumulative {@code HalvedHealing} for 2 Rodadas.
 *
 * <p>⚠️ A reading: the rules text gives the lost Multiplicador no Duração of its own. It is held until
 * the combat ends ({@code CombatantSheet#applyEffectUntilCombatEnds}), the same span the Maior's
 * healing ban names.
 */
public class FeridaProfunda extends AbstractCriticalEffect {

    static final int MAJOR_MULTIPLIER_LOSS = 3;
    static final int MINOR_MULTIPLIER_LOSS = 2;
    static final int HALVED_HEALING_ROUNDS = 2;


    public FeridaProfunda(final CriticalEffectContext context) {
        super(context);
    }

    @Override
    public CriticalEffectType getType() {
        return CriticalEffectType.FERIDA_PROFUNDA;
    }

    @Override
    protected String majorDescription() {
        return "Alvo perde 3 Multiplicadores de PV (não cumulativo) e não pode ser alvo de efeitos de cura até o final da Cena.";
    }

    @Override
    protected String minorDescription() {
        return "Alvo perde 2 Multiplicador de PV (não cumulativo). Efeitos de cura são reduzidos à metade por 2 Rodadas (cumulativo).";
    }

    @Override
    protected InteractionResult.InteractionResultBuilder resolve(final CombatantSheet target) {
        target.applyEffectUntilCombatEnds(TemporaryBonus.openEnded(ModifierType.LIFE_MULTIPLIER,
                -pick(MAJOR_MULTIPLIER_LOSS, MINOR_MULTIPLIER_LOSS), getType().name()));
        if (getContext().isMajor()) {
            // Open-ended and combat-scoped: "até o final da Cena". Replaces any earlier one.
            target.removeCondition(ConditionType.FERIDAS_DOLOROSAS);
            target.applyEffectUntilCombatEnds(new Condition(ConditionType.FERIDAS_DOLOROSAS, null, attacker()));
        } else {
            target.applyEffect(new HalvedHealing(HALVED_HEALING_ROUNDS));
        }
        return InteractionResult.builder();
    }
}
