package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Map;

public class InitiativeServiceImpl implements InitiativeService {

    private final ModifierResolver modifierResolver;

    public InitiativeServiceImpl() {
        this(new ModifierResolverImpl());
    }

    public InitiativeServiceImpl(final ModifierResolver modifierResolver) {
        this.modifierResolver = modifierResolver;
    }

    @Override
    public int getTotalInitiative(final Character character) {
        int total = character.getEgos().getIniciativa().getTotal();
        total += modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.INITIATIVE);
        total += modifierResolver.sumModifiers(character.getSkillCompetencyAbilities(), ModifierType.INITIATIVE);
        for (Map.Entry<SkillType, CharacterSkill> entry : character.getSkills().entrySet()) {
            int graduationValue = entry.getValue().getGraduation().getGraduationValue();
            List<SkillExcellency> unlockedExcellencies = SkillExcellency.unlockedBy(
                    entry.getKey().getExcellencyClass(), graduationValue);
            total += modifierResolver.sumModifiers(unlockedExcellencies, ModifierType.INITIATIVE);
        }
        return total;
    }
}
