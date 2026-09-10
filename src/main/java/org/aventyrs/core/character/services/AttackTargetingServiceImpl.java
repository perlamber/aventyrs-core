package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.skill.SkillType;

public class AttackTargetingServiceImpl implements AttackTargetingService {

    @Override
    public int getMaximumTargets(@NonNull final Character attacker, final SkillType attackSkill) {
        int additional = attacker.getFeats().stream()
                .mapToInt(feat -> feat.resolveAdditionalTargets(attackSkill, attacker))
                .sum();
        return Math.max(BASE_TARGETS, BASE_TARGETS + additional);
    }
}
