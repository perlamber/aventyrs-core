package org.aventyrs.core.character;

import lombok.NonNull;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_DIE_ROLL;

/**
 * A dice expression an effect declares — {@code count}d6 + {@code bonus} ("1d6", "3d6",
 * "1d6+Metade do Instinto"). <b>This core never rolls</b>: the caller rolls {@link #count()} d6
 * and hands the faces to {@link #total(List)}, which checks them and adds the flat part.
 *
 * <p>Deliberately not a {@link DamageBase}: that is a position on the Dano Base scale, capped at
 * 3d6+3 and advanced by scale-ups; an effect's dice are a plain stated figure ("2d6+Instinto").
 */
public record Dice(int count, int bonus) {

    public static final int SIDES = DamageBase.DICE_SIDES;

    public Dice {
        if (count < 0) {
            throw new IllegalArgumentException("A dice expression cannot roll a negative count: " + count);
        }
    }

    public static Dice of(final int count) {
        return new Dice(count, 0);
    }

    public static Dice of(final int count, final int bonus) {
        return new Dice(count, bonus);
    }

    /** The same dice with {@code extra} more on the flat part. */
    public Dice plus(final int extra) {
        return new Dice(count, bonus + extra);
    }

    /**
     * The rolled total — the faces summed, plus {@link #bonus()}.
     *
     * @throws IllegalOperationException {@code INVALID_DIE_ROLL} when the caller rolled a different
     *                                   number of dice, or a face off a d6
     */
    public int total(@NonNull final List<Integer> faces) {
        if (faces.size() != count || faces.stream().anyMatch(face -> face == null || face < 1 || face > SIDES)) {
            throw new IllegalOperationException(INVALID_DIE_ROLL);
        }
        return faces.stream().mapToInt(Integer::intValue).sum() + bonus;
    }

    @Override
    public String toString() {
        return count + "d6" + (bonus == 0 ? "" : (bonus > 0 ? "+" : "") + bonus);
    }
}
