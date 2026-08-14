package org.aventyrs.core.effect;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.ego.AutocontroleAdvantage;
import org.aventyrs.core.skill.DifficultyLevel;

import java.util.Optional;

/**
 * Whether a Corrente de Efeitos actually hits/affects a target — the roll that
 * triggered it must surpass a required challenge number by a margin: {@link
 * #BASE_REQUIRED_MARGIN} normally, {@link #RESOLUTO_REQUIRED_MARGIN} if the target
 * holds {@link AutocontroleAdvantage#RESOLUTO}. No concrete {@link EffectChain}
 * implementation calls this yet (see {@code org.aventyrs.core.effect} package-info) —
 * this is the real, tested margin math RESOLUTO's own rules text describes, ready for
 * the first concrete EffectChain that needs it.
 */
public interface EffectChainService {
    int BASE_REQUIRED_MARGIN = 5;
    int RESOLUTO_REQUIRED_MARGIN = 7;

    /**
     * {@link #RESOLUTO_REQUIRED_MARGIN} when {@code target} holds {@link
     * AutocontroleAdvantage#RESOLUTO}, {@link #BASE_REQUIRED_MARGIN} otherwise.
     */
    int getRequiredMargin(Character target);

    /**
     * {@code challengeLevel}, if present, is shifted easier by {@code
     * difficultyReduction} steps and its {@link DifficultyLevel#getBaseValue()} taken;
     * {@code variableBonus} is then added on top of that (already-reduced) value.
     * {@code challengeLevel} absent means the required challenge number is just {@code
     * variableBonus} alone — {@link DifficultyLevel}'s 8 tiers are coarse, so this is
     * what lets a caller land on an exact target number rather than being stuck on one
     * of those 8 values.
     */
    int getRequiredChallengeNumber(Optional<DifficultyLevel> challengeLevel, int difficultyReduction, int variableBonus);

    /**
     * Whether {@code attackRollTotal} clears {@link #getRequiredChallengeNumber} by at
     * least {@link #getRequiredMargin(Character)} — reaching the margin exactly counts
     * as surpassing it.
     */
    boolean hits(int attackRollTotal, Character target, Optional<DifficultyLevel> challengeLevel,
                 int difficultyReduction, int variableBonus);
}
