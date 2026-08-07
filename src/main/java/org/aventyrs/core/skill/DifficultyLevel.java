package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;
import java.util.function.ToIntFunction;

@Getter
@AllArgsConstructor
public enum DifficultyLevel {

    VERY_EASY(12, 11),
    EASY(14, 13),
    MEDIUM(18, 16),
    HARD(23, 20),
    VERY_HARD(28, 25),
    UNLIKELY(36, 32),
    UNIMAGINABLE(45, 40),
    MIRACLE(60, 49);

    private final int baseValue;
    private final int expertValue;

    public DifficultyLevel shift(int steps) {
        DifficultyLevel[] levels = values();
        int newIndex = Math.max(0, Math.min(ordinal() + steps, levels.length - 1));
        return levels[newIndex];
    }

    public DifficultyLevel easier(int steps) {
        return shift(-steps);
    }

    public DifficultyLevel harder(int steps) {
        return shift(steps);
    }

    /**
     * The highest tier a roll totaling total reaches, judged against {@link #getBaseValue()}
     * — or empty if total falls short of even {@link #VERY_EASY}. Deliberately doesn't
     * consider {@link #getExpertValue()} (the easier threshold a matching Especialização
     * grants) — see {@link #reachedByAsExpert(int)} for the roll that's requesting a held
     * {@code SkillSpecialization} (resolved by {@code AbstractSkillInteraction} via {@code
     * SkillRoll#getRequestedAbility()}).
     */
    public static Optional<DifficultyLevel> reachedBy(final int total) {
        return reachedBy(total, DifficultyLevel::getBaseValue);
    }

    /**
     * Same as {@link #reachedBy(int)}, but judged against {@link #getExpertValue()} instead of
     * {@link #getBaseValue()} — the easier threshold a matching, held Especialização grants.
     * {@code org.aventyrs.core.skill.AbstractSkillInteraction} calls this instead of {@link
     * #reachedBy(int)} once it has confirmed the character actually holds the {@code
     * SkillSpecialization} named by {@code SkillRoll#getRequestedAbility()} — this method
     * itself doesn't re-check that; it's purely the threshold-lookup half.
     */
    public static Optional<DifficultyLevel> reachedByAsExpert(final int total) {
        return reachedBy(total, DifficultyLevel::getExpertValue);
    }

    private static Optional<DifficultyLevel> reachedBy(final int total, final ToIntFunction<DifficultyLevel> threshold) {
        DifficultyLevel reached = null;
        for (DifficultyLevel level : values()) {
            if (total < threshold.applyAsInt(level)) {
                break;
            }
            reached = level;
        }
        return Optional.ofNullable(reached);
    }
}
