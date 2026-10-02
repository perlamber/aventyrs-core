package org.aventyrs.core.sheet;

import org.aventyrs.core.effect.CriticalEffect;

/**
 * What "tornar-se imune a ele ao longo da Cena" is immune <em>to</em> — Autocontrole's permanent point (core 0.0.81;
 * table ruling, 2026-09-30: "that same kind"). One key per kind of thing that can land on a combatant:
 * <ul>
 *   <li>a {@link Condition} → its {@link ConditionType} (Envenenado, Atordoado…);</li>
 *   <li>an Efeito Crítico → its {@code CriticalEffectType} (Sangramento, Empalar…);</li>
 *   <li>anything else — a Corrente's stage, a timed effect — → its concrete class.</li>
 * </ul>
 * Held per sheet by {@link CombatantSheet#grantCenaImmunity} and dropped with the Cena.
 */
public final class CenaImmunity {

    private CenaImmunity() {
    }

    /** The key effect is immune under — see this class's javadoc. */
    public static Object kindOf(final Object effect) {
        if (effect instanceof Condition condition) {
            return condition.getType();
        }
        if (effect instanceof CriticalEffect critical) {
            return critical.getType();
        }
        return effect.getClass();
    }
}
