package org.aventyrs.core.action;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Map;

public class ActionPointsServiceImpl implements ActionPointsService {

    private final ModifierResolver modifierResolver;

    public ActionPointsServiceImpl() {
        this(new ModifierResolverImpl());
    }

    public ActionPointsServiceImpl(final ModifierResolver modifierResolver) {
        this.modifierResolver = modifierResolver;
    }

    @Override
    public int getMaxActionPoints(final Character character, final int turnNumber) {
        int bonus = modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.ACTION_POINTS);
        bonus += modifierResolver.sumModifiers(character.getSkillCompetencyAbilities(), ModifierType.ACTION_POINTS);
        for (Map.Entry<SkillType, CharacterSkill> entry : character.getSkills().entrySet()) {
            int graduationValue = entry.getValue().getGraduation().getGraduationValue();
            List<SkillExcellency> unlockedExcellencies = SkillExcellency.unlockedBy(
                    entry.getKey().getExcellencyClass(), graduationValue);
            bonus += modifierResolver.sumModifiers(unlockedExcellencies, ModifierType.ACTION_POINTS);
        }
        int baseline = character.getActionPoints() + bonus + character.getTemporaryActionPointsBonus();
        int adjusted = character.getActionProfile().adjustActionPoints(baseline, turnNumber);
        return Math.max(0, adjusted);
    }

    @Override
    public int getSkillRollCost(final Character character, final int turnNumber) {
        int adjustment = modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.SKILL_ROLL_COST);
        int adjusted = character.getActionProfile()
                .adjustSkillRollCost(DEFAULT_SKILL_ROLL_COST + adjustment, turnNumber);
        return Math.max(0, adjusted);
    }

    @Override
    public boolean canAffordSkillRoll(final Character character, final int turnNumber) {
        return getMaxActionPoints(character, turnNumber) >= getSkillRollCost(character, turnNumber);
    }
}
