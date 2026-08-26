package org.aventyrs.core.monster;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.MagicPointsService;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <b>Fill in the form.</b> The {@link MonsterTemplate} for a unique foe — a named boss, a
 * one-off encounter, anything worth designing rather than grabbing off the shelf. The builder is
 * the form: every field of a stat block, with a sensible default where a blank is acceptable.
 *
 * <pre>{@code
 * MonsterSheet troll = AbstractMonsterTemplate.builder()
 *         .name("Troll da Ponte Velha")
 *         .attributeBase(AttributeDomain.VIGOR, 9)
 *         .attributeBase(AttributeDomain.STRENGTH, 8)
 *         .skillGraduation(SkillType.ATAQUE_CORPO_A_CORPO, 12)
 *         .sizeCategory(SizeCategory.PLUS_TWO)
 *         .physicalDefense(19)
 *         .magicDefense(13)
 *         .attackDifficulty(DifficultyLevel.HARD)
 *         .attackBonus(3)
 *         .lifeMultiplier(7)
 *         .build()
 *         .spawn(gm);
 * }</pre>
 *
 * <p>Note the Vigor of 9 and the Graduação of 12: both are past what a player character could
 * ever reach, and neither needs a special mechanism to allow. See {@link MonsterTemplate}'s own
 * javadoc for why.
 *
 * <p>This is the {@code AbstractItem} of the monster world — the caller-supplied, non-cataloged
 * variant sitting alongside {@link GenericMonster}'s fixed catalog. A foe that recurs across a
 * campaign is better off as a catalog constant; this is for the ones that don't.
 */
@Getter
@Builder
public class AbstractMonsterTemplate implements MonsterTemplate {

    @NonNull
    private final String name;

    @Singular("attributeBase")
    private final Map<AttributeDomain, Integer> attributeBases;

    @Singular("skillGraduation")
    private final Map<SkillType, Integer> skillGraduations;

    @Singular
    private final List<AttributeAbility> attributeAbilities;

    @Singular
    private final List<SkillCompetencyAbility> skillCompetencyAbilities;

    @Singular("equipmentItem")
    private final List<Item> equipment;

    /** Especializações per Perícia — builder call {@code .skillSpecialization(TYPE, List.of(…))}. */
    @Singular("skillSpecialization")
    private final Map<SkillType, List<SkillSpecialization>> skillSpecializations;

    @Builder.Default
    private final int actionPoints = ActionPointsService.DEFAULT_ACTION_POINTS;

    @Builder.Default
    private final boolean undead = false;

    /** Builder call {@code .criticalEffectImmunity(CriticalEffectType.SANGRAMENTO)}. */
    @Singular("criticalEffectImmunity")
    private final Set<CriticalEffectType> criticalEffectImmunities;

    @Builder.Default
    private final SizeCategory sizeCategory = SizeCategory.ZERO;

    private final int physicalDefense;

    private final int magicDefense;

    @Builder.Default
    @NonNull
    private final DifficultyLevel attackDifficulty = DifficultyLevel.MEDIUM;

    private final int attackBonus;

    @Builder.Default
    private final int lifeMultiplier = HitPointsService.DEFAULT_LIFE_MULTIPLIER;

    @Builder.Default
    private final int manaMultiplier = MagicPointsService.DEFAULT_MANA_MULTIPLIER;

    @Builder.Default
    private final int determinationMultiplier = DeterminationPointsService.DEFAULT_DETERMINATION_MULTIPLIER;
}
