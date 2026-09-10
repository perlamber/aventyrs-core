package org.aventyrs.core.magic;

/**
 * Which shape a {@link SpellDuration} takes. Three of the five are not a plain count of time, and
 * each needs a different companion to the number — see {@link SpellDuration} for the table of
 * which fields each kind carries.
 *
 * <p>There is deliberately no {@code CASTER_ATTRIBUTE} constant, though the catalog does contain
 * one caster-scaled duration: {@code OcultacaoSpell#CAMPO_DE_INVISIBILIDADE}'s Efeito Alternativo
 * runs for "Concentração + Foco Horas". An {@code Efeito Alternativo} is not a separate {@code
 * Spell} and {@code SpellData} holds one Duração, so that figure has nowhere to live either way —
 * adding a kind for it would be a constant no authored value could ever use.
 */
public enum DurationKind {

    /** {@code Instantânea}/{@code Instantâneo} — resolves and is over. 38 of 145. */
    INSTANTANEA,

    /** A stated count in a stated {@link DurationUnit}. 111 of 145, counting the Concentração trailers. */
    FIXED,

    /**
     * {@code 'Vigor do Alvo' Rodadas} — a count read off an Attribute of the <b>target</b>, not
     * the caster. 6 of 145. Nothing resolves it yet; which Attribute is authored data.
     */
    TARGET_ATTRIBUTE,

    /** {@code Até o final do turno}. 1 of 145. */
    UNTIL_END_OF_TURN,

    /** {@code A mesma de ‹other Magia›} — see {@link SpellDuration#reference()}. 10 of 145. */
    SAME_AS_REFERENCED
}
