package org.aventyrs.core.character;

import org.aventyrs.core.skill.attention.Attention;
import org.aventyrs.core.skill.attention.AttentionSpecialization;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterSkillTest {

    @Test
    void specializationsDefaultsToAnEmptyListViaTheBuilder() {
        CharacterSkill characterSkill = CharacterSkill.builder().skill(new Attention()).build();
        assertTrue(characterSkill.getSpecializations().isEmpty());
    }

    @Test
    void specializationsDefaultsToAnEmptyListViaTheSingleArgConstructor() {
        CharacterSkill characterSkill = new CharacterSkill(new Attention());
        assertTrue(characterSkill.getSpecializations().isEmpty());
    }

    @Test
    void builderCanSetAndReadAListOfSpecializations() {
        CharacterSkill characterSkill = CharacterSkill.builder()
                .skill(new Attention())
                .specializations(List.of(AttentionSpecialization.INVESTIGAR, AttentionSpecialization.RASTREAR))
                .build();

        assertEquals(List.of(AttentionSpecialization.INVESTIGAR, AttentionSpecialization.RASTREAR),
                characterSkill.getSpecializations());
    }
}
