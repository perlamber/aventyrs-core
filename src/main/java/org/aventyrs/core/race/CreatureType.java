package org.aventyrs.core.race;

/**
 * The broad creature classification every {@link Race} belongs to — used to validate a
 * Mestiço's chosen parent {@link Race} against rules text like "escolha uma raça Feérica,
 * Humanoide ou Monstruosa que não seja mestiça" (see {@code AbstractMesticoRace}/{@code
 * MeioElfo}). Only these three values exist: no existing race's own rules text describes a
 * fourth "base" creature type, and a Mestiço race reports whichever of these three its own
 * chosen parent race has, per its own "para critérios de pré-requisitos" clause — "Mestiço"
 * itself is a separate {@link Race#isMestico()} flag, not a fourth value here.
 */
public enum CreatureType {
    HUMANOIDE,
    FEERICO,
    MONSTRUOSO
}
