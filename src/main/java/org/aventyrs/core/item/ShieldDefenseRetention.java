package org.aventyrs.core.item;

/**
 * How much of a Escudo's Defesa bonuses its wielder keeps after attacking with it — {@code
 * EscudeiroFeat#ATACAR_COM_ESCUDOS}' "se o fizer você perde metade dos bônus em Defesas concedidos por
 * ele até o início de seu próximo turno … atacar duas ou mais vezes com um escudo faz com que você não
 * receba seus bônus em Defesas", eased by {@code #ATAQUE_MULTIPLO_COM_ESCUDOS} ("mantém metade … ao
 * realizar mais de um Ataque") and lifted by {@code #ARTE_DO_ESCUDO_ATACANTE} ("não perde Bônus
 * Defensivo"). Each held Talento states one ({@code Feat#resolveShieldDefenseRetention}); the most
 * generous wins. Ordered from least to most kept.
 */
public enum ShieldDefenseRetention {

    /** Nothing kept. */
    NONE,

    /** Half kept — the lost half rounded down, so a +1 keeps +1. */
    HALF,

    /** Everything kept. */
    FULL;

    /** What of bonus this retention keeps. */
    public int apply(final int bonus) {
        return switch (this) {
            case NONE -> Math.min(0, bonus);
            case HALF -> bonus - bonus / 2;
            case FULL -> bonus;
        };
    }

    /** The more generous of the two. */
    public ShieldDefenseRetention atLeast(final ShieldDefenseRetention other) {
        return other == null || other.ordinal() < ordinal() ? this : other;
    }
}
