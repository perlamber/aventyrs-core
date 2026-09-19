package org.aventyrs.core.title;

import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Optional;

import static org.aventyrs.core.util.TranslatableMessages.ABILITY_ACTIVATION_PREVENTED;
import static org.aventyrs.core.util.TranslatableMessages.INVALID_PD_AMOUNT;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_DETERMINATION_POINTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The shared gates and payment every Título activation goes through. */
class AbstractTitleAbilityInteractionTest {

    private static final String VALIDATION_FAILED = "VALIDATION_FAILED";

    private final DeterminationPointsService determinationPointsService = new DeterminationPointsServiceImpl();

    private CharacterSheet activator;

    /** An ability that costs cost and names no activation class of its own. */
    private static AventyrTitleAbility abilityCosting(final PDCost cost) {
        return new AventyrTitleAbility() {
            @Override
            public String getDescription() {
                return "test ability";
            }

            @Override
            public PDCost getPDCost() {
                return cost;
            }

            @Override
            public Optional<Class<? extends Interaction>> getInteractionClass() {
                return Optional.empty();
            }
        };
    }

    /** Records how often its effect ran and the PD it was handed; optionally refuses in validate. */
    private static final class RecordingInteraction extends AbstractTitleAbilityInteraction {
        private final boolean refuse;
        private int resolved;
        private int lastDeterminationPoints = -1;

        RecordingInteraction(final PDCost cost, final boolean refuse) {
            super(abilityCosting(cost));
            this.refuse = refuse;
        }

        @Override
        protected void validate(final TitleAbilityActivationRequest request) {
            if (refuse) {
                throw new IllegalOperationException(VALIDATION_FAILED);
            }
        }

        @Override
        protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
            resolved++;
            lastDeterminationPoints = determinationPoints;
            return InteractionResult.builder().build();
        }
    }

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        activator = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }

    private int currentPd() {
        return determinationPointsService.getCurrentDeterminationPoints(activator.getCharacter(), activator);
    }

    private TitleAbilityActivationRequest.TitleAbilityActivationRequestBuilder request() {
        return TitleAbilityActivationRequest.builder().activator(activator);
    }

    /** Asserts the activation is refused with message, and that nothing was paid or resolved. */
    private void assertRefusedFree(final String message, final RecordingInteraction interaction,
                                   final Executable activation) {
        int before = currentPd();
        assertEquals(message, assertThrows(IllegalOperationException.class, activation).getMessage());
        assertEquals(before, currentPd());
        assertEquals(0, interaction.resolved);
    }

    @Test
    void aFixedCostIsPaidWhenTheRequestNamesNoAmount() {
        RecordingInteraction interaction = new RecordingInteraction(PDCost.fixed(2), false);
        int before = currentPd();

        InteractionResult result = interaction.activate(request().build());

        assertEquals(before - 2, currentPd());
        assertEquals(2, result.getDeterminationPointsSpent());
        assertEquals(1, interaction.resolved);
        assertEquals(2, interaction.lastDeterminationPoints);
    }

    @Test
    void theBareApplyToIsASelfActivationAtTheStatedCost() {
        RecordingInteraction interaction = new RecordingInteraction(PDCost.fixed(1), false);
        int before = currentPd();

        interaction.applyTo(activator);

        assertEquals(before - 1, currentPd());
    }

    @Test
    void aFixedCostNamedExplicitlyIsAccepted() {
        RecordingInteraction interaction = new RecordingInteraction(PDCost.fixed(2), false);

        assertEquals(2, interaction.activate(request().determinationPoints(2).build()).getDeterminationPointsSpent());
    }

    @Test
    void aWrongAmountForAFixedCostIsRefused() {
        RecordingInteraction interaction = new RecordingInteraction(PDCost.fixed(2), false);

        assertRefusedFree(INVALID_PD_AMOUNT, interaction,
                () -> interaction.activate(request().determinationPoints(3).build()));
    }

    @Test
    void aVariableCostPaysTheChosenAmount() {
        RecordingInteraction interaction = new RecordingInteraction(PDCost.variable(1), false);
        int before = currentPd();

        interaction.activate(request().determinationPoints(3).build());

        assertEquals(before - 3, currentPd());
        assertEquals(3, interaction.lastDeterminationPoints);
    }

    @Test
    void aVariableCostWithNoAmountIsRefused() {
        RecordingInteraction interaction = new RecordingInteraction(PDCost.variable(1), false);

        assertRefusedFree(INVALID_PD_AMOUNT, interaction, () -> interaction.activate(request().build()));
    }

    @Test
    void aVariableCostBelowItsMinimumIsRefused() {
        RecordingInteraction interaction = new RecordingInteraction(PDCost.variable(2), false);

        assertRefusedFree(INVALID_PD_AMOUNT, interaction,
                () -> interaction.activate(request().determinationPoints(1).build()));
    }

    @Test
    void tooLittlePdIsRefused() {
        RecordingInteraction interaction = new RecordingInteraction(PDCost.variable(1), false);

        assertRefusedFree(NOT_ENOUGH_DETERMINATION_POINTS, interaction,
                () -> interaction.activate(request().determinationPoints(currentPd() + 1).build()));
    }

    @Test
    void silencioIsRefused() {
        activator.applyCondition(new Condition(ConditionType.SILENCIO, 1));
        RecordingInteraction interaction = new RecordingInteraction(PDCost.fixed(1), false);

        assertRefusedFree(ABILITY_ACTIVATION_PREVENTED, interaction, () -> interaction.activate(request().build()));
    }

    @Test
    void aRefusingValidateCostsNothing() {
        RecordingInteraction interaction = new RecordingInteraction(PDCost.fixed(1), true);

        assertRefusedFree(VALIDATION_FAILED, interaction, () -> interaction.activate(request().build()));
    }

    @Test
    void aFreeAbilitySpendsNothingAndReportsZero() {
        RecordingInteraction interaction = new RecordingInteraction(PDCost.NONE, false);
        int before = currentPd();

        assertEquals(0, interaction.activate(request().build()).getDeterminationPointsSpent());
        assertEquals(before, currentPd());
    }

    @Test
    void theRequestDefaultsItsTargetToTheActivator() {
        CharacterSheet other = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());

        assertSame(activator, request().build().getEffectiveTarget());
        assertSame(other, request().target(other).build().getEffectiveTarget());
    }

    @Test
    void aChoiceIsReadOnlyAsItsOwnType() {
        TitleAbilityActivationRequest request = request().choice("heal").build();

        assertEquals(Optional.of("heal"), request.getChoice(String.class));
        assertEquals(Optional.empty(), request.getChoice(Integer.class));
        assertEquals(Optional.empty(), request().build().getChoice(String.class));
    }
}
