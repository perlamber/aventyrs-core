package org.aventyrs.core.effect;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.CriticalResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SabotageTest {

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
        assertThrows(IllegalOperationException.class, () -> new Sabotage(CriticalResult.NONE));
        assertThrows(IllegalOperationException.class, () -> new Sabotage(CriticalResult.FALHA_CRITICA_MENOR));
        assertThrows(IllegalOperationException.class, () -> new Sabotage(CriticalResult.FALHA_CRITICA_MAIOR));
    }

    @Test
    void applyToOnlyReportsTheResultStatusSinceNoItemSystemExistsYet() {
        CharacterSheet sheet = newSheet();

        InteractionResult result = new Sabotage(CriticalResult.ACERTO_CRITICO_MENOR).applyTo(sheet);

        assertEquals(sheet.getCharacter().getStatus(), result.getResultStatus());
        assertNull(result.getResourceLossValue());
        assertNull(result.getResourceLossType());
        assertNull(result.getEgoLossValue());
        assertNull(result.getEgoLossDomain());
        assertNull(result.getNextInteraction());
    }

    @Test
    void receiveInteractionDelegatesCorrectly() {
        CharacterSheet sheet = newSheet();

        InteractionResult result = sheet.receiveInteraction(new Sabotage(CriticalResult.ACERTO_CRITICO_MAIOR));

        assertEquals(sheet.getCharacter().getStatus(), result.getResultStatus());
    }

    @Test
    void bothAcertoCriticoTiersHaveANonBlankDescription() {
        assertFalse(new Sabotage(CriticalResult.ACERTO_CRITICO_MAIOR).getDescription().isBlank());
        assertFalse(new Sabotage(CriticalResult.ACERTO_CRITICO_MENOR).getDescription().isBlank());
    }

    @Test
    void descriptionsMatchTheRulesTextPerSeverity() {
        assertEquals("Itens tecnológicos afetados são destruídos",
                new Sabotage(CriticalResult.ACERTO_CRITICO_MAIOR).getDescription());
        assertEquals("Itens tecnológicos sofrem 3d6 pontos de dano",
                new Sabotage(CriticalResult.ACERTO_CRITICO_MENOR).getDescription());
    }
}
