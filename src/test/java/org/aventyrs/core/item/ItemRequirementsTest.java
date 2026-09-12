package org.aventyrs.core.item;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemRequirementsTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void isMetWhenTheAttributeReachesTheRequiredValue() {
        Character character = characterWithStrength(3, 0, 0);

        assertTrue(new ItemRequirements(AttributeDomain.STRENGTH, 3).isMetBy(character));
    }

    @Test
    void isNotMetWhenTheAttributeFallsShort() {
        Character character = characterWithStrength(2, 0, 0);

        assertFalse(new ItemRequirements(AttributeDomain.STRENGTH, 3).isMetBy(character));
    }

    @Test
    void countsRacialAndVariableBonusesTowardsTheRequirement() {
        Character character = characterWithStrength(1, 1, 1);

        assertTrue(new ItemRequirements(AttributeDomain.STRENGTH, 3).isMetBy(character));
    }

    @Test
    void isAlwaysMetWhenNoAttributeIsNamed() {
        Character character = characterWithStrength(1, 0, 0);

        assertTrue(new ItemRequirements(null, 99).isMetBy(character));
    }

    /** "Car 3/Gno 3" — either Atributo at the value satisfies the requirement. */
    @Test
    void isMetWhenEitherOfTwoNamedAttributesReachesTheValue() {
        ItemRequirements charismaOrGnose =
                new ItemRequirements(AttributeDomain.CHARISMA, 3, AttributeDomain.GNOSE);

        assertTrue(charismaOrGnose.isMetBy(characterWith(AttributeDomain.CHARISMA, 3)));
        assertTrue(charismaOrGnose.isMetBy(characterWith(AttributeDomain.GNOSE, 3)));
        assertFalse(charismaOrGnose.isMetBy(characterWith(AttributeDomain.CHARISMA, 2)));
    }

    private static Character characterWith(final AttributeDomain domain, final int base) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(org.aventyrs.core.character.CharacterAttributes.of(
                        java.util.Map.of(domain, base)))
                .build();
    }

    private static Character characterWithStrength(final int base, final int racialBonus, final int variable) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder()
                                .domain(AttributeDomain.STRENGTH)
                                .base(base)
                                .fixedRacialBonus(racialBonus)
                                .variable(variable)
                                .build())
                        .build())
                .build();
    }
}
