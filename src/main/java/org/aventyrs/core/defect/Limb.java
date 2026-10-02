package org.aventyrs.core.defect;

import org.aventyrs.core.skill.SkillType;

import java.util.EnumSet;
import java.util.Set;

/**
 * Deficiência Física's "Escolha entre pernas ou braços" — and which Perícias depend on each (a table
 * ruling): braços for the two attack Perícias, pernas for Esquiva e Aparar and Furtividade, both for
 * Atletismo and Dirigir e Cavalgar; the rest depend on neither.
 */
public enum Limb {
    BRACOS(EnumSet.of(SkillType.ATAQUE_A_DISTANCIA, SkillType.ATAQUE_CORPO_A_CORPO,
            SkillType.ATLETISMO, SkillType.DIRIGIR_E_CAVALGAR)),
    PERNAS(EnumSet.of(SkillType.ESQUIVA_E_APARAR, SkillType.FURTIVIDADE,
            SkillType.ATLETISMO, SkillType.DIRIGIR_E_CAVALGAR));

    private final Set<SkillType> dependentSkills;

    Limb(final Set<SkillType> dependentSkills) {
        this.dependentSkills = Set.copyOf(dependentSkills);
    }

    /** The Perícias a roll of which "depends on" this limb. */
    public Set<SkillType> getDependentSkills() {
        return dependentSkills;
    }
}
