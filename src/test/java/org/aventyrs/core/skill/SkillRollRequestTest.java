package org.aventyrs.core.skill;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkillRollRequestTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet blankSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void builderRequiresASkillType() {
        CharacterSheet sheet = blankSheet();

        assertThrows(NullPointerException.class, () -> SkillRollRequest.builder().target(sheet).build());
    }

    @Test
    void builderRequiresATarget() {
        assertThrows(NullPointerException.class, () -> SkillRollRequest.builder().skillType(SkillType.ARTES).build());
    }

    @Test
    void sceneContextAndSkillRollAreOptional() {
        CharacterSheet sheet = blankSheet();

        SkillRollRequest request = SkillRollRequest.builder()
                .skillType(SkillType.ARTES)
                .target(sheet)
                .build();

        assertNull(request.getSceneContext());
        assertNull(request.getSkillRoll());
    }

    @Test
    void builderAssignsEveryField() {
        CharacterSheet sheet = blankSheet();
        SceneContext sceneContext = new SceneContext(List.of(), List.of(), Map.of());
        SkillRoll skillRoll = new SkillRoll(List.of(2, 3, 4));

        SkillRollRequest request = SkillRollRequest.builder()
                .skillType(SkillType.ARTES)
                .target(sheet)
                .sceneContext(sceneContext)
                .skillRoll(skillRoll)
                .build();

        assertEquals(SkillType.ARTES, request.getSkillType());
        assertSame(sheet, request.getTarget());
        assertSame(sceneContext, request.getSceneContext());
        assertSame(skillRoll, request.getSkillRoll());
    }
}
