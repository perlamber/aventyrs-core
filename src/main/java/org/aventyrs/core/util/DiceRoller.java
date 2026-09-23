package org.aventyrs.core.util;

import lombok.NonNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Where a d6 comes from when a rule needs one mid-resolution — Amaldiçoar's "1d6 Rodadas",
 * Empalar's "3d6 pontos de dano", Oferenda Maldita's "Roubo de Vida 2d6".
 *
 * <p><b>This core still never rolls.</b> The caller supplies the roller — an RNG at the table, a
 * fixed sequence in a test — exactly as it supplies a {@code SkillRoll}'s faces. What changed is
 * only <em>when</em>: an Efeito Crítico's dice are thrown once its critical is known, which is
 * inside {@code AttackDelivery}/{@code AttackReceiver}, so the caller hands over the means rather
 * than the result. A request that carries no roller simply gets no dice-bearing effect — reported,
 * never invented.
 */
@FunctionalInterface
public interface DiceRoller {

    /** One d6, 1–6. */
    int rollD6();

    /** The sum of count d6 — 0 for a count of 0 or less. */
    default int rollD6(final int count) {
        int total = 0;
        for (int i = 0; i < count; i++) {
            total += rollD6();
        }
        return total;
    }

    /**
     * A roller handing out faces in order, for a test or a replay — then repeating the last face.
     *
     * @throws IllegalArgumentException if faces is empty or holds a value outside 1–6
     */
    static DiceRoller fixed(@NonNull final List<Integer> faces) {
        if (faces.isEmpty() || faces.stream().anyMatch(face -> face < 1 || face > 6)) {
            throw new IllegalArgumentException("A d6 face is 1-6: " + faces);
        }
        Deque<Integer> remaining = new ArrayDeque<>(faces);
        int[] last = {faces.get(faces.size() - 1)};
        return () -> remaining.isEmpty() ? last[0] : remaining.poll();
    }

    /** {@link #fixed(List)} over the given faces. */
    static DiceRoller fixed(final Integer... faces) {
        return fixed(List.of(faces));
    }
}
