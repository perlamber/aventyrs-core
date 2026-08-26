package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.CriticalResult;

import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.CRITICAL_EFFECT_REQUIRES_A_CRITICAL_HIT;

/**
 * An Efeito Crítico — a secondary effect specifically triggered by a critical roll
 * outcome (see {@link CriticalResult}), e.g. {@code
 * org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoCompetencyAbility
 * #ABRIR_DEFESAS}'s Malefício Desprevenido on a critical hit. Still just a marker on top
 * of {@link Effect} beyond {@link #validateCriticalHit} below — nothing here should be
 * inferred beyond "this is an Effect that is an Efeito Crítico": no critical-detection
 * wiring, no Malefício/status-effect tracking, no duration/Rodada tracking beyond what a
 * concrete implementation builds for itself (see {@code Sangramento}/{@code ManaPurge}).
 * See {@code org.aventyrs.core.effect} package-info for the pipeline this fits into.
 */
public interface CriticalEffect extends Effect {

    /**
     * Which Efeito Crítico this is. Needed because a creature's immunities are authored as
     * {@link CriticalEffectType}s rather than as classes — see that enum's own javadoc for why.
     */
    CriticalEffectType getType();

    /**
     * effects minus every one target is immune to — the single place an Efeito Crítico is
     * filtered out before it can be applied.
     *
     * <h2>Why it lives here and not in either attack entry point</h2>
     *
     * {@code org.aventyrs.core.combat.AttackDelivery} and {@code
     * org.aventyrs.core.combat.AttackReceiver} are mirrored halves of one exchange, and an
     * immunity is a fact about the <i>victim</i>, identical whichever half is running. Putting
     * the filter in one of them would leave the other direction wrong, and putting it in both
     * would be the same rule written twice.
     *
     * <p>That symmetry is not hypothetical. A summoned creature (see {@code
     * org.aventyrs.core.monster.SummonedMonsterTemplate}) attacks on its summoner's roll, so a
     * player can end up driving <i>either</i> direction of an exchange against a foe whose
     * anatomy resists part of what lands. Both entry points route their {@code criticalEffects}
     * through here.
     *
     * <p>Filtering, not throwing: a caller assembling an attack has no obligation to know what
     * its target resists, and an attack that crits against a Zumbi is still a critical hit —
     * it simply produces a shorter chain. Order is preserved, and an empty result is normal.
     */
    static List<CriticalEffect> applicableTo(final CombatantSheet target,
                                             final List<CriticalEffect> effects) {
        if (target == null || effects == null || effects.isEmpty()) {
            return effects == null ? List.of() : effects;
        }
        return effects.stream()
                .filter(effect -> !target.getCriticalEffectImmunities().contains(effect.getType()))
                .toList();
    }

    /**
     * Every concrete CriticalEffect built so far (e.g. {@code Sangramento}, {@code
     * ManaPurge}) is only ever a *consequence* of a critical hit landing, never something
     * a caller applies on its own judgment — each rejects any {@code criticalResult}
     * that isn't {@link CriticalResult#ACERTO_CRITICO_MAIOR}/{@link
     * CriticalResult#ACERTO_CRITICO_MENOR} at construction. Shared here once two
     * concrete implementations needed the identical check, the same "validate
     * possession/legitimacy up front rather than compute a result for an illegitimate
     * case" discipline {@code AbstractSkillInteraction#validateRequestedTrait} already
     * applies to a requested Habilidade/Especialização.
     * @throws IllegalOperationException if criticalResult isn't an Acerto Crítico
     */
    static void validateCriticalHit(final CriticalResult criticalResult) {
        if (criticalResult != CriticalResult.ACERTO_CRITICO_MAIOR && criticalResult != CriticalResult.ACERTO_CRITICO_MENOR) {
            throw new IllegalOperationException(CRITICAL_EFFECT_REQUIRES_A_CRITICAL_HIT);
        }
    }
}
