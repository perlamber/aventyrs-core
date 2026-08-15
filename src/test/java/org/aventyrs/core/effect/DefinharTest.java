package org.aventyrs.core.effect;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class DefinharTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet newSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void applyToDealsNoImmediateDamage() {
        CharacterSheet sheet = newSheet();

        InteractionResult result = new Definhar().applyTo(sheet);

        assertNull(result.getResourceLossValue());
        assertNull(result.getResourceLossType());
        assertEquals(0, sheet.getDamageTaken());
        assertEquals(sheet.getCharacter().getStatus(), result.getResultStatus());
    }

    @Test
    void witherForRoundsEqualToTheTargetsVigor() {
        CharacterSheet sheet = newSheet();
        int vigor = sheet.getCharacter().getAttributes().getVigor().getTotal();

        new Definhar().applyTo(sheet);
        for (int round = 0; round < vigor; round++) {
            sheet.tickTemporaryEffects();
        }
        int damageAfterVigorRounds = sheet.getDamageTaken();
        sheet.tickTemporaryEffects();

        assertEquals(vigor, damageAfterVigorRounds);
        assertEquals(damageAfterVigorRounds, sheet.getDamageTaken());
    }

    @Test
    void theOngoingDamageBypassesShieldSinceItsCurseDamage() {
        CharacterSheet sheet = newSheet();
        sheet.addShield(10);

        new Definhar().applyTo(sheet);
        sheet.tickTemporaryEffects();

        assertEquals(1, sheet.getDamageTaken());
        assertEquals(10, sheet.getShieldPoints());
    }

    @Test
    void reapplyingDefinharDoesNotStackThePerRoundLoss() {
        CharacterSheet sheet = newSheet();

        new Definhar().applyTo(sheet);
        new Definhar().applyTo(sheet);
        sheet.tickTemporaryEffects();

        assertEquals(1, sheet.getDamageTaken());
    }

    @Test
    void receiveInteractionDelegatesCorrectly() {
        CharacterSheet sheet = newSheet();

        InteractionResult result = sheet.receiveInteraction(new Definhar());
        sheet.tickTemporaryEffects();

        assertEquals(1, sheet.getDamageTaken());
        assertEquals(sheet.getCharacter().getStatus(), result.getResultStatus());
    }

    @Test
    void descriptionMatchesTheRulesText() {
        assertFalse(new Definhar().getDescription().isBlank());
        assertEquals("O Alvo sofre 1 ponto de Dano Físico Profano por Rodada por Vigor " +
                        "Rodadas (não cumulativo). Este é um Efeito de Maldição.",
                new Definhar().getDescription());
    }
}
