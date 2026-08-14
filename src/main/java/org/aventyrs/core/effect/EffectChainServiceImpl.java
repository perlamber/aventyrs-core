package org.aventyrs.core.effect;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.ego.AutocontroleAdvantage;
import org.aventyrs.core.skill.DifficultyLevel;

import java.util.Optional;

public class EffectChainServiceImpl implements EffectChainService {

    @Override
    public int getRequiredMargin(final Character target) {
        return target.getAutocontroleAdvantage() == AutocontroleAdvantage.RESOLUTO
                ? RESOLUTO_REQUIRED_MARGIN : BASE_REQUIRED_MARGIN;
    }

    @Override
    public int getRequiredChallengeNumber(final Optional<DifficultyLevel> challengeLevel, final int difficultyReduction,
                                           final int variableBonus) {
        int baseValue = challengeLevel
                .map(level -> level.easier(difficultyReduction).getBaseValue())
                .orElse(0);
        return baseValue + variableBonus;
    }

    @Override
    public boolean hits(final int attackRollTotal, final Character target, final Optional<DifficultyLevel> challengeLevel,
                         final int difficultyReduction, final int variableBonus) {
        int requiredNumber = getRequiredChallengeNumber(challengeLevel, difficultyReduction, variableBonus);
        return attackRollTotal >= requiredNumber + getRequiredMargin(target);
    }
}
