package org.aventyrs.core.effect;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.ego.AutocontroleAdvantage;
import org.aventyrs.core.skill.DifficultyLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectChainServiceImplTest {

    private final EffectChainService effectChainService = new EffectChainServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void requiredMarginIsFiveWithNoAutocontroleAdvantage() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        assertEquals(5, effectChainService.getRequiredMargin(character));
    }

    @Test
    void requiredMarginIsFiveWithADifferentAutocontroleAdvantage() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .egoAdvantage(EgoDomain.AUTOCONTROLE, AutocontroleAdvantage.DETERMINACAO_HEROICA)
                .build();
        assertEquals(5, effectChainService.getRequiredMargin(character));
    }

    @Test
    void requiredMarginIsSevenWithResoluto() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .egoAdvantage(EgoDomain.AUTOCONTROLE, AutocontroleAdvantage.RESOLUTO)
                .build();
        assertEquals(7, effectChainService.getRequiredMargin(character));
    }

    @Test
    void requiredChallengeNumberWithNoChallengeLevelIsJustTheVariableBonus() {
        int requiredNumber = effectChainService.getRequiredChallengeNumber(Optional.empty(), 0, 4);
        assertEquals(4, requiredNumber);
    }

    @Test
    void requiredChallengeNumberWithAChallengeLevelAndNoReductionOrBonusIsItsBaseValue() {
        int requiredNumber = effectChainService.getRequiredChallengeNumber(Optional.of(DifficultyLevel.MEDIUM), 0, 0);
        assertEquals(DifficultyLevel.MEDIUM.getBaseValue(), requiredNumber);
    }

    @Test
    void requiredChallengeNumberAppliesReductionBeforeAddingTheVariableBonus() {
        int requiredNumber = effectChainService.getRequiredChallengeNumber(Optional.of(DifficultyLevel.MEDIUM), 1, 3);

        assertEquals(DifficultyLevel.EASY.getBaseValue() + 3, requiredNumber);
    }

    @Test
    void hitsIsTrueWhenTheAttackRollExactlyMeetsTheRequiredMargin() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        int requiredNumber = effectChainService.getRequiredChallengeNumber(Optional.of(DifficultyLevel.MEDIUM), 0, 0);

        assertTrue(effectChainService.hits(requiredNumber + 5, character, Optional.of(DifficultyLevel.MEDIUM), 0, 0));
    }

    @Test
    void hitsIsFalseWhenTheAttackRollFallsOneShortOfTheRequiredMargin() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        int requiredNumber = effectChainService.getRequiredChallengeNumber(Optional.of(DifficultyLevel.MEDIUM), 0, 0);

        assertFalse(effectChainService.hits(requiredNumber + 4, character, Optional.of(DifficultyLevel.MEDIUM), 0, 0));
    }

    @Test
    void hitsUsesTheHigherResolutoMarginWhenTheTargetHoldsIt() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .egoAdvantage(EgoDomain.AUTOCONTROLE, AutocontroleAdvantage.RESOLUTO)
                .build();
        int requiredNumber = effectChainService.getRequiredChallengeNumber(Optional.of(DifficultyLevel.MEDIUM), 0, 0);

        assertFalse(effectChainService.hits(requiredNumber + 5, character, Optional.of(DifficultyLevel.MEDIUM), 0, 0));
        assertTrue(effectChainService.hits(requiredNumber + 7, character, Optional.of(DifficultyLevel.MEDIUM), 0, 0));
    }
}
