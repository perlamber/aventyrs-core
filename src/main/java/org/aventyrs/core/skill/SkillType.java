package org.aventyrs.core.skill;

/**
 * Identifies each Perícia, used to key a Character's trained skills for O(1) lookup instead
 * of filtering a list. One constant per concrete {@link Skill} implementation.
 */
public enum SkillType {
    ATTENTION,
    ARTES
}
