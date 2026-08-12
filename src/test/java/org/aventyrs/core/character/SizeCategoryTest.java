package org.aventyrs.core.character;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SizeCategoryTest {

    static Stream<Arguments> tableRows() {
        return Stream.of(
                Arguments.of(SizeCategory.MINUS_FOUR, 0.1, 0.4, 1, -2, 2, 2, 1),
                Arguments.of(SizeCategory.MINUS_THREE, 0.4, 0.7, 1, -2, 2, 2, 1),
                Arguments.of(SizeCategory.MINUS_TWO, 0.7, 1.0, 1, -1, 1, 1, 2),
                Arguments.of(SizeCategory.MINUS_ONE, 1.0, 1.4, 1, -1, 1, 1, 3),
                Arguments.of(SizeCategory.ZERO, 1.5, 2.0, 1, 0, 0, 0, 4),
                Arguments.of(SizeCategory.PLUS_ONE, 2.2, 3.0, 1, 1, -1, -1, 5),
                Arguments.of(SizeCategory.PLUS_TWO, 3.3, 4.5, 2, 1, -1, -1, 6),
                Arguments.of(SizeCategory.PLUS_THREE, 4.8, 7.2, 2, 2, -2, -2, 7),
                Arguments.of(SizeCategory.PLUS_FOUR, 7.2, 10.1, 3, 2, -2, -2, 8)
        );
    }

    @ParameterizedTest
    @MethodSource("tableRows")
    void matchesSizeCategoryTable(SizeCategory sizeCategory, double minHeight, double maxHeight,
                                   int meleeRange, int attackAndDamageModifier, int defenseModifier,
                                   int stealthAndAttentionModifier, int movementPerActionPoint) {
        assertEquals(minHeight, sizeCategory.getMinHeight());
        assertEquals(maxHeight, sizeCategory.getMaxHeight());
        assertEquals(meleeRange, sizeCategory.getRange());
        assertEquals(attackAndDamageModifier, sizeCategory.getAttackAndDamageModifier());
        assertEquals(defenseModifier, sizeCategory.getDefenseModifier());
        assertEquals(stealthAndAttentionModifier, sizeCategory.getStealthAndAttentionModifier());
        assertEquals(movementPerActionPoint, sizeCategory.getMovementPerActionPoint());
    }

    @Test
    void humanBaselineHasAverageHeightOfOneSeventyFive() {
        assertEquals(1.75, SizeCategory.ZERO.getAverageHeight());
    }

    @Test
    void shiftMovesByTheGivenNumberOfSteps() {
        assertEquals(SizeCategory.PLUS_ONE, SizeCategory.ZERO.shift(1));
        assertEquals(SizeCategory.MINUS_ONE, SizeCategory.ZERO.shift(-1));
    }

    @Test
    void shiftClampsAtThePlusFourCeiling() {
        assertEquals(SizeCategory.PLUS_FOUR, SizeCategory.PLUS_FOUR.shift(1));
    }

    @Test
    void shiftClampsAtTheMinusFourFloor() {
        assertEquals(SizeCategory.MINUS_FOUR, SizeCategory.MINUS_FOUR.shift(-1));
    }
}
