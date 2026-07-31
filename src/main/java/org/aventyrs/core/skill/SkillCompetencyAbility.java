package org.aventyrs.core.skill;

public interface SkillCompetencyAbility {
    SkillType getSkillType();
    String getDescription();

    /**
     * How many GD (DifficultyLevel) steps this Habilidade de Competência reduces a Perícia
     * roll's difficulty by — e.g. Atletismo's Atleta Versátil. Mirrors
     * {@link SkillExcellency#getDifficultyReduction()}; the default is 0, only abilities with
     * a concrete difficulty effect override it.
     */
    default int getDifficultyReduction() {
        return 0;
    }
}
