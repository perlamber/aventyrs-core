package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Map;

public class DamageServiceImpl implements DamageService {

    private final ModifierResolver modifierResolver;
    private final HitPointsService hitPointsService;

    public DamageServiceImpl() {
        this(new ModifierResolverImpl());
    }

    public DamageServiceImpl(final ModifierResolver modifierResolver) {
        this(modifierResolver, new HitPointsServiceImpl());
    }

    public DamageServiceImpl(final ModifierResolver modifierResolver, final HitPointsService hitPointsService) {
        this.modifierResolver = modifierResolver;
        this.hitPointsService = hitPointsService;
    }

    @Override
    public int getTotalDamageReduction(final Character character) {
        return sumAcrossSources(character, ModifierType.DAMAGE_REDUCTION);
    }

    @Override
    public int getTotalDamageReduction(final CharacterSheet target, final DamageType damageType, final CharacterSheet source) {
        Character character = target.getCharacter();
        int total = sumAcrossSources(character, ModifierType.DAMAGE_REDUCTION);
        total += sumAttributeAbilityDamageReduction(character, target, damageType, source);
        return Math.max(0, total);
    }

    @Override
    public int getTotalAbsoluteDamageReduction(final Character character) {
        return sumAcrossSources(character, ModifierType.ABSOLUTE_DAMAGE_REDUCTION);
    }

    @Override
    public int getTotalAbsoluteDamageReduction(final CharacterSheet target, final SceneContext sceneContext) {
        return computeTotalAbsoluteDamageReduction(target.getCharacter(), target, sceneContext);
    }

    private int computeTotalAbsoluteDamageReduction(final Character character, final CharacterSheet target, final SceneContext sceneContext) {
        int total = sumAcrossSources(character, ModifierType.ABSOLUTE_DAMAGE_REDUCTION);
        total += sumEgoAdvantageAbsoluteDamageReduction(character, sceneContext);
        total += sumTitleAbilityAbsoluteDamageReduction(character, target, sceneContext);
        return Math.max(0, total);
    }

    @Override
    public int calculateFinalDamage(final Character character, final int rawDamage, final boolean ignoreDamageReduction) {
        return computeFinalDamage(character, null, null, null, null, rawDamage, ignoreDamageReduction);
    }

    @Override
    public int calculateFinalDamage(final Character character, final SceneContext sceneContext, final int rawDamage, final boolean ignoreDamageReduction) {
        return computeFinalDamage(character, null, sceneContext, null, null, rawDamage, ignoreDamageReduction);
    }

    @Override
    public int calculateFinalDamage(final CharacterSheet target, final SceneContext sceneContext,
                                     final DamageType damageType, final CharacterSheet source,
                                     final int rawDamage, final boolean ignoreDamageReduction) {
        return computeFinalDamage(target.getCharacter(), target, sceneContext, damageType, source, rawDamage, ignoreDamageReduction);
    }

    private int computeFinalDamage(final Character character, final CharacterSheet target, final SceneContext sceneContext,
                                    final DamageType damageType, final CharacterSheet source,
                                    final int rawDamage, final boolean ignoreDamageReduction) {
        final boolean halfDamage = sumAcrossSources(character, ModifierType.HALF_DAMAGE) > 0
                || character.getEgoAdvantages().values().stream()
                        .anyMatch(advantage -> advantage.resolveHalfDamage(sceneContext));
        int reduction = target != null
                ? getTotalAbsoluteDamageReduction(target, sceneContext)
                : computeTotalAbsoluteDamageReduction(character, null, sceneContext);
        if (!ignoreDamageReduction) {
            reduction += target != null
                    ? getTotalDamageReduction(target, damageType, source)
                    : getTotalDamageReduction(character);
        }
        int afterFlatReduction = Math.max(0, rawDamage - reduction);
        int finalDamage = halfDamage ? afterFlatReduction / 2 : afterFlatReduction;
        return Math.max(0, finalDamage);
    }

    @Override
    public int applyDamage(final CharacterSheet characterSheet, final int rawDamage, final boolean ignoreDamageReduction) {
        return applyDamage(characterSheet, null, null, null, rawDamage, ignoreDamageReduction);
    }

    @Override
    public int applyDamage(final CharacterSheet characterSheet, final SceneContext sceneContext,
                            final int rawDamage, final boolean ignoreDamageReduction) {
        return applyDamage(characterSheet, sceneContext, null, null, rawDamage, ignoreDamageReduction);
    }

    @Override
    public int applyDamage(final CharacterSheet characterSheet, final SceneContext sceneContext,
                            final DamageType damageType, final CharacterSheet source,
                            final int rawDamage, final boolean ignoreDamageReduction) {
        int finalDamage = calculateFinalDamage(characterSheet, sceneContext, damageType, source, rawDamage, ignoreDamageReduction);
        int totalDamageTaken = characterSheet.applyDamage(finalDamage);
        refreshStatus(characterSheet);
        return totalDamageTaken;
    }

    @Override
    public CharacterStatus refreshStatus(final CharacterSheet characterSheet) {
        Character character = characterSheet.getCharacter();
        int maxHitPoints = hitPointsService.getMaxHitPoints(character);
        int unclampedCurrentHitPoints = maxHitPoints - characterSheet.getDamageTaken();
        CharacterStatus status = hitPointsService.getStatus(unclampedCurrentHitPoints, maxHitPoints);
        character.updateStatus(status);
        return status;
    }

    private int sumEgoAdvantageAbsoluteDamageReduction(final Character character, final SceneContext sceneContext) {
        return character.getEgoAdvantages().values().stream()
                .mapToInt(advantage -> advantage.resolveAbsoluteDamageReduction(sceneContext))
                .sum();
    }

    private int sumTitleAbilityAbsoluteDamageReduction(final Character character, final CharacterSheet target, final SceneContext sceneContext) {
        boolean hasLowerPvAdjacentAlly = hasAdjacentAllyWithLowerCurrentHitPoints(character, target, sceneContext);
        return character.getAllTitles().stream()
                .flatMap(title -> title.getAllAbilities().stream())
                .mapToInt(ability -> ability.resolveAbsoluteDamageReduction(sceneContext, hasLowerPvAdjacentAlly))
                .sum();
    }

    private boolean hasAdjacentAllyWithLowerCurrentHitPoints(final Character character, final CharacterSheet target, final SceneContext sceneContext) {
        if (target == null || sceneContext == null) {
            return false;
        }
        int holderCurrentHitPoints = hitPointsService.getCurrentHitPoints(character, target);
        return sceneContext.getAlliesWithin(Range.ADJACENTE).stream()
                .anyMatch(ally -> hitPointsService.getCurrentHitPoints(ally.getCharacter(), ally) < holderCurrentHitPoints);
    }

    private int sumAttributeAbilityDamageReduction(final Character character, final CharacterSheet target,
                                                     final DamageType damageType, final CharacterSheet source) {
        return character.getAttributeAbilities().stream()
                .mapToInt(ability -> ability.resolveDamageReduction(damageType, source, target))
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
