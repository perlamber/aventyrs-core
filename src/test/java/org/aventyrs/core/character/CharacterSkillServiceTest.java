package org.aventyrs.core.character;

import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.skill.SkillGraduation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CharacterSkillServiceTest {

    @BeforeEach
    public void setup() {
        CharacterSkillFixture.loadTemplates();
    }

    @Test
    void getValueForRoll() {
        CharacterSkill skill = CharacterSkillFixture.blank(CharacterSkillFixture.ATTENTION_1).graduation(SkillGraduation.INITIAL_BUILDER.build()).build();
        skill.increaseGraduation(1);
        CharacterAttributes constitution = CharacterAttributes.builder().instinct(2).build();
        Race character = new Human();
        assertEquals(3, skill.getValueForRoll(constitution, character));
    }

    @Test
    public void getValueForRollTestWithSkillGraduation() {
        CharacterSkill skill = CharacterSkillFixture.blank(CharacterSkillFixture.ATTENTION_1).build();
        skill.increaseGraduation(1);
        CharacterAttributes constitution = CharacterAttributes.builder().instinct(2).build();
        Race race = new Human();
        assertEquals(4, skill.getValueForRoll(constitution, race));
        skill.increaseGraduation(3);
        assertEquals(7, skill.getValueForRoll(constitution, race));
    }

    @Test
    public void getValueForRollTestWithNewConstitution() {
        CharacterSkill skill = CharacterSkillFixture.blank(CharacterSkillFixture.ATTENTION_1).build();
        skill.increaseGraduation(1);
        CharacterAttributes constitution = CharacterAttributes.builder().instinct(2).build();
        Race race = new Human();
        assertEquals(4, skill.getValueForRoll(constitution, race));
        CharacterAttributes newConstitution = constitution.toBuilder().instinct(4).build();
        assertEquals(6, skill.getValueForRoll(newConstitution, race));
    }

}