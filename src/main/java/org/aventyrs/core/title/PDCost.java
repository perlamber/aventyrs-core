package org.aventyrs.core.title;

/**
 * A Título trait's Custo de Ativação in PD (Pontos de Determinação) — either a {@link Fixed}
 * amount stated by the rules text ("1PD"), or a {@link Variable} one the activating player
 * chooses above a floor ("Variável", e.g. {@code
 * org.aventyrs.core.title.santo.AbencoadoPelaLuzAbility#ORGULHO_ELDURIANO}, whose Duração
 * equals the PD actually spent).
 *
 * <p>A sealed pair of records rather than an enum, because each constant needs its own number.
 * {@link #fixed(int)}/{@link #variable(int)} are the factories; catalog enums import them
 * statically, since inside an enum whose field is itself named {@code PDCost} the simple name
 * {@code PDCost} resolves to that instance field rather than to this type (JLS 6.4.2), which a
 * constant's constructor arguments cannot reference.
 *
 * <p>This type only answers which amounts are legal ({@link #accepts(int)}); whether the holder
 * can afford one, and spending it, is the activating code's job.
 */
public sealed interface PDCost permits PDCost.Fixed, PDCost.Variable {

    /** No PD cost at all — a passive, or a trait whose cost is paid in something else. */
    PDCost NONE = fixed(0);

    /** A cost of exactly value PD. */
    static PDCost fixed(final int value) {
        return new Fixed(value);
    }

    /** A cost of any amount of PD from minimum upward, chosen at activation. */
    static PDCost variable(final int minimum) {
        return new Variable(minimum);
    }

    /** The least PD an activation can spend — the whole cost for a {@link Fixed} one. */
    int minimum();

    /** Whether spending spent PD is a legal activation of this cost. */
    boolean accepts(int spent);

    /** Whether the activating player chooses the amount. */
    default boolean isVariable() {
        return this instanceof Variable;
    }

    /** "Custo de Ativação: NPD". */
    record Fixed(int value) implements PDCost {

        public Fixed {
            if (value < 0) {
                throw new IllegalArgumentException("A fixed PD cost cannot be negative: " + value);
            }
        }

        @Override
        public int minimum() {
            return value;
        }

        @Override
        public boolean accepts(final int spent) {
            return spent == value;
        }
    }

    /** "Custo de Ativação: Variável", with the floor the rules text states. */
    record Variable(int minimum) implements PDCost {

        public Variable {
            if (minimum < 1) {
                throw new IllegalArgumentException("A variable PD cost needs a minimum of at least 1: " + minimum);
            }
        }

        @Override
        public boolean accepts(final int spent) {
            return spent >= minimum;
        }
    }
}
