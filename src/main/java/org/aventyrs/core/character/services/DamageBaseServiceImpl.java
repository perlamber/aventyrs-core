package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import lombok.NonNull;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;

public class DamageBaseServiceImpl implements DamageBaseService {

    @Override
    public DamageBase getDamageBase(final Character character, final SkillType attackingSkill) {
        return DamageBase.UNARMED.scaledUp(sumScaleUps(character, attackingSkill, null));
    }

    @Override
    public DamageBase getDamageBase(final Character character, @NonNull final Weapon weapon) {
        return weapon.getEffectiveDamageBase().scaledUp(sumScaleUps(character, weapon.getSkillType(), weapon));
    }

    /**
     * Every "+N Dano Base" grant character currently brings to an attackingSkill attack — see
     * {@link DamageBaseService}'s own javadoc for the three sources and why the Excelência one
     * is scoped to the attacking Perícia alone.
     */
    private int sumScaleUps(final Character character, final SkillType attackingSkill, final Weapon weapon) {
        int scaleUps = character.getFeats().stream()
                .mapToInt(feat -> feat.resolveDamageBaseIncrease(character, weapon))
                .sum();

        scaleUps += SkillCompetencyAbility.allFor(character).stream()
                .mapToInt(ability -> ability.resolveDamageBaseIncrease(attackingSkill, character))
                .sum();
        if (weapon != null) {
            scaleUps += character.getEquipment().stream()
                    .mapToInt(item -> item.resolveEnhancementDamageBaseIncrease(weapon, character))
                    .sum();
        }

        return scaleUps + sumAttackingSkillExcellencies(character, attackingSkill);
    }

    /**
     * The Dano Base scale-ups from the tiers attackingSkill's <em>own</em> Graduação has
     * unlocked. An untrained Perícia has unlocked none, and reads as Graduação 0 rather than
     * being an error — same reading as everywhere else in this core.
     */
    private int sumAttackingSkillExcellencies(final Character character, final SkillType attackingSkill) {
        CharacterSkill characterSkill = character.getSkills().get(attackingSkill);
        if (characterSkill == null) {
            return 0;
        }
        int graduationValue = characterSkill.getGraduation().getGraduationValue();
        return SkillExcellency.unlockedBy(attackingSkill.getExcellencyClass(), graduationValue).stream()
                .mapToInt(SkillExcellency::resolveDamageBaseIncrease)
                .sum();
    }
}
