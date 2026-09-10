package org.aventyrs.core.character;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    /**
     * The authored table stops at Categoria +4, but the scale does not: {@code
     * docs/rules/categorias-de-tamanho.txt} says outright that beyond it "os modificadores seguem
     * as mesmas fórmulas". So the formulas — not a hardcoded table — are what carry +5…+10, and
     * this pins that they keep producing sane, monotonic values rather than stopping or wrapping.
     *
     * <p>Every category is covered, so extending the enum further later fails here first if the
     * formulas stop holding.
     */
    @Test
    void theFormulasCarryEveryCategoryPastTheAuthoredTable() {
        SizeCategory[] all = SizeCategory.values();
        for (int i = 0; i < all.length; i++) {
            SizeCategory size = all[i];
            assertEquals(Math.max(SizeCategory.MINIMUM_MELEE_RANGE, 1 + Math.floorDiv(size.getCategory(), 2)),
                    size.getRange(), size + " melee reach");
            assertEquals(Math.floorDiv(size.getCategory(), 2), size.getRangeModifier(), size + " range modifier");
            assertEquals(Math.max(1, 4 + size.getCategory()), size.getMovementPerActionPoint(),
                    size + " movement per PA");
            if (i > 0) {
                SizeCategory smaller = all[i - 1];
                assertTrue(size.getRange() >= smaller.getRange(), size + " must not reach less than " + smaller);
                assertTrue(size.getRangeModifier() >= smaller.getRangeModifier(),
                        size + " modifier must not fall below " + smaller);
            }
        }
    }

    /**
     * The body radius is the Alcance column's defender-side twin: a bigger body both reaches
     * further and is reached sooner, derived from one number so the two cannot disagree.
     *
     * <p>Floored at zero across every small Categoria — a Gnomo is not harder to punch than a
     * human, it just occupies no extra space.
     */
    @Test
    void theBodyRadiusIsTheRangeModifierNeverBelowZero() {
        for (SizeCategory size : SizeCategory.values()) {
            assertEquals(Math.max(0, size.getRangeModifier()), size.getBodyRadius(),
                    size + " body radius");
            assertTrue(size.getBodyRadius() >= 0, size + " must never shrink an attacker's reach");
        }

        // Nothing up to +1 takes extra space; the ladder starts at +2.
        assertEquals(0, SizeCategory.MINUS_FOUR.getBodyRadius());
        assertEquals(0, SizeCategory.ZERO.getBodyRadius());
        assertEquals(0, SizeCategory.PLUS_ONE.getBodyRadius());
        assertEquals(1, SizeCategory.PLUS_TWO.getBodyRadius());
        assertEquals(1, SizeCategory.PLUS_THREE.getBodyRadius());
        assertEquals(2, SizeCategory.PLUS_FOUR.getBodyRadius());
    }

    /** The Alcance column is a reach and the modifier is an addend — two different numbers with
     * two different consumers, which is the distinction {@code AttackRangeService} turns on. They
     * coincide only because an adjacency-only weapon's base happens to be 1UD. */
    @Test
    void theAlcanceColumnAndTheRangeModifierAreDifferentQuantities() {
        assertEquals(1, SizeCategory.ZERO.getRange());
        assertEquals(0, SizeCategory.ZERO.getRangeModifier());

        assertEquals(3, SizeCategory.PLUS_FOUR.getRange());
        assertEquals(2, SizeCategory.PLUS_FOUR.getRangeModifier());

        // Never below one hex, however small — while the modifier itself stays negative.
        assertEquals(1, SizeCategory.MINUS_FOUR.getRange());
        assertEquals(-2, SizeCategory.MINUS_FOUR.getRangeModifier());
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
    void shiftClampsAtThePlusTenCeiling() {
        assertEquals(SizeCategory.PLUS_TEN, SizeCategory.PLUS_TEN.shift(1));
    }

    @Test
    void shiftClampsAtTheMinusFourFloor() {
        assertEquals(SizeCategory.MINUS_FOUR, SizeCategory.MINUS_FOUR.shift(-1));
    }
}
