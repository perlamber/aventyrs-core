package org.aventyrs.core.magic;

import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.skill.SkillType;

public interface Spell {

    DifficultyLevel getCastingDifficultyLevel();

    String getDescription();

    CriticalEffectType getCriticalEffectType();

    SkillType getConjurationSkillType();

    SkillType getAttackSkillType();

    BranchLevel getBranchLevel();

    MagicType getPrimaryType();

    MagicType getSecondaryType();

    int getDuration();

    SpellReach getReach();
}
