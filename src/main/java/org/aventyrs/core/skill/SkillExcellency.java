package org.aventyrs.core.skill;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A Perícia's automatic Excelência bonus at one of the three universal
 * {@link ExcellencyTier} thresholds. Most bonus content needs systems that don't exist yet
 * and stays descriptive-only (see CLAUDE.md's Skill conventions); {@link #getDifficultyReduction}
 * is the one piece of an Excelência that can have a real, testable effect today, since
 * {@link DifficultyLevel} already supports easier/harder shifts.
 */
public interface SkillExcellency {
    SkillType getSkillType();
    ExcellencyTier getTier();
    String getDescription();

    /**
     * How many GD (DifficultyLevel) steps this Excelência reduces a Perícia roll's
     * difficulty by — e.g. Prodígio's -1 GD level. The default is 0; only Excelências with a
     * concrete difficulty effect override it.
     */
    default int getDifficultyReduction() {
        return 0;
    }

    /** Convenience: applies {@link #getDifficultyReduction()} to a concrete DifficultyLevel. */
    default DifficultyLevel adjustDifficulty(DifficultyLevel level) {
        return level.easier(getDifficultyReduction());
    }

    /** Every constant of the given Excelência enum whose tier is reached by graduationValue. */
    static <E extends Enum<E> & SkillExcellency> List<E> unlockedBy(Class<E> excellencyClass, int graduationValue) {
        return EnumSet.allOf(excellencyClass).stream()
                .filter(excellency -> excellency.getTier().isUnlockedBy(graduationValue))
                .collect(Collectors.toList());
    }

    /**
     * Total GD reduction from every Excelência tier the given graduationValue has reached —
     * the sum of {@link #getDifficultyReduction()} across {@link #unlockedBy}.
     */
    static <E extends Enum<E> & SkillExcellency> int totalDifficultyReduction(Class<E> excellencyClass, int graduationValue) {
        return unlockedBy(excellencyClass, graduationValue).stream()
                .mapToInt(SkillExcellency::getDifficultyReduction)
                .sum();
    }
}
