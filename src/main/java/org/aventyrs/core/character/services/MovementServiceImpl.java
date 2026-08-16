package org.aventyrs.core.character.services;

import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Map;

public class MovementServiceImpl implements MovementService {

    private final CharacterSizeService characterSizeService;
    private final ActionPointsService actionPointsService;
    private final ModifierResolver modifierResolver;

    public MovementServiceImpl() {
        this(new CharacterSizeServiceImpl(), new ActionPointsServiceImpl(), new ModifierResolverImpl());
    }

    public MovementServiceImpl(final CharacterSizeService characterSizeService,
                                final ActionPointsService actionPointsService,
                                final ModifierResolver modifierResolver) {
        this.characterSizeService = characterSizeService;
        this.actionPointsService = actionPointsService;
        this.modifierResolver = modifierResolver;
    }

    @Override
    public int getTotalMovement(final Character character, final int turnNumber) {
        SizeCategory effectiveSize = characterSizeService.getEffectiveSizeCategory(character);
        int actionPoints = actionPointsService.getMaxActionPoints(character, turnNumber);
        int total = effectiveSize.getMovementPerActionPoint() * actionPoints;

        List<SkillCompetencyAbility> skillCompetencyAbilities = SkillCompetencyAbility.allFor(character);
        total += modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.MOVEMENT);
        total += modifierResolver.sumModifiers(skillCompetencyAbilities, ModifierType.MOVEMENT);
        for (Map.Entry<SkillType, CharacterSkill> entry : character.getSkills().entrySet()) {
            int graduationValue = entry.getValue().getGraduation().getGraduationValue();
            List<SkillExcellency> unlockedExcellencies = SkillExcellency.unlockedBy(
                    entry.getKey().getExcellencyClass(), graduationValue);
            total += modifierResolver.sumModifiers(unlockedExcellencies, ModifierType.MOVEMENT);
        }
        return Math.max(0, total);
    }
}
