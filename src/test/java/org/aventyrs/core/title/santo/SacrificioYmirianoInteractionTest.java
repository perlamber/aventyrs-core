package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.CharacterSizeService;
import org.aventyrs.core.character.services.CharacterSizeServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_HIT_POINTS;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_ACTIVATION_LIMIT_REACHED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Sacrifício Ymiriano — PV equal to Vigor buys +2 Força, or +3 and a size step when doubled. */
class SacrificioYmirianoInteractionTest {

    private final SacrificioYmirianoInteraction interaction = new SacrificioYmirianoInteraction();
    private final CharacterSizeService characterSizeService = new CharacterSizeServiceImpl();
    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    private CharacterSheet holder;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        holder = holderWithVigor(2);
    }

    private CharacterSheet holderWithVigor(final int vigorBase) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(vigorBase).build())
                        .build())
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private int strengthBonus(final CharacterSheet sheet) {
        return sheet.getTemporaryBonus(AttributeDomain.STRENGTH.getBonusModifierType());
    }

    @Test
    void theFirstActivationCostsVigorPvAndGrantsTwoStrengthForTwoRodadas() {
        InteractionResult result = interaction.applyTo(holder);

        assertEquals(2, result.getResourceLossValue());
        assertEquals(ResourceType.HIT_POINTS, result.getResourceLossType());
        assertEquals(2, holder.getDamageTaken());
        assertEquals(0, result.getDeterminationPointsSpent());
        assertEquals(SacrificioYmirianoInteraction.STRENGTH_BONUS, strengthBonus(holder));
        assertEquals(0, holder.getTemporaryBonus(ModifierType.SIZE_CATEGORY));
    }

    @Test
    void theStrengthBonusLapsesAfterTwoRodadas() {
        interaction.applyTo(holder);

        holder.finishTurn();
        assertEquals(SacrificioYmirianoInteraction.STRENGTH_BONUS, strengthBonus(holder));
        holder.finishTurn();

        assertEquals(0, strengthBonus(holder));
    }

    @Test
    void aSecondActivationInTheSameTurnUpgradesTheBonusRatherThanStacking() {
        SizeCategory baseSize = characterSizeService.getEffectiveSizeCategory(holder);
        interaction.applyTo(holder);

        interaction.applyTo(holder);

        // "o Bônus em Força muda para +3" — 3, never 2 + 3.
        assertEquals(SacrificioYmirianoInteraction.UPGRADED_STRENGTH_BONUS, strengthBonus(holder));
        assertEquals(SacrificioYmirianoInteraction.SIZE_CATEGORY_INCREASE,
                holder.getTemporaryBonus(ModifierType.SIZE_CATEGORY));
        assertEquals(baseSize.ordinal() + 1, characterSizeService.getEffectiveSizeCategory(holder).ordinal());
        assertEquals(4, holder.getDamageTaken());
    }

    @Test
    void theUpgradedBonusLastsThreeRodadas() {
        interaction.applyTo(holder);
        interaction.applyTo(holder);

        for (int rodada = 0; rodada < SacrificioYmirianoInteraction.UPGRADED_DURATION_IN_ROUNDS; rodada++) {
            assertEquals(SacrificioYmirianoInteraction.UPGRADED_STRENGTH_BONUS, strengthBonus(holder));
            holder.finishTurn();
        }

        assertEquals(0, strengthBonus(holder));
    }

    @Test
    void aThirdActivationInTheSameTurnIsRefusedAndCostsNothing() {
        interaction.applyTo(holder);
        interaction.applyTo(holder);
        int damageTaken = holder.getDamageTaken();

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> interaction.applyTo(holder));

        assertEquals(TITLE_ABILITY_ACTIVATION_LIMIT_REACHED, refused.getMessage());
        assertEquals(damageTaken, holder.getDamageTaken());
        assertEquals(SacrificioYmirianoInteraction.UPGRADED_STRENGTH_BONUS, strengthBonus(holder));
    }

    @Test
    void aNewTurnAllowsItAgainAtTheLowerFigure() {
        interaction.applyTo(holder);
        interaction.applyTo(holder);

        holder.startTurn(1);
        interaction.applyTo(holder);

        assertEquals(SacrificioYmirianoInteraction.STRENGTH_BONUS, strengthBonus(holder));
    }

    @Test
    void anActivationThatWouldBeSelfFatalIsRefused() {
        // The cost is Vigor (2), so being left with exactly 2 PV is already too little to pay:
        // paying may never bring the holder to 0.
        int toLeaveTwoHitPoints = hitPointsService.getMaxHitPoints(holder.getCharacter()) - 2;
        holder.applyDamage(toLeaveTwoHitPoints);

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> interaction.applyTo(holder));

        assertEquals(NOT_ENOUGH_HIT_POINTS, refused.getMessage());
        assertEquals(toLeaveTwoHitPoints, holder.getDamageTaken());
        assertEquals(0, strengthBonus(holder));
    }

    @Test
    void anActivationLeavingAtLeastOneHitPointIsAllowed() {
        int toLeaveThreeHitPoints = hitPointsService.getMaxHitPoints(holder.getCharacter()) - 3;
        holder.applyDamage(toLeaveThreeHitPoints);

        interaction.applyTo(holder);

        assertEquals(toLeaveThreeHitPoints + 2, holder.getDamageTaken());
        assertEquals(SacrificioYmirianoInteraction.STRENGTH_BONUS, strengthBonus(holder));
    }
}
