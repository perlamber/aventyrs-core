package org.aventyrs.core.title.santo;

import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Furor de Sylph — a budget of enhanced attacks rather than a Duração, and a +1PA that lasts
 * exactly as long as that budget does.
 */
class FurorDeSylphInteractionTest {

    private final FurorDeSylphInteraction interaction = new FurorDeSylphInteraction();
    private final ActionPointsService actionPointsService = new ActionPointsServiceImpl();

    private CharacterSheet holder;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        holder = holderWithVigor(4);
    }

    /** A Santo actually holding the Suprema, since the +1PA is scanned off the held Título. */
    private CharacterSheet holderWithVigor(final int vigorBase) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(vigorBase).build())
                        .build())
                .build();
        character.grantTitle(new Santo(List.of(SantoSpecialization.ABRACADO_PELA_ESCURIDAO),
                List.of(AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH)), TitleSlot.PRIMARY);
        return CharacterSheet.of(character, new Player());
    }

    private int remaining() {
        return holder.getRemainingEnhancedAttacks(AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH);
    }

    /** "1+ metade dos PV gastos", the PV spent being Vigor — 4 Vigor buys 1 + 2 = 3 attacks. */
    @Test
    void itPaysVigorInPvAndBuysOnePlusHalfThatManyAttacks() {
        InteractionResult result = interaction.applyTo(holder);

        assertEquals(4, result.getResourceLossValue());
        assertEquals(2, result.getDeterminationPointsSpent());
        assertEquals(3, remaining());
    }

    @Test
    void thePaBonusLastsExactlyAsLongAsTheBudget() {
        int paBefore = actionPointsService.getMaxActionPoints(holder, 1);

        interaction.applyTo(holder);

        assertEquals(paBefore + FurorDeSylphInteraction.ACTION_POINT_BONUS,
                actionPointsService.getMaxActionPoints(holder, 1));

        for (int attack = 0; attack < 3; attack++) {
            assertTrue(holder.consumeEnhancedAttack(AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH));
        }

        assertEquals(0, remaining());
        assertEquals(paBefore, actionPointsService.getMaxActionPoints(holder, 1));
    }

    /**
     * The budget is deliberately not a TemporaryBonus: a Rodada passing must not spend it, since
     * the clause names no Duração at all.
     */
    @Test
    void theBudgetSurvivesARodadaBoundary() {
        interaction.applyTo(holder);

        holder.finishTurn();

        assertEquals(3, remaining());
    }

    @Test
    void consumingWithNoneLeftReportsSo() {
        assertFalse(holder.consumeEnhancedAttack(AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH));
    }

    /** Re-activating restates the budget rather than banking onto what was left. */
    @Test
    void reactivatingReplacesTheRemainingBudget() {
        interaction.applyTo(holder);
        holder.consumeEnhancedAttack(AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH);
        assertEquals(2, remaining());

        interaction.applyTo(holder);

        assertEquals(3, remaining());
    }

    /** No budget, no bonus — the scan must not grant PA to a Santo who never activated it. */
    @Test
    void theBonusIsAbsentBeforeAnyActivation() {
        assertEquals(0, AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH.resolveActionPointBonus(holder));
        assertEquals(0, AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH.resolveActionPointBonus(null));
    }

    /** The caller's one generic charge call spends Furor de Sylph's budget on any attack. */
    @Test
    void titleAttackModifiersConsumeChargesSpendsOneFurorCharge() {
        interaction.applyTo(holder);
        int before = remaining();

        org.aventyrs.core.title.TitleAttackModifiers.consumeCharges(holder, null);

        assertEquals(before - 1, remaining());
    }
}
