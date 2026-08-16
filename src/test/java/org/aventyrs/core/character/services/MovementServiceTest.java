package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MovementServiceTest {

    private final MovementService movementService = new MovementServiceImpl();

    private static class MovementBonusAbility implements AttributeAbility {
        @Override
        public AttributeDomain getAttributeDomain() {
            return AttributeDomain.DEXTERITY;
        }

        @Override
        public String getDescription() {
            return "Test-only +1 Movimento Base bonus source.";
        }

        @Modifier(ModifierType.MOVEMENT)
        public int bonus() {
            return 1;
        }
    }

    private static class MovementMalusAbility implements AttributeAbility {
        @Override
        public AttributeDomain getAttributeDomain() {
            return AttributeDomain.DEXTERITY;
        }

        @Override
        public String getDescription() {
            return "Test-only -100 Movimento Base malus source.";
        }

        @Modifier(ModifierType.MOVEMENT)
        public int malus() {
            return -100;
        }
    }

    private static class MovementBonusSkillCompetencyAbility implements SkillCompetencyAbility {
        @Override
        public SkillType getSkillType() {
            return SkillType.ATLETISMO;
        }

        @Override
        public String getDescription() {
            return "Test-only +1 Movimento Base bonus source.";
        }

        @Modifier(ModifierType.MOVEMENT)
        public int bonus() {
            return 1;
        }
    }

    private static class RaceWithMovementBonus extends Human {
        @Override
        public List<SkillCompetencyAbility> getRacialAbilities() {
            return List.of(new MovementBonusSkillCompetencyAbility());
        }
    }

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    @Test
    void baseMovementIsSizeMovementPerActionPointTimesMaxActionPoints() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        // SizeCategory.ZERO's 4UD/PA * ActionPointsService.DEFAULT_ACTION_POINTS(3) = 12.
        assertEquals(12, movementService.getTotalMovement(character, 0));
    }

    @Test
    void largerSizeCategoriesMoveFurtherPerActionPoint() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .sizeCategory(SizeCategory.PLUS_ONE)
                .build();
        // SizeCategory.PLUS_ONE's 5UD/PA * 3 = 15.
        assertEquals(15, movementService.getTotalMovement(character, 0));
    }

    @Test
    void attributeAbilityModifierIsAdded() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(new MovementBonusAbility())
                .build();
        assertEquals(13, movementService.getTotalMovement(character, 0));
    }

    @Test
    void skillCompetencyAbilityModifierIsAdded() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skillCompetencyAbility(new MovementBonusSkillCompetencyAbility())
                .build();
        assertEquals(13, movementService.getTotalMovement(character, 0));
    }

    @Test
    void racialAbilityModifierIsAdded() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .race(new RaceWithMovementBonus())
                .build();
        assertEquals(13, movementService.getTotalMovement(character, 0));
    }

    @Test
    void unlockedSkillExcellencyModifierIsAdded() {
        CharacterSkill ataqueCorpoACorpoSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        ataqueCorpoACorpoSkill.increaseGraduation(5); // unlocks AtaqueCorpoACorpoExcellency.FOCADO (+2UD).
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, ataqueCorpoACorpoSkill)
                .build();
        assertEquals(14, movementService.getTotalMovement(character, 0));
    }

    @Test
    void neverNegative() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(new MovementMalusAbility())
                .build();
        assertEquals(0, movementService.getTotalMovement(character, 0));
    }
}
