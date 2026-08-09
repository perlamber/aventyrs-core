package org.aventyrs.core.skill;

/**
 * Something a {@link SkillRoll} can name as its {@code requestedAbility} — carries one
 * canonical {@link SkillType} via {@link #getSkillType()}, and must actually be held by the
 * character attempting the roll before {@link AbstractSkillInteraction} lets the roll proceed
 * using it. {@link SkillCompetencyAbility} (an acquired maneuver) and {@link SkillSpecialization}
 * (a held Especialização) are the two implementations — the common ground between them, and
 * nothing more; each still carries its own additional behavior on top of this shared contract.
 */
public interface SkillTrait {
    SkillType getSkillType();
    String getDescription();

    /**
     * Whether this trait can be named as a {@link SkillRoll#getRequestedAbility()} for a roll
     * of {@code requestedSkillType} — {@code true} by default only when it matches {@link
     * #getSkillType()} exactly. Override when a trait's rules text applies to a whole category
     * of Perícias rather than naming one — e.g. {@code AnoesRacialAbility
     * #ABATEDORES_DE_GIGANTES}'s "vantagem nas rolagens de Ataque" covers every {@link
     * SkillType#isAttackSkill()} Perícia, not just the one {@link #getSkillType()} still
     * reports as its canonical/representative value. {@link AbstractSkillInteraction
     * #validateRequestedTrait} is the sole caller.
     */
    default boolean matchesSkillType(final SkillType requestedSkillType) {
        return getSkillType() == requestedSkillType;
    }
}
