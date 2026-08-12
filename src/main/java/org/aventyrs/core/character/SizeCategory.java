package org.aventyrs.core.character;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Humans are the reference for size: category ZERO, 1.5m to 2m tall.
 * Every other category shifts height, reach, attack/damage, defenses,
 * movement and stealth/attention relative to that baseline.
 */
@Getter
@AllArgsConstructor
public enum SizeCategory {

    MINUS_FOUR(-4, 0.1, 0.4),
    MINUS_THREE(-3, 0.4, 0.7),
    MINUS_TWO(-2, 0.7, 1.0),
    MINUS_ONE(-1, 1.0, 1.4),
    ZERO(0, 1.5, 2.0),
    PLUS_ONE(1, 2.2, 3.0),
    PLUS_TWO(2, 3.3, 4.5),
    PLUS_THREE(3, 4.8, 7.2),
    PLUS_FOUR(4, 7.2, 10.1);

    private static final int BASE_MOVEMENT_PER_ACTION_POINT = 4;
    private static final int MINIMUM_MOVEMENT_PER_ACTION_POINT = 1;
    private static final int MINIMUM_MELEE_RANGE = 1;

    private final int category;
    private final double minHeight;
    private final double maxHeight;

    public double getAverageHeight() {
        return (minHeight + maxHeight) / 2.0;
    }

    /**
     * Shifts this category by the given number of steps, clamped to the -4/+4 range —
     * used to apply bonuses such as the Sangue de Gigante Vigor ability.
     */
    public SizeCategory shift(int steps) {
        SizeCategory[] categories = values();
        int newIndex = Math.max(0, Math.min(ordinal() + steps, categories.length - 1));
        return categories[newIndex];
    }

    /**
     * Half of the size category, rounded away from zero, as laid out in the size table.
     * Applies to attack rolls, damage rolls and combat maneuvers.
     */
    public int getAttackAndDamageModifier() {
        return (int) (Math.signum(category) * Math.ceil(Math.abs(category) / 2.0));
    }

    /**
     * Inverse of the attack/damage modifier: smaller creatures are harder to hit,
     * larger creatures are easier targets.
     */
    public int getDefenseModifier() {
        return -getAttackAndDamageModifier();
    }

    /**
     * Same magnitude as the defense modifier: smaller creatures hide and stay
     * unnoticed more easily, larger creatures less so.
     */
    public int getStealthAndAttentionModifier() {
        return getDefenseModifier();
    }

    /**
     * Movement per Action Point, base 4m at category ZERO, never below 1m.
     */
    public int getMovementPerActionPoint() {
        return Math.max(MINIMUM_MOVEMENT_PER_ACTION_POINT, BASE_MOVEMENT_PER_ACTION_POINT + category);
    }

    /**
     * Half the size category, rounded down, added to (or subtracted from) a weapon's base range.
     */
    public int getRangeModifier() {
        return Math.floorDiv(category, 2);
    }

    /**
     * Effective range in distance units for a weapon with the given base range.
     * Melee attacks never have their range reduced below {@value #MINIMUM_MELEE_RANGE}UD.
     */
    public int getRange() {
        return Math.max(MINIMUM_MELEE_RANGE, MINIMUM_MELEE_RANGE+getRangeModifier());
    }
}
