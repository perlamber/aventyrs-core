package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

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
        int newIndex = Math.clamp(ordinal() + steps, 0, levels.length - 1);
        return levels[newIndex];
    }

    public DifficultyLevel easier(int steps) {
        return shift(-steps);
    }

    public DifficultyLevel harder(int steps) {
        return shift(steps);
    }
}
