package org.aventyrs.core.sheet;

import lombok.Getter;

/**
 * An {@link ConditionType#IMOBILIZADO} that knows how it is escaped — "A GD da Furtividade pode variar
 * entre Resistida ou Pré-Determinada. Rolagens são consideradas Pré-Determinadas quando imobilizado
 * por um material ou situação. Rolagens são consideradas Resistidas quando imobilizado por outro
 * personagem."
 *
 * <p>{@link #getEscapeDifficulty()} is the Pré-Determinada GD (a net, a web, a collapsed wall); when
 * it is {@code null} the escape is Resistida, against whoever the {@link #getSource()} is. Imobilizado
 * comes only from specific effects, never from a plain Agarrar (table ruling, 2026-10-07), so each such
 * effect builds one of these.
 */
@Getter
public class Immobilization extends Condition {

    private final Integer escapeDifficulty;

    /** Held by a material or situation — escaped by a Furtividade roll reaching escapeDifficulty. */
    public static Immobilization byMaterial(final int escapeDifficulty, final Integer remainingRounds) {
        return new Immobilization(remainingRounds, null, escapeDifficulty);
    }

    /** Held by captor — escaped by a Furtividade roll resisted by the captor's Ataque. */
    public static Immobilization byCaptor(final CombatantSheet captor, final Integer remainingRounds) {
        return new Immobilization(remainingRounds, captor, null);
    }

    private Immobilization(final Integer remainingRounds, final CombatantSheet source, final Integer escapeDifficulty) {
        super(ConditionType.IMOBILIZADO, remainingRounds, source);
        this.escapeDifficulty = escapeDifficulty;
    }

    @Override
    Condition decayed(final ConditionType next, final int rounds) {
        return next == ConditionType.IMOBILIZADO
                ? new Immobilization(rounds, getSource(), escapeDifficulty)
                : super.decayed(next, rounds);
    }
}
