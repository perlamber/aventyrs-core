package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;

import java.util.Optional;

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

    /**
     * The Attribute this ability unconditionally lets its own Perícia use instead of that
     * Perícia's normal base Attribute — e.g. {@code AtaqueCorpoACorpoCompetencyAbility
     * .ACUIDADE} substituting Destreza for Ataque Corpo-a-Corpo's normal Força. Empty by
     * default; only override on a constant whose rules text grants the substitution
     * unconditionally. A substitution that's scoped to a specific circumstance (e.g.
     * {@code AtaqueADistanciaCompetencyAbility.ARREMESSO_PODEROSO}, only for thrown-weapon/
     * spell attacks) can't be modeled this way yet — this codebase doesn't track what a roll
     * is *for*, the same simplification already applied to scoped Vantagem bonuses (see
     * CLAUDE.md's "Vantagem is a flat +2 bonus" section); document that gap in a TODO on the
     * constant instead of over- or under-granting here.
     */
    default Optional<AttributeDomain> getSubstituteAttributeDomain() {
        return Optional.empty();
    }
}
