package org.aventyrs.core.monster;

import lombok.NonNull;

/**
 * One way a {@link MonsterBlueprint} breaks {@code criacao-de-monstros.txt} — returned in a list by
 * {@link MonsterRules#validate} rather than thrown, so an editor can show every problem at once.
 *
 * @param code    what is wrong — the translation key's suffix for a UI
 * @param subject what it is wrong about (an Atributo, Perícia, Modelo, Habilidade or Talento's
 *                constant name), or {@code null} when the violation is about the whole monster
 * @param actual  the offending amount, where one applies (points spent, Modelos held …), else 0
 * @param limit   the amount allowed, where one applies, else 0
 */
public record MonsterViolation(@NonNull Code code, String subject, int actual, int limit) {

    public enum Code {
        /** The name is blank. */
        NAME_BLANK,
        /** A negative Grau de Poder. */
        NEGATIVE_POWER_DEGREE,
        /** An Atributo base outside 1..5 — "iniciados em Base 1 e limitados à Base 5". */
        ATTRIBUTE_BASE_OUT_OF_RANGE,
        /** More than the 10 Atributo points spent above base 1. */
        ATTRIBUTE_POINTS_EXCEEDED,
        /** More Modelos than the Categoria allows. */
        TOO_MANY_MODELS,
        /** The same Modelo listed twice. */
        DUPLICATE_MODEL,
        /** A held Modelo with no Presa-tier Habilidade — "criado com 1 Habilidade de cada Modelo". */
        MODEL_WITHOUT_STARTING_ABILITY,
        /** A Habilidade from a Modelo the monster doesn't hold. */
        ABILITY_MODEL_NOT_HELD,
        /** A Habilidade whose tier is above the monster's Categoria. */
        ABILITY_TIER_TOO_HIGH,
        /** The same Habilidade held twice. */
        DUPLICATE_ABILITY,
        /** More Habilidades than one per Modelo plus one per GP+3. */
        TOO_MANY_ABILITIES,
        /** A Habilidade that asks for a choice holds none, or one it doesn't offer. */
        INVALID_ABILITY_CHOICE,
        /** A GD upgrade on a Perícia the monster isn't trained in. */
        UPGRADE_ON_UNTRAINED_SKILL,
        /** More Gnose upgrades than the monster's Gnose. */
        GNOSE_UPGRADES_EXCEEDED,
        /** More GP upgrades than one per GP+4. */
        PROGRESSION_UPGRADES_EXCEEDED,
        /** A negative upgrade or Ego allocation. */
        NEGATIVE_AMOUNT,
        /** More Talentos than one per GP+5 (plus an Exemplar's extras). */
        TOO_MANY_FEATS,
        /** A Talento that is neither Geral nor Monstruoso. */
        FEAT_NOT_ALLOWED,
        /** More Ego points allocated than the Categoria and GP+10 steps grant. */
        EGO_POINTS_EXCEEDED,
        /** A Regular monster holding what only an Exemplar may. */
        EXEMPLAR_ONLY_TRAIT
    }

    public static MonsterViolation of(@NonNull final Code code) {
        return new MonsterViolation(code, null, 0, 0);
    }

    public static MonsterViolation of(@NonNull final Code code, final String subject) {
        return new MonsterViolation(code, subject, 0, 0);
    }

    public static MonsterViolation of(@NonNull final Code code, final String subject, final int actual, final int limit) {
        return new MonsterViolation(code, subject, actual, limit);
    }
}
