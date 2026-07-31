package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FreeActionsServiceImplTest {

    private final FreeActionsService freeActionsService = new FreeActionsServiceImpl();

    private static class FreeActionsBonusAbility implements AttributeAbility {
        @Override
        public AttributeDomain getAttributeDomain() {
            return AttributeDomain.STRENGTH;
        }

        @Override
        public String getDescription() {
            return "Test-only +1 Ações Livres bonus source.";
        }

        @Modifier(ModifierType.FREE_ACTIONS)
        public int bonus() {
            return 1;
        }
    }

    private static class FreeActionsBonusSkillCompetencyAbility implements SkillCompetencyAbility {
        @Override
        public SkillType getSkillType() {
            return SkillType.ATLETISMO;
        }

        @Override
        public String getDescription() {
            return "Test-only +1 Ações Livres bonus source.";
        }

        @Modifier(ModifierType.FREE_ACTIONS)
        public int bonus() {
            return 1;
        }
    }

    private static class FreeActionsMalusSkillCompetencyAbility implements SkillCompetencyAbility {
        @Override
        public SkillType getSkillType() {
            return SkillType.ATLETISMO;
        }

        @Override
        public String getDescription() {
            return "Test-only -10 Ações Livres malus source.";
        }

        @Modifier(ModifierType.FREE_ACTIONS)
        public int malus() {
            return -10;
        }
    }

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    @Test
    void defaultTotalFreeActionsIsOne() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        assertEquals(1, freeActionsService.getTotalFreeActions(character));
    }

    @Test
    void attributeAbilityModifierIsAdded() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(new FreeActionsBonusAbility())
                .build();
        assertEquals(2, freeActionsService.getTotalFreeActions(character));
    }

    @Test
    void skillCompetencyAbilityModifierIsAdded() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skillCompetencyAbility(new FreeActionsBonusSkillCompetencyAbility())
                .build();
        assertEquals(2, freeActionsService.getTotalFreeActions(character));
    }

    @Test
    void unlockedExcellencyModifierIsAddedForATrainedSkill() {
        CharacterSkill atletismoSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATLETISMO_1).build();
        atletismoSkill.increaseGraduation(3);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.ATLETISMO, atletismoSkill)
                .build();

        assertEquals(2, freeActionsService.getTotalFreeActions(character));
    }

    @Test
    void excellencyNotYetUnlockedGrantsNoBonus() {
        CharacterSkill atletismoSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATLETISMO_1).build();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.ATLETISMO, atletismoSkill)
                .build();

        assertEquals(1, freeActionsService.getTotalFreeActions(character));
    }

    @Test
    void totalFreeActionsNeverGoesBelowZero() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skillCompetencyAbility(new FreeActionsMalusSkillCompetencyAbility())
                .build();
        assertEquals(0, freeActionsService.getTotalFreeActions(character));
    }
}
