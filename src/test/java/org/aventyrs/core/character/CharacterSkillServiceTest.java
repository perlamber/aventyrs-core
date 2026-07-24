package org.aventyrs.core.character;

import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.character.services.CharacterSkillServiceImpl;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.util.RollErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CharacterSkillServiceTest {

    private final CharacterSkillService characterSkillService = new CharacterSkillServiceImpl();

    @BeforeEach
    public void setup() {
        CharacterSkillFixture.loadTemplates();
    }

    @Test
    void getValueForRoll() throws RollErrorException {
        CharacterSkill skill = CharacterSkillFixture.blank(CharacterSkillFixture.ATTENTION_1).graduation(SkillGraduation.INITIAL_BUILDER.build()).build();
        skill.increaseGraduation(1);
        CharacterAttributes attributes = CharacterAttributes.builder().instinct(AttributeValue.builder().base(2).build()).build();
        Race race = new Human();
        assertEquals(3, characterSkillService.getValueForRoll(skill, attributes, race));
    }

    @Test
    public void getValueForRollTestWithSkillGraduation() throws RollErrorException {
        CharacterSkill skill = CharacterSkillFixture.blank(CharacterSkillFixture.ATTENTION_1).build();
        skill.increaseGraduation(1);
        CharacterAttributes attributes = CharacterAttributes.builder().instinct(AttributeValue.builder().base(2).build()).build();
        Race race = new Human();
        assertEquals(3, characterSkillService.getValueForRoll(skill, attributes, race));
        skill.increaseGraduation(3);
        assertEquals(6, characterSkillService.getValueForRoll(skill, attributes, race));
    }

    @Test
    public void getValueForRollTestWithNewConstitution() throws RollErrorException {
        CharacterSkill skill = CharacterSkillFixture.blank(CharacterSkillFixture.ATTENTION_1).build();
        skill.increaseGraduation(1);
        CharacterAttributes attributes = CharacterAttributes.builder().instinct(AttributeValue.builder().base(2).build()).build();
        Race race = new Human();
        assertEquals(3, characterSkillService.getValueForRoll(skill, attributes, race));
        CharacterAttributes newAttributes = attributes.toBuilder().instinct(AttributeValue.builder().base(4).build()).build();
        assertEquals(5, characterSkillService.getValueForRoll(skill, newAttributes, race));
    }

}
