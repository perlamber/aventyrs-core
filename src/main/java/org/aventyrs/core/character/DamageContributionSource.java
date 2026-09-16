package org.aventyrs.core.character;

/**
 * Where one addend of a dano roll came from — the named halves of the single {@link DamageBonus}
 * {@code AbstractSkillInteraction#sumDamageBonus} folds them all into.
 *
 * <p>One constant per source that method already scans, and nothing else: this enum reports the
 * sum's provenance, it does not decide it. A new dano-bonus source adds a constant here in the
 * same change that adds its scan, or it will be summed into the total while being invisible in
 * the {@link DamageBonusBreakdown} — the exact blindness this type exists to end.
 */
public enum DamageContributionSource {

    /** The base Ataque Corpo-a-Corpo rule's half-Força term (full Força on the Rodada's first
     * attack for a {@code StrengthAbility#DESTRUIDOR_DE_MUROS} holder). Held by nobody — it is a
     * property of the Perícia itself. */
    MEIA_FORCA,

    /** A {@code SkillCompetencyAbility#resolveDamageBonus} — e.g. {@code BRUTALIDADE}'s +1. */
    SKILL_COMPETENCY_ABILITY,

    /** An {@code EgoAdvantage#resolveDamageBonus} — e.g. {@code InitiativeAdvantage#IMPETO}. */
    EGO_ADVANTAGE,

    /** A {@code Feat#resolveDamageBonus} — e.g. {@code MonstruosoFeat#FEROCIDADE}. */
    FEAT,

    /** A round-scoped {@code TemporaryBonus} of {@code ModifierType#DAMAGE_ROLL_BONUS}. */
    TEMPORARY_BONUS,

    /** A Condição in force on the attacker (Caído/Desarmado's Desvantagem, the fear ladder). */
    CONDITION,

    /** What the <em>victim's</em> own Condições hand the attacker — Flanqueado. */
    TARGET_CONDITION,

    /** What a named manoeuvre adds to the dano roll it is the attack half of — an Investida's +2. */
    MANOEUVRE
}
