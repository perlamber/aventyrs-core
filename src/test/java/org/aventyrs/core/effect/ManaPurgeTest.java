package org.aventyrs.core.effect;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.ResourceType;
import org.aventyrs.core.skill.CriticalResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ManaPurgeTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet newSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void rejectsAnyCriticalResultThatIsNotAnAcertoCritico() {
        assertThrows(IllegalOperationException.class, () -> new ManaPurge(CriticalResult.NONE));
        assertThrows(IllegalOperationException.class, () -> new ManaPurge(CriticalResult.FALHA_CRITICA_MENOR));
        assertThrows(IllegalOperationException.class, () -> new ManaPurge(CriticalResult.FALHA_CRITICA_MAIOR));
    }

    @Test
    void applyToDealsTheImmediateTwoPmLoss() {
        CharacterSheet sheet = newSheet();

        InteractionResult result = new ManaPurge(CriticalResult.ACERTO_CRITICO_MENOR).applyTo(sheet);

        assertEquals(2, result.getResourceLossValue());
        assertEquals(ResourceType.MAGIC_POINTS, result.getResourceLossType());
        assertEquals(2, sheet.getManaSpent());
        assertEquals(sheet.getCharacter().getStatus(), result.getResultStatus());
    }

    @Test
    void acertoCriticoMenorDrainsForRoundsEqualToTheTargetsFoco() {
        CharacterSheet sheet = newSheet();
        int foco = sheet.getCharacter().getAttributes().getFocus().getTotal();

        new ManaPurge(CriticalResult.ACERTO_CRITICO_MENOR).applyTo(sheet);
        for (int round = 0; round < foco; round++) {
            sheet.tickTemporaryEffects();
        }
        int manaSpentAfterFocoRounds = sheet.getManaSpent();
        sheet.tickTemporaryEffects();

        assertEquals(2 + foco, manaSpentAfterFocoRounds);
        assertEquals(manaSpentAfterFocoRounds, sheet.getManaSpent());
    }

    @Test
    void acertoCriticoMaiorDrainNeverExpiresFromTickingAlone() {
        CharacterSheet sheet = newSheet();

        new ManaPurge(CriticalResult.ACERTO_CRITICO_MAIOR).applyTo(sheet);
        for (int round = 0; round < 50; round++) {
            sheet.tickTemporaryEffects();
        }

        assertEquals(2 + 50, sheet.getManaSpent());
    }

    @Test
    void recoveringManaInterruptsTheOngoingDrainButNotTheImmediateLoss() {
        CharacterSheet sheet = newSheet();

        new ManaPurge(CriticalResult.ACERTO_CRITICO_MAIOR).applyTo(sheet);
        sheet.recoverMagicPoints(1);
        sheet.tickTemporaryEffects();

        assertEquals(1, sheet.getManaSpent());
    }

    @Test
    void receiveInteractionDelegatesCorrectly() {
        CharacterSheet sheet = newSheet();

        InteractionResult result = sheet.receiveInteraction(new ManaPurge(CriticalResult.ACERTO_CRITICO_MENOR));

        assertEquals(2, result.getResourceLossValue());
    }

    @Test
    void bothAcertoCriticoTiersHaveANonBlankDescription() {
        assertFalse(new ManaPurge(CriticalResult.ACERTO_CRITICO_MAIOR).getDescription().isBlank());
        assertFalse(new ManaPurge(CriticalResult.ACERTO_CRITICO_MENOR).getDescription().isBlank());
    }
}
