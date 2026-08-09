package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.artes.ArtesCompetencyAbility;
import org.aventyrs.core.skill.artes.ArtesInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillInteractionFactoryTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet sheetWithCharisma(final int charismaBase) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .charisma(AttributeValue.builder().domain(AttributeDomain.CHARISMA).base(charismaBase).build())
                        .build())
                .build();
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void createReturnsTheRightConcreteInteractionForEverySkillType() {
        for (SkillType skillType : SkillType.values()) {
            assertTrue(skillType.newInteraction().getClass() == SkillInteractionFactory.create(skillType).getClass());
        }
    }

    @Test
    void resolveDelegatesToTheMatchingInteractionsApplyTo() {
        CharacterSheet sheet = sheetWithCharisma(2);
        SkillRollRequest request = SkillRollRequest.builder()
                .skillType(SkillType.ARTES)
                .target(sheet)
                .build();

        InteractionResult direct = new ArtesInteraction().applyTo(sheet);
        InteractionResult viaFactory = SkillInteractionFactory.resolve(request);

        assertEquals(direct.getSkillRollBonus(), viaFactory.getSkillRollBonus());
    }

    @Test
    void resolvePassesTheSkillRollThrough() {
        CharacterSheet sheet = sheetWithCharisma(2);
        SkillRoll skillRoll = new SkillRoll(List.of(6, 6, 6));
        SkillRollRequest request = SkillRollRequest.builder()
                .skillType(SkillType.ARTES)
                .target(sheet)
                .skillRoll(skillRoll)
                .build();

        InteractionResult result = SkillInteractionFactory.resolve(request);

        assertEquals(CriticalResult.ACERTO_CRITICO_MAIOR, result.getCriticalResult());
    }

    @Test
    void resolvePropagatesTheRequestedAbilityValidationFailure() {
        CharacterSheet sheet = sheetWithCharisma(2);
        SkillRoll skillRoll = new SkillRoll(List.of(2, 3, 4), ArtesCompetencyAbility.DOM_BARDICO);
        SkillRollRequest request = SkillRollRequest.builder()
                .skillType(SkillType.ARTES)
                .target(sheet)
                .skillRoll(skillRoll)
                .build();

        assertThrows(IllegalOperationException.class, () -> SkillInteractionFactory.resolve(request));
    }
}
