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

public class FreeActionsServiceImpl implements FreeActionsService {

    private final ModifierResolver modifierResolver;

    public FreeActionsServiceImpl() {
        this(new ModifierResolverImpl());
    }

    public FreeActionsServiceImpl(final ModifierResolver modifierResolver) {
        this.modifierResolver = modifierResolver;
    }

    @Override
    public int getTotalFreeActions(final Character character) {
        int total = character.getFreeActions();
        total += modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.FREE_ACTIONS);
        total += modifierResolver.sumModifiers(character.getSkillCompetencyAbilities(), ModifierType.FREE_ACTIONS);
        for (Map.Entry<SkillType, CharacterSkill> entry : character.getSkills().entrySet()) {
            int graduationValue = entry.getValue().getGraduation().getGraduationValue();
            List<SkillExcellency> unlockedExcellencies = SkillExcellency.unlockedBy(
                    entry.getKey().getExcellencyClass(), graduationValue);
            total += modifierResolver.sumModifiers(unlockedExcellencies, ModifierType.FREE_ACTIONS);
        }
        return Math.max(0, total);
    }
}
