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
import org.aventyrs.core.character.CharacterStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SangramentoTest {

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
        assertThrows(IllegalOperationException.class, () -> new Sangramento(CriticalResult.NONE));
        assertThrows(IllegalOperationException.class, () -> new Sangramento(CriticalResult.FALHA_CRITICA_MENOR));
        assertThrows(IllegalOperationException.class, () -> new Sangramento(CriticalResult.FALHA_CRITICA_MAIOR));
    }

    @Test
    void applyToDealsTheImmediateTwoPvLoss() {
        CharacterSheet sheet = newSheet();

        InteractionResult result = new Sangramento(CriticalResult.ACERTO_CRITICO_MENOR).applyTo(sheet);

        assertEquals(2, result.getResourceLossValue());
        assertEquals(ResourceType.HIT_POINTS, result.getResourceLossType());
        assertEquals(2, sheet.getDamageTaken());
        // 14 max - its own 2 PV = 12, above two thirds. Before status was derived this
        // reported CLEAN: the tier from before Sangramento dealt its own damage.
        assertEquals(CharacterStatus.HIGH_LIFE, result.getResultStatus());
    }

    @Test
    void acertoCriticoMenorBleedsForRoundsEqualToTheTargetsVigor() {
        CharacterSheet sheet = newSheet();
        int vigor = sheet.getCharacter().getAttributes().getVigor().getTotal();

        new Sangramento(CriticalResult.ACERTO_CRITICO_MENOR).applyTo(sheet);
        for (int round = 0; round < vigor; round++) {
            sheet.tickTemporaryEffects();
        }
        int damageAfterVigorRounds = sheet.getDamageTaken();
        sheet.tickTemporaryEffects();

        assertEquals(2 + vigor, damageAfterVigorRounds);
        assertEquals(damageAfterVigorRounds, sheet.getDamageTaken());
    }

    @Test
    void acertoCriticoMaiorBleedingNeverExpiresFromTickingAlone() {
        CharacterSheet sheet = newSheet();

        new Sangramento(CriticalResult.ACERTO_CRITICO_MAIOR).applyTo(sheet);
        for (int round = 0; round < 50; round++) {
            sheet.tickTemporaryEffects();
        }

        assertEquals(2 + 50, sheet.getDamageTaken());
    }

    @Test
    void healingInterruptsTheOngoingBleedingButNotTheImmediateLoss() {
        CharacterSheet sheet = newSheet();

        new Sangramento(CriticalResult.ACERTO_CRITICO_MAIOR).applyTo(sheet);
        sheet.heal(1);
        sheet.tickTemporaryEffects();

        assertEquals(1, sheet.getDamageTaken());
    }

    @Test
    void receiveInteractionDelegatesCorrectly() {
        CharacterSheet sheet = newSheet();

        InteractionResult result = sheet.receiveInteraction(new Sangramento(CriticalResult.ACERTO_CRITICO_MENOR));

        assertEquals(2, result.getResourceLossValue());
    }

    @Test
    void bothAcertoCriticoTiersHaveANonBlankDescription() {
        assertFalse(new Sangramento(CriticalResult.ACERTO_CRITICO_MAIOR).getDescription().isBlank());
        assertFalse(new Sangramento(CriticalResult.ACERTO_CRITICO_MENOR).getDescription().isBlank());
    }
}
