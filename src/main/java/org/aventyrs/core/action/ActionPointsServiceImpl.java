package org.aventyrs.core.action;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;

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
        int adjusted = character.getActionProfile().adjustActionPoints(character.getActionPoints() + bonus, turnNumber);
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
