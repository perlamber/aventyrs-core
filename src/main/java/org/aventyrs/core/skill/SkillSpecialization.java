package org.aventyrs.core.skill;

/**
 * A held Especialização — one of a Perícia's fixed, named specialties (e.g. {@code
 * AttentionSpecialization#INVESTIGAR}). Purely a {@link SkillTrait}: it doesn't grant a bonus
 * or reduction of its own the way a {@link SkillCompetencyAbility} can — its only mechanical
 * effect is what {@link AbstractSkillInteraction} does when one is named as a {@link
 * SkillRoll#getRequestedAbility()} and the character actually holds it (see {@link
 * org.aventyrs.core.character.CharacterSkill#getSpecializations()}): the roll's reached {@link
 * DifficultyLevel} is resolved against {@link DifficultyLevel#getExpertValue()} instead of
 * {@link DifficultyLevel#getBaseValue()}.
 */
public interface SkillSpecialization extends SkillTrait {
}
