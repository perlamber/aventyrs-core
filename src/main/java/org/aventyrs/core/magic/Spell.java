package org.aventyrs.core.magic;

import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.skill.SkillType;

/**
 * A Magia. It is an {@link AttackSource}: casting one at somebody is an attack delivered by
 * {@link #getAttackSkillType()}, exactly as swinging a {@code Weapon} is — which is what lets a
 * delivery-scoped ability such as {@code AtaqueADistanciaCompetencyAbility#ARREMESSO_PODEROSO}
 * cover "armas de arremessos <b>e magias</b>" without either side knowing about the other.
 * {@code getAttackSkillType()} already existed and needed no change to satisfy that interface.
 */
public interface Spell extends AttackSource {

    DifficultyLevel getCastingDifficultyLevel();

    String getDescription();

    CriticalEffectType getCriticalEffectType();

    SkillType getConjurationSkillType();

    @Override
    SkillType getAttackSkillType();

    BranchLevel getBranchLevel();

    MagicType getPrimaryType();

    MagicType getSecondaryType();

    int getDuration();

    SpellReach getReach();
}
