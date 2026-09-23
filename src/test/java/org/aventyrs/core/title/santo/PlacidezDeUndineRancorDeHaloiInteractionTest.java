package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.title.EmpoweredAttack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_DETERMINATION_POINTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Placidez de Undine, Rancor de Haloi — 2PD reported as everything the one required attack gains.
 * Nothing is applied: the attack does not exist yet when this resolves.
 */
class PlacidezDeUndineRancorDeHaloiInteractionTest {

    private final PlacidezDeUndineRancorDeHaloiInteraction interaction =
            new PlacidezDeUndineRancorDeHaloiInteraction();
    private final DeterminationPointsService determinationPointsService = new DeterminationPointsServiceImpl();

    private CharacterSheet holder;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        holder = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }

    private int currentPd(final CharacterSheet sheet) {
        return determinationPointsService.getCurrentDeterminationPoints(sheet.getCharacter(), sheet);
    }

    @Test
    void itSpendsTwoPdAndReportsEveryFigureTheAttackGains() {
        int pdBefore = currentPd(holder);

        InteractionResult result = interaction.applyTo(holder);

        assertEquals(2, result.getDeterminationPointsSpent());
        assertEquals(pdBefore - 2, currentPd(holder));

        EmpoweredAttack empowered = result.getEmpoweredAttack();
        assertEquals(PlacidezDeUndineRancorDeHaloiInteraction.CRITICAL_MARGIN_INCREASE,
                empowered.criticalMarginIncrease());
        assertEquals(PlacidezDeUndineRancorDeHaloiInteraction.LIFE_STEAL, empowered.lifeSteal());
        assertEquals(PlacidezDeUndineRancorDeHaloiInteraction.DETERMINATION_STEAL, empowered.determinationSteal());
        assertEquals(CriticalEffectType.OFERENDA_MALDITA, empowered.additionalCriticalEffect());
    }

    /** It touches only the Margem Crítica / Roubo stage, so the roll-and-dano fields stay inert. */
    @Test
    void itReportsNothingForTheRollOrTheDanoRoll() {
        EmpoweredAttack empowered = interaction.applyTo(holder).getEmpoweredAttack();

        assertEquals(0, empowered.attackRollBonus());
        assertEquals(0, empowered.difficultyReduction());
        assertEquals(0, empowered.extraDamageDice());
        assertEquals(0, empowered.extraDamageFlat());
        assertEquals(0, empowered.rangeIncrease());
    }

    /** It is PD-priced, unlike its three Abraçado pela Escuridão siblings. */
    @Test
    void itCostsNoHitPoints() {
        InteractionResult result = interaction.applyTo(holder);

        assertNull(result.getResourceLossValue());
        assertEquals(0, holder.getDamageTaken());
    }

    @Test
    void anActivationThePdCannotCoverIsRefusedAndCostsNothing() {
        holder.spendDeterminationPoints(currentPd(holder));

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> interaction.applyTo(holder));

        assertEquals(NOT_ENOUGH_DETERMINATION_POINTS, refused.getMessage());
        assertEquals(0, currentPd(holder));
    }
}
