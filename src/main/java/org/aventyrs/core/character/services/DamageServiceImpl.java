package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Map;

public class DamageServiceImpl implements DamageService {

    private final ModifierResolver modifierResolver;

    public DamageServiceImpl() {
        this(new ModifierResolverImpl());
    }

    public DamageServiceImpl(final ModifierResolver modifierResolver) {
        this.modifierResolver = modifierResolver;
    }

    @Override
    public int getTotalDamageReduction(final Character character) {
        return sumAcrossSources(character, ModifierType.DAMAGE_REDUCTION);
    }

    @Override
    public int getTotalAbsoluteDamageReduction(final Character character) {
        return sumAcrossSources(character, ModifierType.ABSOLUTE_DAMAGE_REDUCTION);
    }

    @Override
    public int calculateFinalDamage(final Character character, final int rawDamage, final boolean ignoreDamageReduction, final boolean halfDamage) {
        int reduction = getTotalAbsoluteDamageReduction(character);
        if (!ignoreDamageReduction) {
            reduction += getTotalDamageReduction(character);
        }
        int afterFlatReduction = Math.max(0, rawDamage - reduction);
        int finalDamage = halfDamage ? afterFlatReduction / 2 : afterFlatReduction;
        return Math.max(0, finalDamage);
    }

    @Override
    public int applyDamage(final Character character, final CharacterSheet characterSheet, final int rawDamage, final boolean ignoreDamageReduction, final boolean halfDamage) {
        int finalDamage = calculateFinalDamage(character, rawDamage, ignoreDamageReduction, halfDamage);
        return characterSheet.applyDamage(finalDamage);
    }

    private int sumAcrossSources(final Character character, final ModifierType modifierType) {
        int total = modifierResolver.sumModifiers(character.getAttributeAbilities(), modifierType);
        total += modifierResolver.sumModifiers(character.getSkillCompetencyAbilities(), modifierType);
        for (Map.Entry<SkillType, CharacterSkill> entry : character.getSkills().entrySet()) {
            int graduationValue = entry.getValue().getGraduation().getGraduationValue();
            List<SkillExcellency> unlockedExcellencies = SkillExcellency.unlockedBy(
                    entry.getKey().getExcellencyClass(), graduationValue);
            total += modifierResolver.sumModifiers(unlockedExcellencies, modifierType);
        }
        return Math.max(0, total);
    }
}
