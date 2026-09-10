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
    PLUS_FOUR(4, 7.2, 10.1),
    PLUS_FIVE(5, 10.1, 14.0),
    PLUS_SIX(6, 14.0, 20.0),
    PLUS_SEVEN(7, 20.0, 28.0),
    PLUS_EIGHT(8, 28.0, 40.0),
    PLUS_NINE(9, 40.0, 60.0),
    PLUS_TEN(10, 60.0, 100.0);

    private static final int BASE_MOVEMENT_PER_ACTION_POINT = 4;
    private static final int MINIMUM_MOVEMENT_PER_ACTION_POINT = 1;
    /** No attack ever reaches less than the hex in front of you, however small the attacker. */
    public static final int MINIMUM_MELEE_RANGE = 1;

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
     * How far this creature's body extends from its own centre, in UD — the space it takes up
     * measured outward, and the <b>defender-side twin</b> of the Alcance column.
     *
     * <p>The rules give a larger body one spatial consequence, and give it outward: it reaches
     * further ("Um corpo maior alcança mais longe por ser maior"). This is the other half of that
     * same fact — an attacker needs less distance to touch a creature whose body already fills the
     * space between them. Derived from {@link #getRangeModifier()} rather than authored separately
     * so the two can never disagree about how big a body is.
     *
     * <p><b>Floored at zero.</b> A small creature is not <em>harder</em> to reach than a human; it
     * simply takes up no extra space, so every Categoria up to +1 contributes nothing.
     *
     * <p>A caller measuring reach subtracts <b>only the target's</b> radius. The attacker's own is
     * already paid for by the Alcance column, and subtracting it again would double-count the same
     * body. Where neither party is the roller — the rules' "alvos adicionais precisam estar
     * adjacentes ao alvo primário" — both radii count.
     *
     * <p>This is a <b>reach</b> rule, not an occupancy one: a creature still stands in exactly one
     * position for movement and placement. This core holds no positions at all (see {@code
     * SceneContext}), so it states how far a body extends and leaves the measuring to whoever
     * tracks where the bodies are.
     */
    public int getBodyRadius() {
        return Math.max(0, getRangeModifier());
    }

    /**
     * Half the size category, rounded down, added to (or subtracted from) the base range of a
     * weapon that states one — a Lança, a Pique, an arco. Read by {@code AttackRangeService} for
     * every such weapon, Arremesso and Ataque à Distância included.
     *
     * <p>Not used for an adjacency-only weapon: that case reads {@link #getRange()} instead, for
     * the reason documented there.
     */
    public int getRangeModifier() {
        return Math.floorDiv(category, 2);
    }

    /**
     * The <b>Alcance column</b> of the Categorias de Tamanho table, in UD — how far a body of this
     * size reaches with an attack that has no reach of its own (a fist, a dagger, a sword: anything
     * whose Alcance is {@code Range.ADJACENTE}). 1UD up to Categoria +1, 2UD at +2/+3, 3UD at +4,
     * and onward by the same formula.
     *
     * <p>This is a reach, not a modifier — {@code AttackRangeService} substitutes it wholesale for
     * an adjacency-only weapon rather than adding it to anything, because {@code Range.ADJACENTE}
     * means there is no space between the combatants rather than a distance of 1UD. A weapon that
     * <em>does</em> state a distance gets {@link #getRangeModifier()} added to it instead.
     *
     * <p>Never below {@value #MINIMUM_MELEE_RANGE}UD: a Categoria -4 creature still reaches the
     * hex in front of it.
     */
    public int getRange() {
        return Math.max(MINIMUM_MELEE_RANGE, MINIMUM_MELEE_RANGE+getRangeModifier());
    }
}
