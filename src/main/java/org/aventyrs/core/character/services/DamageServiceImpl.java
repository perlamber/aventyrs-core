package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.SkillCompetencyAbility;
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
        return Math.max(0, sumAcrossSources(character, ModifierType.DAMAGE_REDUCTION)
                + sumEquipmentDamageReduction(character)
                + sumFeatDamageReduction(character));
    }

    /**
     * The fifth RD source: every held {@link Feat}'s {@code resolveDamageReduction}. Talentos are
     * outside every {@code ModifierResolver} scan (nothing scans them reflectively), so they get
     * an explicit pass — the same shape {@code DefenseServiceImpl}/{@code MovementServiceImpl}
     * already use for their own {@code Feat} hooks.
     *
     * <p>No {@code ABSOLUTE_DAMAGE_REDUCTION} counterpart, for the same reason
     * {@link #sumEquipmentDamageReduction} has none: no Talento grants RA, so a symmetric scan
     * would be built for a hypothetical consumer.
     */
    private int sumFeatDamageReduction(final Character character) {
        int total = 0;
        for (Feat feat : character.getFeats()) {
            total += feat.resolveDamageReduction(character);
        }
        return total;
    }

    @Override
    public int getTotalDamageReduction(final CombatantSheet target, final DamageType damageType, final CombatantSheet source) {
        Character character = target.getCharacter();
        int total = sumAcrossSources(character, ModifierType.DAMAGE_REDUCTION);
        total += sumEquipmentDamageReduction(character);
        total += sumFeatDamageReduction(character);
        total += sumAttributeAbilityDamageReduction(character, target, damageType, source);
        return Math.max(0, total);
    }

    /**
     * The fourth RD source, alongside the three {@link #sumAcrossSources} scans: every equipped
     * {@link Item}'s {@code ItemFavor}, for whatever {@code DAMAGE_REDUCTION} it currently
     * grants this character — 0 for an item with no Favor, or one whose Requisitos aren't met.
     * This is what makes {@code org.aventyrs.core.item.ArmorItem}'s RD Favores (e.g. {@code
     * ARMADURA_COMPLETA}'s "Dano de Corte sofrido é reduzido em -2", modeled as plain RD since
     * no damage-type-scoped mitigation exists) real rather than data with no consumer.
     *
     * <p>Deliberately has no {@code ABSOLUTE_DAMAGE_REDUCTION} counterpart: no cataloged item
     * grants RA, so a symmetric RA scan would be built for a hypothetical consumer rather than a
     * real one. Add it when an item actually needs it.
     */
    private int sumEquipmentDamageReduction(final Character character) {
        int total = 0;
        for (Item item : character.getEquipment()) {
            total += item.resolveFavorBonus(ModifierType.DAMAGE_REDUCTION, character);
        }
        return total;
    }

    @Override
    public int getTotalAbsoluteDamageReduction(final Character character) {
        return sumAcrossSources(character, ModifierType.ABSOLUTE_DAMAGE_REDUCTION);
    }

    @Override
    public int getTotalAbsoluteDamageReduction(final CombatantSheet target, final SceneContext sceneContext) {
        return computeTotalAbsoluteDamageReduction(target.getCharacter(), target, sceneContext);
    }

    private int computeTotalAbsoluteDamageReduction(final Character character, final CombatantSheet target, final SceneContext sceneContext) {
        int total = sumAcrossSources(character, ModifierType.ABSOLUTE_DAMAGE_REDUCTION);
        total += sumEgoAdvantageAbsoluteDamageReduction(character, sceneContext);
        total += sumTitleAbilityAbsoluteDamageReduction(character, target, sceneContext);
        total += sumAllyGrantedAbsoluteDamageReduction(target, sceneContext);
        return Math.max(0, total);
    }

    /**
     * RA granted <i>to</i> target by an adjacent ally's own always-on Título ability — Santo's
     * Bastião dos Necessitados being the one that exists (see {@code
     * AventyrTitleAbility#resolveAllyAbsoluteDamageReduction}).
     *
     * <p>The scan runs in the opposite direction from every other source above: those all start
     * from target's own traits, this one starts from target's neighbours and asks what each of
     * them grants outward. {@code sceneContext} is target's own — the same assumption {@link
     * #hasAdjacentAllyWithLowerCurrentHitPoints} already makes — so {@code getAlliesWithin}
     * returns exactly the neighbours to ask.
     *
     * <p>Nothing is granted or stored: an ally who walks out of adjacency simply stops being
     * counted on the next calculation, and one who walks in starts being counted. That is the
     * whole reason this is a scan rather than a {@code TemporaryBonus} handed to the recipient.
     */
    private int sumAllyGrantedAbsoluteDamageReduction(final CombatantSheet target, final SceneContext sceneContext) {
        if (target == null || sceneContext == null) {
            return 0;
        }
        int targetCurrentHitPoints = hitPointsService.getCurrentHitPoints(target.getCharacter(), target);
        int total = 0;
        for (CombatantSheet ally : sceneContext.getAlliesWithin(Range.ADJACENTE)) {
            boolean allyHasLowerPv =
                    targetCurrentHitPoints < hitPointsService.getCurrentHitPoints(ally.getCharacter(), ally);
            total += ally.getCharacter().getAllTitles().stream()
                    .flatMap(title -> title.getAllAbilities().stream())
                    .mapToInt(ability -> ability.resolveAllyAbsoluteDamageReduction(sceneContext, allyHasLowerPv))
                    .sum();
        }
        return total;
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
    public int calculateFinalDamage(final CombatantSheet target, final SceneContext sceneContext,
                                     final DamageType damageType, final CombatantSheet source,
                                     final int rawDamage, final boolean ignoreDamageReduction) {
        return computeFinalDamage(target.getCharacter(), target, sceneContext, damageType, source, rawDamage, ignoreDamageReduction);
    }

    private int computeFinalDamage(final Character character, final CombatantSheet target, final SceneContext sceneContext,
                                    final DamageType damageType, final CombatantSheet source,
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
    public int applyDamage(final CombatantSheet characterSheet, final int rawDamage, final boolean ignoreDamageReduction) {
        return applyDamage(characterSheet, null, null, null, rawDamage, ignoreDamageReduction);
    }

    @Override
    public int applyDamage(final CombatantSheet characterSheet, final SceneContext sceneContext,
                            final int rawDamage, final boolean ignoreDamageReduction) {
        return applyDamage(characterSheet, sceneContext, null, null, rawDamage, ignoreDamageReduction);
    }

    @Override
    public int applyDamage(final CombatantSheet characterSheet, final SceneContext sceneContext,
                            final DamageType damageType, final CombatantSheet source,
                            final int rawDamage, final boolean ignoreDamageReduction) {
        int finalDamage = calculateFinalDamage(characterSheet, sceneContext, damageType, source, rawDamage, ignoreDamageReduction);
        return characterSheet.applyDamage(finalDamage);
    }

    private int sumEgoAdvantageAbsoluteDamageReduction(final Character character, final SceneContext sceneContext) {
        return character.getEgoAdvantages().values().stream()
                .mapToInt(advantage -> advantage.resolveAbsoluteDamageReduction(sceneContext))
                .sum();
    }

    private int sumTitleAbilityAbsoluteDamageReduction(final Character character, final CombatantSheet target, final SceneContext sceneContext) {
        boolean hasLowerPvAdjacentAlly = hasAdjacentAllyWithLowerCurrentHitPoints(character, target, sceneContext);
        return character.getAllTitles().stream()
                .flatMap(title -> title.getAllAbilities().stream())
                .mapToInt(ability -> ability.resolveAbsoluteDamageReduction(sceneContext, hasLowerPvAdjacentAlly))
                .sum();
    }

    private boolean hasAdjacentAllyWithLowerCurrentHitPoints(final Character character, final CombatantSheet target, final SceneContext sceneContext) {
        if (target == null || sceneContext == null) {
            return false;
        }
        int holderCurrentHitPoints = hitPointsService.getCurrentHitPoints(character, target);
        return sceneContext.getAlliesWithin(Range.ADJACENTE).stream()
                .anyMatch(ally -> hitPointsService.getCurrentHitPoints(ally.getCharacter(), ally) < holderCurrentHitPoints);
    }

    private int sumAttributeAbilityDamageReduction(final Character character, final CombatantSheet target,
                                                     final DamageType damageType, final CombatantSheet source) {
        return character.getAttributeAbilities().stream()
                .mapToInt(ability -> ability.resolveDamageReduction(damageType, source, target))
                .sum();
    }

    /**
     * The standard three-source scan. The competency source is {@code
     * SkillCompetencyAbility#allFor}, not {@code character.getSkillCompetencyAbilities()} — a
     * Habilidade Racial grants RD/RA the same way an acquired one does, and scanning only the
     * acquired list silently dropped it ({@code GuamposRacialAbility#VIGOR_DE_EPONA}, "reduzem
     * em -1 todo dano sofrido", is the first racial ability to grant either).
     */
    private int sumAcrossSources(final Character character, final ModifierType modifierType) {
        int total = modifierResolver.sumModifiers(character.getAttributeAbilities(), modifierType);
        total += modifierResolver.sumModifiers(SkillCompetencyAbility.allFor(character), modifierType);
        for (Map.Entry<SkillType, CharacterSkill> entry : character.getSkills().entrySet()) {
            int graduationValue = entry.getValue().getGraduation().getGraduationValue();
            List<SkillExcellency> unlockedExcellencies = SkillExcellency.unlockedBy(
                    entry.getKey().getExcellencyClass(), graduationValue);
            total += modifierResolver.sumModifiers(unlockedExcellencies, modifierType);
        }
        return Math.max(0, total);
    }
}
