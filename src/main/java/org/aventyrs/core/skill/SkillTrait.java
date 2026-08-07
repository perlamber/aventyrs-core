package org.aventyrs.core.skill;

/**
 * Something a {@link SkillRoll} can name as its {@code requestedAbility} — belongs to exactly
 * one {@link SkillType}, and must actually be held by the character attempting the roll before
 * {@link AbstractSkillInteraction} lets the roll proceed using it. {@link SkillCompetencyAbility}
 * (an acquired maneuver) and {@link SkillSpecialization} (a held Especialização) are the two
 * implementations — the common ground between them, and nothing more; each still carries its
 * own additional behavior on top of this shared contract.
 */
public interface SkillTrait {
    SkillType getSkillType();
    String getDescription();
}
