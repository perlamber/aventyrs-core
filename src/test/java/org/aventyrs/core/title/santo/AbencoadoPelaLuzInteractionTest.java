package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.ResourceType;
import org.aventyrs.core.title.TitleAbilityActivationRequest;
import org.aventyrs.core.character.TitleSlot;
import org.junit.jupiter.api.BeforeEach;
import org.aventyrs.core.character.CharacterStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_CHOICE_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbencoadoPelaLuzInteractionTest {

    private final DeterminationPointsService determinationPointsService = new DeterminationPointsServiceImpl();
    private final AbencoadoPelaLuzInteraction interaction = new AbencoadoPelaLuzInteraction();

    /** The heal counts the toucher's own Habilidades, so an activator needs a real Santo. */
    private CharacterSheet santoSheet(final AbencoadoPelaLuzAbility... gatedAbilities) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        character.grantTitle(new Santo(List.of(SantoSpecialization.ABENCOADO_PELA_LUZ), List.of(gatedAbilities)),
                TitleSlot.PRIMARY);
        return CharacterSheet.of(character, new Player());
    }

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
    void choosingHealRestoresTheBaseAmountWhenTheSantoHoldsNoGatedHabilidade() {
        CharacterSheet target = damagedTargetSheet(10);

        InteractionResult result = touch(santoSheet(), target, AbencoadoPelaLuzInteraction.Branch.HEAL);

        assertEquals(SantoSpecialization.BASE_TOUCH_HEAL, result.getResourceGainValue());
        assertEquals(ResourceType.HIT_POINTS, result.getResourceGainType());
        assertEquals(10 - SantoSpecialization.BASE_TOUCH_HEAL, target.getDamageTaken());
    }

    /** "3+ Quantidade de Habilidades de Abençoado pela Luz" — the *toucher's* count, not the target's. */
    @Test
    void theHealGrowsWithTheTouchersOwnGatedHabilidades() {
        CharacterSheet target = damagedTargetSheet(10);
        CharacterSheet activator = santoSheet(AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO,
                AbencoadoPelaLuzAbility.GRITO_DE_GUERRA_VULCANO);

        InteractionResult result = touch(activator, target, AbencoadoPelaLuzInteraction.Branch.HEAL);

        assertEquals(SantoSpecialization.BASE_TOUCH_HEAL + 2, result.getResourceGainValue());
    }

    /**
     * The heal has to move the reported tier, not merely be reported alongside a stale one.
     * A BLANK target has 14 max PV, so at 12 damage it sits on 2 PV ({@code LOW_LIFE}); the base
     * 3 PV restored carry it to 5 — just past {@code MEDIUM_LIFE}'s one-third threshold. Before
     * status was derived this reported the pre-heal tier, since {@code CombatantSheet#heal}
     * refreshed nothing.
     */
    @Test
    void choosingHealReportsThePostHealStatusNotThePreHealOne() {
        CharacterSheet target = damagedTargetSheet(12);

        InteractionResult result = touch(santoSheet(), target, AbencoadoPelaLuzInteraction.Branch.HEAL);

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

    /** V19 prices this Especialização in PV, not PD: 3PV off the toucher, no PD at all. */
    @Test
    void theActivatorPaysThreePvAndOnlyTheTargetIsHealed() {
        CharacterSheet activator = santoSheet();
        CharacterSheet target = damagedTargetSheet(10);
        int pdBefore = currentPd(activator);

        InteractionResult result = touch(activator, target, AbencoadoPelaLuzInteraction.Branch.HEAL);

        assertEquals(0, result.getDeterminationPointsSpent());
        assertEquals(pdBefore, currentPd(activator));
        assertEquals(AbencoadoPelaLuzInteraction.TOUCH_HIT_POINT_COST, result.getResourceLossValue());
        assertEquals(ResourceType.HIT_POINTS, result.getResourceLossType());
        assertEquals(AbencoadoPelaLuzInteraction.TOUCH_HIT_POINT_COST, activator.getDamageTaken());
        assertEquals(10 - result.getResourceGainValue(), target.getDamageTaken());
    }

    /** Touching yourself still costs the 3PV, so the net change is the heal minus that cost. */
    @Test
    void withNoTargetTheActivatorTouchesThemself() {
        // Not a near-death figure: the 3PV cost is refused outright if paying it would reach 0.
        CharacterSheet activator = santoSheet();
        activator.applyDamage(8);

        InteractionResult result = interaction.activate(TitleAbilityActivationRequest.builder()
                .activator(activator)
                .choice(AbencoadoPelaLuzInteraction.Branch.HEAL)
                .build());

        assertEquals(8 + AbencoadoPelaLuzInteraction.TOUCH_HIT_POINT_COST - result.getResourceGainValue(),
                activator.getDamageTaken());
    }

    @Test
    void anActivationWithoutABranchIsRefusedAndCostsNothing() {
        CharacterSheet activator = santoSheet();
        activator.applyDamage(5);

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> interaction.applyTo(activator));

        assertEquals(TITLE_ABILITY_CHOICE_REQUIRED, refused.getMessage());
        assertEquals(5, activator.getDamageTaken());
    }

    /** Sourced as the Especialização, so a target in Coma gets 1PV of it — and that is what is reported. */
    @Test
    void aTargetInComaGetsOnePvAndTheReportSaysSo() {
        CharacterSheet probe = damagedTargetSheet(0);
        int max = new org.aventyrs.core.character.services.HitPointsServiceImpl()
                .getMaxHitPoints(probe.getCharacter(), probe);
        CharacterSheet target = damagedTargetSheet(2 * max - 1);

        InteractionResult result = touch(santoSheet(), target, AbencoadoPelaLuzInteraction.Branch.HEAL);

        assertEquals(1, result.getResourceGainValue());
        assertEquals(2 * max - 2, target.getDamageTaken());
    }
}
