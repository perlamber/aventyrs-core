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
import org.aventyrs.core.character.CharacterStatus;
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

    /**
     * The heal has to move the reported tier, not merely be reported alongside a stale one.
     * A BLANK target has 14 max PV and Vigor 1, so a Descanso Curto restores exactly 1: at 10
     * damage it sits on 4 PV ({@code LOW_LIFE}), and the single restored point carries it to 5
     * — just past {@code MEDIUM_LIFE}'s one-third threshold. Before status was derived this
     * reported the pre-heal tier, since {@code CombatantSheet#heal} refreshed nothing.
     */
    @Test
    void choosingHealReportsThePostHealStatusNotThePreHealOne() {
        CharacterSheet target = damagedTargetSheet(10);
        assertEquals(1, restService.getRecoveredHitPoints(target.getCharacter(), RestType.CURTO));

        InteractionResult result = interaction.applyTo(target, null, true);

        assertEquals(9, target.getDamageTaken());
        assertEquals(CharacterStatus.MEDIUM_LIFE, result.getResultStatus());
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
