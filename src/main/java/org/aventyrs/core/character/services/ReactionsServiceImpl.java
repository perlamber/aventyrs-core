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

public class ReactionsServiceImpl implements ReactionsService {

    private final ModifierResolver modifierResolver;

    public ReactionsServiceImpl() {
        this(new ModifierResolverImpl());
    }

    public ReactionsServiceImpl(final ModifierResolver modifierResolver) {
        this.modifierResolver = modifierResolver;
    }

    @Override
    public int getTotalReactions(final Character character) {
        int total = character.getReactions();
        total += modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.REACTIONS);
        total += modifierResolver.sumModifiers(character.getSkillCompetencyAbilities(), ModifierType.REACTIONS);
        for (Map.Entry<SkillType, CharacterSkill> entry : character.getSkills().entrySet()) {
            int graduationValue = entry.getValue().getGraduation().getGraduationValue();
            List<SkillExcellency> unlockedExcellencies = SkillExcellency.unlockedBy(
                    entry.getKey().getExcellencyClass(), graduationValue);
            total += modifierResolver.sumModifiers(unlockedExcellencies, ModifierType.REACTIONS);
        }
        return Math.max(0, total);
    }
}
