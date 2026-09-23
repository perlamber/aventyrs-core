package org.aventyrs.core.effect;

import lombok.NonNull;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.util.DiceRoller;

/**
 * What an Efeito Crítico built from its {@link CriticalEffectType} needs beyond the target it lands
 * on — several act on <em>the attacker</em> (Guilhotina's Vantagem, Oferenda Maldita's Roubo de
 * Vida, Prevenir's Defesas), some name the weapon (Empalar), and a dozen throw dice.
 *
 * <p><b>Severity is normalised here.</b> On the {@code AttackReceiver} path the attacker's critical
 * is the defender's own Falha Crítica, so a {@code FALHA_CRITICA_MAIOR} means the same Maior
 * effect an {@code ACERTO_CRITICO_MAIOR} does on the {@code AttackDelivery} path; {@link #of}
 * maps both onto the Acerto, which is what every effect class validates.
 *
 * @param attacker       who landed the critical — {@code null} when the caller didn't say, which
 *                       leaves an attacker-side benefit ungranted rather than misdirected
 * @param attackSource   the weapon or Magia it was made with, or {@code null}
 * @param criticalResult always {@code ACERTO_CRITICO_MAIOR} or {@code ACERTO_CRITICO_MENOR}
 * @param dice           where the effect's own dice come from — {@code null} leaves every
 *                       dice-bearing effect unbuilt (see {@code CriticalEffects#requiresDice})
 */
public record CriticalEffectContext(CombatantSheet attacker,
                                    AttackSource attackSource,
                                    @NonNull CriticalResult criticalResult,
                                    DiceRoller dice) {

    public CriticalEffectContext {
        CriticalEffect.validateCriticalHit(criticalResult);
    }

    /**
     * A context for critical, whichever side of the exchange rolled it — a Falha Crítica is read as
     * the Acerto of the same severity.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException if critical is no critical at all
     */
    public static CriticalEffectContext of(final CombatantSheet attacker, final AttackSource attackSource,
                                           final CriticalResult critical, final DiceRoller dice) {
        CriticalResult normalised = switch (critical) {
            case FALHA_CRITICA_MAIOR -> CriticalResult.ACERTO_CRITICO_MAIOR;
            case FALHA_CRITICA_MENOR -> CriticalResult.ACERTO_CRITICO_MENOR;
            default -> critical;
        };
        return new CriticalEffectContext(attacker, attackSource, normalised, dice);
    }

    /** Whether this is a Maior critical. */
    public boolean isMajor() {
        return criticalResult == CriticalResult.ACERTO_CRITICO_MAIOR;
    }

    /** Maior's figure or Menor's. */
    public int pick(final int major, final int minor) {
        return isMajor() ? major : minor;
    }
}
