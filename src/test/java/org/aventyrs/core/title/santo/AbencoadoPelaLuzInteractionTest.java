package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.rest.RestService;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.ResourceType;
import org.aventyrs.core.title.TitleAbilityActivationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.aventyrs.core.character.CharacterStatus;
import org.junit.jupiter.api.Test;

import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_CHOICE_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbencoadoPelaLuzInteractionTest {

    private final RestService restService = new RestServiceImpl();
    private final DeterminationPointsService determinationPointsService = new DeterminationPointsServiceImpl();
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

    private CharacterSheet activatorSheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }

    private int currentPd(final CharacterSheet sheet) {
        return determinationPointsService.getCurrentDeterminationPoints(sheet.getCharacter(), sheet);
    }

    /** activator touches target, choosing branch. */
    private InteractionResult touch(final CharacterSheet activator, final CharacterSheet target,
                                    final AbencoadoPelaLuzInteraction.Branch branch) {
        return interaction.activate(TitleAbilityActivationRequest.builder()
                .activator(activator)
                .target(target)
                .choice(branch)
                .build());
    }

    @Test
    void choosingHealRestoresTheRestServicesOwnShortRestAmount() {
        CharacterSheet target = damagedTargetSheet(1000);
        int expectedHeal = restService.getRecoveredHitPoints(target.getCharacter(), RestType.CURTO);

        InteractionResult result = touch(activatorSheet(), target, AbencoadoPelaLuzInteraction.Branch.HEAL);

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

        InteractionResult result = touch(activatorSheet(), target, AbencoadoPelaLuzInteraction.Branch.HEAL);

        assertEquals(9, target.getDamageTaken());
        assertEquals(CharacterStatus.MEDIUM_LIFE, result.getResultStatus());
    }

    @Test
    void choosingCureIsAnInertNoOpUntilMaleficioClassificationExists() {
        CharacterSheet target = damagedTargetSheet(5);

        InteractionResult result = touch(activatorSheet(), target, AbencoadoPelaLuzInteraction.Branch.REMOVE_MALEFICIO);

        assertNull(result.getResourceGainValue());
        assertNull(result.getResourceGainType());
        assertEquals(5, target.getDamageTaken());
    }

    @Test
    void theActivatorPaysOnePdAndOnlyTheTargetIsHealed() {
        CharacterSheet activator = damagedTargetSheet(5);
        CharacterSheet target = damagedTargetSheet(1000);
        int pdBefore = currentPd(activator);

        InteractionResult result = touch(activator, target, AbencoadoPelaLuzInteraction.Branch.HEAL);

        assertEquals(1, result.getDeterminationPointsSpent());
        assertEquals(pdBefore - 1, currentPd(activator));
        assertEquals(5, activator.getDamageTaken());
        assertEquals(1000 - result.getResourceGainValue(), target.getDamageTaken());
    }

    @Test
    void withNoTargetTheActivatorTouchesThemself() {
        CharacterSheet activator = damagedTargetSheet(1000);

        InteractionResult result = interaction.activate(TitleAbilityActivationRequest.builder()
                .activator(activator)
                .choice(AbencoadoPelaLuzInteraction.Branch.HEAL)
                .build());

        assertEquals(1000 - result.getResourceGainValue(), activator.getDamageTaken());
    }

    @Test
    void anActivationWithoutABranchIsRefusedAndCostsNothing() {
        CharacterSheet target = damagedTargetSheet(5);
        int pdBefore = currentPd(target);

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> interaction.applyTo(target));

        assertEquals(TITLE_ABILITY_CHOICE_REQUIRED, refused.getMessage());
        assertEquals(pdBefore, currentPd(target));
        assertEquals(5, target.getDamageTaken());
    }
}
