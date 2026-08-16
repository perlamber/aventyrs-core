package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
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
    public int getTotalAbsoluteDamageReduction(final Character character, final SceneContext sceneContext) {
        int total = sumAcrossSources(character, ModifierType.ABSOLUTE_DAMAGE_REDUCTION);
        total += sumEgoAdvantageAbsoluteDamageReduction(character, sceneContext);
        return Math.max(0, total);
    }

    @Override
    public int calculateFinalDamage(final Character character, final int rawDamage, final boolean ignoreDamageReduction) {
        return calculateFinalDamage(character, null, rawDamage, ignoreDamageReduction);
    }

    @Override
    public int calculateFinalDamage(final Character character, final SceneContext sceneContext, final int rawDamage, final boolean ignoreDamageReduction) {
        final boolean halfDamage = sumAcrossSources(character, ModifierType.HALF_DAMAGE) > 0
                || character.getEgoAdvantages().values().stream()
                        .anyMatch(advantage -> advantage.resolveHalfDamage(sceneContext));
        int reduction = getTotalAbsoluteDamageReduction(character, sceneContext);
        if (!ignoreDamageReduction) {
            reduction += getTotalDamageReduction(character);
        }
        int afterFlatReduction = Math.max(0, rawDamage - reduction);
        int finalDamage = halfDamage ? afterFlatReduction / 2 : afterFlatReduction;
        return Math.max(0, finalDamage);
    }

    @Override
    public int applyDamage(final Character character, final CharacterSheet characterSheet, final int rawDamage, final boolean ignoreDamageReduction) {
        return applyDamage(character, characterSheet, null, rawDamage, ignoreDamageReduction);
    }

    @Override
    public int applyDamage(final Character character, final CharacterSheet characterSheet, final SceneContext sceneContext,
                            final int rawDamage, final boolean ignoreDamageReduction) {
        int finalDamage = calculateFinalDamage(character, sceneContext, rawDamage, ignoreDamageReduction);
        return characterSheet.applyDamage(finalDamage);
    }

    private int sumEgoAdvantageAbsoluteDamageReduction(final Character character, final SceneContext sceneContext) {
        return character.getEgoAdvantages().values().stream()
                .mapToInt(advantage -> advantage.resolveAbsoluteDamageReduction(sceneContext))
                .sum();
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
