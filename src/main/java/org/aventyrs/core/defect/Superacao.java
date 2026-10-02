package org.aventyrs.core.defect;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.SkillTrait;

/** Small reads shared by the Defeito/Qualidade choices and their validation. */
final class Superacao {

    private Superacao() {
    }

    static boolean holds(final Character character, final SkillTrait trait) {
        if (trait instanceof SkillSpecialization specialization) {
            return character.getSpecializations(specialization.getSkillType()).contains(specialization);
        }
        return SkillCompetencyAbility.allFor(character).contains(trait);
    }
}
