package org.aventyrs.core.skill;

import java.util.Arrays;
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

    /**
     * How many Dano Base scale-ups this Excelência grants an attack made with its own Perícia —
     * e.g. {@code org.aventyrs.core.skill.ataqueadistancia.AtaqueADistanciaExcellency#FOCADO}'s
     * "Danos Base da Arma +1". Summed by {@code
     * org.aventyrs.core.character.services.DamageBaseService#getDamageBase} across only the
     * tiers the <em>attacking</em> Perícia's own Graduação has unlocked — unlike this codebase's
     * usual scan of every trained Perícia's tiers, because an Ataque à Distância Excelência must
     * not raise the Dano Base of a Corpo-a-Corpo swing. That scoping is why this needs no {@code
     * SkillType} parameter of its own, where {@code SkillCompetencyAbility#resolveDamageBaseIncrease}
     * does. Zero by default; only override on a constant whose rules text raises Dano Base.
     */
    default int resolveDamageBaseIncrease() {
        return 0;
    }

    /** Convenience: applies {@link #getDifficultyReduction()} to a concrete DifficultyLevel. */
    default DifficultyLevel adjustDifficulty(DifficultyLevel level) {
        return level.easier(getDifficultyReduction());
    }

    /**
     * Every constant of the given Excelência enum whose tier is reached by graduationValue.
     * Takes a plain {@code Class<? extends SkillExcellency>} (resolved via reflection through
     * {@link Class#getEnumConstants()}) rather than a generic {@code Enum} bound, so it also
     * works from a {@link SkillType}-resolved class whose concrete enum type isn't known at
     * compile time — see
     * {@code org.aventyrs.core.character.services.ReactionsServiceImpl#getTotalReactions}.
     */
    static List<SkillExcellency> unlockedBy(Class<? extends SkillExcellency> excellencyClass, int graduationValue) {
        return Arrays.stream(excellencyClass.getEnumConstants())
                .filter(excellency -> excellency.getTier().isUnlockedBy(graduationValue))
                .collect(Collectors.toList());
    }

    /**
     * Total GD reduction from every Excelência tier the given graduationValue has reached —
     * the sum of {@link #getDifficultyReduction()} across {@link #unlockedBy}.
     */
    static int totalDifficultyReduction(Class<? extends SkillExcellency> excellencyClass, int graduationValue) {
        return unlockedBy(excellencyClass, graduationValue).stream()
                .mapToInt(SkillExcellency::getDifficultyReduction)
                .sum();
    }
}
