package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Identifies each Perícia, used to key a Character's trained skills for O(1) lookup instead
 * of filtering a list. One constant per concrete {@link Skill} implementation, carrying a
 * reference to that skill's {@link SkillExcellency} enum so code holding just a
 * {@code CharacterSkill} (and thus a {@code SkillType}) can resolve its unlocked Excelência
 * tiers generically — see {@link org.aventyrs.core.character.Character#getReactions()}.
 */
@Getter
@AllArgsConstructor
public enum SkillType {
    ATTENTION(AttentionExcellency.class),
    ARTES(ArtesExcellency.class);

    private final Class<? extends SkillExcellency> excellencyClass;
}
