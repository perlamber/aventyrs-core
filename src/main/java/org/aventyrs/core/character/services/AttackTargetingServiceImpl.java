package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.scene.AreaOfEffect;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

public class AttackTargetingServiceImpl implements AttackTargetingService {

    @Override
    public int getMaximumTargets(@NonNull final Character attacker, final SkillType attackSkill) {
        int additional = attacker.getFeats().stream()
                .mapToInt(feat -> feat.resolveAdditionalTargets(attackSkill, attacker))
                .sum();
        return Math.max(BASE_TARGETS, BASE_TARGETS + additional);
    }

    @Override
    public Optional<AreaOfEffect> resolveAttackArea(@NonNull final Character attacker, final SkillType attackSkill,
                                                    final AttackSource attackSource, final SkillRoll skillRoll) {
        return attacker.getFeats().stream()
                .map(feat -> feat.resolveAttackArea(attacker, attackSkill, attackSource, skillRoll))
                .filter(java.util.Objects::nonNull)
                .findFirst();
    }
}
