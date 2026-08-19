package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.rest.RestService;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AbencoadoPelaLuzInteractionTest {

    private final RestService restService = new RestServiceImpl();
    private final AbencoadoPelaLuzInteraction interaction = new AbencoadoPelaLuzInteraction(restService);

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet damagedTargetSheet(final int damageTaken) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.applyDamage(damageTaken);
        return sheet;
    }

    @Test
    void choosingHealRestoresTheRestServicesOwnShortRestAmount() {
        CharacterSheet target = damagedTargetSheet(1000);
        int expectedHeal = restService.getRecoveredHitPoints(target.getCharacter(), RestType.CURTO);

        InteractionResult result = interaction.applyTo(target, null, true);

        assertEquals(expectedHeal, result.getResourceGainValue());
        assertEquals(ResourceType.HIT_POINTS, result.getResourceGainType());
        assertEquals(1000 - expectedHeal, target.getDamageTaken());
    }

    @Test
    void choosingHealReportsTheHoldersOwnResultStatus() {
        CharacterSheet target = damagedTargetSheet(0);

        InteractionResult result = interaction.applyTo(target, null, true);

        assertEquals(target.getCharacter().getStatus(), result.getResultStatus());
    }

    @Test
    void choosingCureIsAnInertNoOpUntilMaleficioClassificationExists() {
        CharacterSheet target = damagedTargetSheet(5);

        InteractionResult result = interaction.applyTo(target, null, false);

        assertNull(result.getResourceGainValue());
        assertNull(result.getResourceGainType());
        assertEquals(5, target.getDamageTaken());
    }

    @Test
    void theBareOneArgOverloadDefaultsToTheSafeNoOp() {
        CharacterSheet target = damagedTargetSheet(5);

        InteractionResult result = interaction.applyTo(target);

        assertNull(result.getResourceGainValue());
        assertEquals(5, target.getDamageTaken());
    }
}
