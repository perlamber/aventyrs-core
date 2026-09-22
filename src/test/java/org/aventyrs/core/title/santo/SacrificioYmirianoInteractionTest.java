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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Sacrifício Ymiriano — PV equal to Vigor buys a Força bonus equal to that same Vigor, plus
 * Categoria de Tamanho +2, for 1 Rodada.
 */
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
    void itCostsVigorInPvAndGrantsThatSameVigorAsForca() {
        InteractionResult result = interaction.applyTo(holder);

        assertEquals(2, result.getResourceLossValue());
        assertEquals(ResourceType.HIT_POINTS, result.getResourceLossType());
        assertEquals(2, holder.getDamageTaken());
        assertEquals(0, result.getDeterminationPointsSpent());
        assertEquals(2, strengthBonus(holder));
    }

    /** The price and the benefit are the same figure, so a bigger Vigor moves both together. */
    @Test
    void aHigherVigorRaisesBothTheCostAndTheForcaBonus() {
        CharacterSheet burly = holderWithVigor(5);

        InteractionResult result = interaction.applyTo(burly);

        assertEquals(5, result.getResourceLossValue());
        assertEquals(5, strengthBonus(burly));
    }

    @Test
    void itRaisesTheCategoriaDeTamanhoByTwo() {
        SizeCategory baseSize = characterSizeService.getEffectiveSizeCategory(holder);

        interaction.applyTo(holder);

        assertEquals(SacrificioYmirianoInteraction.SIZE_CATEGORY_INCREASE,
                holder.getTemporaryBonus(ModifierType.SIZE_CATEGORY));
        assertEquals(baseSize.ordinal() + SacrificioYmirianoInteraction.SIZE_CATEGORY_INCREASE,
                characterSizeService.getEffectiveSizeCategory(holder).ordinal());
    }

    @Test
    void bothGrantsLapseAfterOneRodada() {
        interaction.applyTo(holder);

        holder.finishTurn();

        assertEquals(0, strengthBonus(holder));
        assertEquals(0, holder.getTemporaryBonus(ModifierType.SIZE_CATEGORY));
    }

    /**
     * V19 dropped the previous revision's "até duas vezes / seu efeito é cumulativo" clause, so a
     * repeat is now an ordinary re-activation: same source, so the Blessing replaces its
     * predecessor and renews the Duração rather than stacking to +4.
     */
    @Test
    void aSecondActivationReplacesRatherThanStacksAndIsNotRefused() {
        interaction.applyTo(holder);

        interaction.applyTo(holder);

        assertEquals(2, strengthBonus(holder));
        assertEquals(SacrificioYmirianoInteraction.SIZE_CATEGORY_INCREASE,
                holder.getTemporaryBonus(ModifierType.SIZE_CATEGORY));
        // Paid for twice all the same — the PV cost is per activation.
        assertEquals(4, holder.getDamageTaken());
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
        assertEquals(2, strengthBonus(holder));
    }
}
