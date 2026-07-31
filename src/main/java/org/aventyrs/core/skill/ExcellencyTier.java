package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The three universal Excelência tiers: every Perícia grants its own bonus automatically
 * once a character's graduation in that Perícia reaches the threshold — same tier names and
 * thresholds for every skill; only the bonus content differs per skill (see
 * {@link SkillExcellency} implementations like {@link ArtesExcellency}).
 */
@Getter
@AllArgsConstructor
public enum ExcellencyTier {
    FOCADO(3),
    PRODIGIO(7),
    LENDA(10);

    private final int requiredGraduation;

    public boolean isUnlockedBy(int graduationValue) {
        return graduationValue >= requiredGraduation;
    }

    /** Every tier reached by the given graduation value, in ascending threshold order. */
    public static List<ExcellencyTier> unlockedBy(int graduationValue) {
        return Arrays.stream(values())
                .filter(tier -> tier.isUnlockedBy(graduationValue))
                .collect(Collectors.toList());
    }
}
