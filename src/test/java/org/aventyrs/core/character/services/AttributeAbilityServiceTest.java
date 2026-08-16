package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.CharismaAbility;
import org.aventyrs.core.ability.StrengthAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttributeAbilityServiceTest {

    private final AttributeAbilityService abilityService = new AttributeAbilityServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private Character characterWithCharismaBase(final int base) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .charisma(AttributeValue.builder().domain(AttributeDomain.CHARISMA).base(base).build())
                        .build())
                .build();
    }

    @Test
    void noSlotsUnlockedBelowBaseThree() {
        assertEquals(0, abilityService.getUnlockedAbilitySlots(2));
    }

    @Test
    void oneSlotUnlockedFromBaseThree() {
        assertEquals(1, abilityService.getUnlockedAbilitySlots(3));
        assertEquals(1, abilityService.getUnlockedAbilitySlots(4));
    }

    @Test
    void twoSlotsUnlockedFromBaseFive() {
        assertEquals(2, abilityService.getUnlockedAbilitySlots(5));
        assertEquals(2, abilityService.getUnlockedAbilitySlots(6));
    }

    @Test
    void allowsFirstChoiceAtBaseThree() {
        assertDoesNotThrow(() -> abilityService.validateChoice(3, List.of(), StrengthAbility.SUBJUGAR));
    }

    @Test
    void rejectsChoiceWithNoUnlockedSlot() {
        assertThrows(IllegalOperationException.class,
                () -> abilityService.validateChoice(2, List.of(), StrengthAbility.SUBJUGAR));
    }

    @Test
    void rejectsChoosingTheSameAbilityTwice() {
        assertThrows(IllegalOperationException.class,
                () -> abilityService.validateChoice(5, List.of(StrengthAbility.SUBJUGAR), StrengthAbility.SUBJUGAR));
    }

    @Test
    void rejectsSecondChoiceUntilBaseFive() {
        assertThrows(IllegalOperationException.class,
                () -> abilityService.validateChoice(4, List.of(StrengthAbility.SUBJUGAR), StrengthAbility.MOVIMENTO_LIVRE));
    }

    @Test
    void allowsSecondDistinctChoiceAtBaseFive() {
        assertDoesNotThrow(() -> abilityService.validateChoice(5, List.of(StrengthAbility.SUBJUGAR), StrengthAbility.MOVIMENTO_LIVRE));
    }

    @Test
    void grantAttributeAbilityAddsItToAttributeAbilities() {
        Character character = characterWithCharismaBase(3);

        Character granted = abilityService.grantAttributeAbility(character, CharismaAbility.VOZ_DE_OURO);

        assertTrue(granted.getAttributeAbilities().contains(CharismaAbility.VOZ_DE_OURO));
    }

    @Test
    void grantAttributeAbilityAppliesDestinoFavoravelsPermanentSortePoint() {
        Character character = characterWithCharismaBase(3);

        Character granted = abilityService.grantAttributeAbility(character, CharismaAbility.DESTINO_FAVORAVEL);

        assertEquals(1, granted.getEgos().getSorte().getVariable());
        assertEquals(0, granted.getEgos().getAutocontrole().getVariable());
        assertEquals(0, granted.getEgos().getRecursos().getVariable());
        assertEquals(0, granted.getEgos().getIniciativa().getVariable());
    }

    @Test
    void grantAttributeAbilityLeavesEgosUnchangedForAnAbilityWithNoPermanentGain() {
        Character character = characterWithCharismaBase(3);

        Character granted = abilityService.grantAttributeAbility(character, CharismaAbility.VOZ_DE_OURO);

        assertEquals(character.getEgos().getSorte().getVariable(), granted.getEgos().getSorte().getVariable());
    }

    @Test
    void grantAttributeAbilityRejectsAnAlreadyChosenAbility() {
        Character character = characterWithCharismaBase(3).toBuilder()
                .attributeAbility(CharismaAbility.VOZ_DE_OURO)
                .build();

        assertThrows(IllegalOperationException.class,
                () -> abilityService.grantAttributeAbility(character, CharismaAbility.VOZ_DE_OURO));
    }

    @Test
    void grantAttributeAbilityRejectsWhenNoSlotIsFree() {
        Character character = characterWithCharismaBase(2);

        assertThrows(IllegalOperationException.class,
                () -> abilityService.grantAttributeAbility(character, CharismaAbility.VOZ_DE_OURO));
    }
}
