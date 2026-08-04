package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

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
     * grants): resolving "does this roll's Especialização match what it's being used for" is
     * a separate, still-unbuilt concern (this core doesn't track what a roll is *for* — same
     * gap documented for scoped Vantagem/substitution elsewhere) — a caller who *has* already
     * resolved that externally can still compare against {@code getExpertValue()} directly
     * instead of calling this method.
     */
    public static Optional<DifficultyLevel> reachedBy(final int total) {
        DifficultyLevel reached = null;
        for (DifficultyLevel level : values()) {
            if (total < level.baseValue) {
                break;
            }
            reached = level;
        }
        return Optional.ofNullable(reached);
    }
}
