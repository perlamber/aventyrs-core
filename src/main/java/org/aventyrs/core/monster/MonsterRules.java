package org.aventyrs.core.monster;

import lombok.NonNull;
import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.model.AbilityContext;
import org.aventyrs.core.monster.model.ChoiceSpec;
import org.aventyrs.core.monster.model.MonsterModel;
import org.aventyrs.core.monster.model.MonstrousAbility;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.aventyrs.core.monster.MonsterViolation.Code.ABILITY_MODEL_NOT_HELD;
import static org.aventyrs.core.monster.MonsterViolation.Code.ABILITY_TIER_TOO_HIGH;
import static org.aventyrs.core.monster.MonsterViolation.Code.ATTRIBUTE_BASE_OUT_OF_RANGE;
import static org.aventyrs.core.monster.MonsterViolation.Code.ATTRIBUTE_POINTS_EXCEEDED;
import static org.aventyrs.core.monster.MonsterViolation.Code.DUPLICATE_ABILITY;
import static org.aventyrs.core.monster.MonsterViolation.Code.DUPLICATE_MODEL;
import static org.aventyrs.core.monster.MonsterViolation.Code.EGO_POINTS_EXCEEDED;
import static org.aventyrs.core.monster.MonsterViolation.Code.EXEMPLAR_ONLY_TRAIT;
import static org.aventyrs.core.monster.MonsterViolation.Code.FEAT_NOT_ALLOWED;
import static org.aventyrs.core.monster.MonsterViolation.Code.GNOSE_UPGRADES_EXCEEDED;
import static org.aventyrs.core.monster.MonsterViolation.Code.INVALID_ABILITY_CHOICE;
import static org.aventyrs.core.monster.MonsterViolation.Code.MODEL_WITHOUT_STARTING_ABILITY;
import static org.aventyrs.core.monster.MonsterViolation.Code.NAME_BLANK;
import static org.aventyrs.core.monster.MonsterViolation.Code.NEGATIVE_AMOUNT;
import static org.aventyrs.core.monster.MonsterViolation.Code.NEGATIVE_POWER_DEGREE;
import static org.aventyrs.core.monster.MonsterViolation.Code.PROGRESSION_UPGRADES_EXCEEDED;
import static org.aventyrs.core.monster.MonsterViolation.Code.TOO_MANY_ABILITIES;
import static org.aventyrs.core.monster.MonsterViolation.Code.TOO_MANY_FEATS;
import static org.aventyrs.core.monster.MonsterViolation.Code.TOO_MANY_MODELS;
import static org.aventyrs.core.monster.MonsterViolation.Code.UPGRADE_ON_UNTRAINED_SKILL;

/**
 * {@code criacao-de-monstros.txt} as code: the budgets a {@link MonsterBlueprint} must stay inside
 * ({@link #validate}) and every number it derives (GDs, Defesas, PA, multipliers, Bônus Racial).
 *
 * <p>Pure functions of a blueprint — plus, for the numbers that move in play (a GD reading a Destreza
 * an Impulso just raised), the live {@link CombatantSheet}. {@link MonsterSheet} calls in here for
 * everything it presents, so a sheet and an editor preview can never disagree.
 *
 * <h2>How a Perícia's GD is built</h2>
 * <ol>
 *   <li>Start at the kind's base — Muito Fácil (Regular) or Fácil (Exemplar).</li>
 *   <li>If trained: +1 step for a Gnose upgrade, +1 per GP upgrade spent on it.</li>
 *   <li>Clamp to the Categoria's "GD Máximo em Perícia" (an Exemplar one step higher).</li>
 *   <li>Then the Habilidades' steps and the Mestre's — "Aprimoramentos Monstruosos podem fazer com
 *   que a GD máxima em perícia seja superada", so these come after the clamp.</li>
 *   <li>Bonus: half the governing Atributo, rounded down ("Monstros adicional apenas metade dos seus
 *   valores de Atributo como Modificadores nos GD"), plus Habilidade bonuses on that Perícia, plus
 *   the Mestre's.</li>
 * </ol>
 * An <b>untrained</b> Perícia takes step 1 and step 5 only — the text doesn't say what an untrained
 * monster presents, and the base GD is the least it can.
 *
 * <p>Defesas are the Esquiva e Aparar GD's value plus whatever {@link DefenseService} sums for the
 * sheet (equipment, Habilidades, temporary bonuses, Condições) — "As Defesas dos Monstros são
 * baseadas no GD de Esquiva e Aparar, somado aos Modificadores cedidos por Equipamentos e
 * Habilidades". The same base serves DF and DM: the text names only "Defesas".
 */
public final class MonsterRules {

    /** "Por padrão os Monstros possuem 3 Pontos de Ação". */
    public static final int BASE_ACTION_POINTS = ActionPointsService.DEFAULT_ACTION_POINTS;

    /** PD = Metade dos PV + (Instinto × 3). */
    public static final int DETERMINATION_MULTIPLIER = DeterminationPointsService.DEFAULT_DETERMINATION_MULTIPLIER;

    /** PM = Metade dos PV + (Foco × 2). */
    public static final int MANA_MULTIPLIER = 2;

    /** "Para cada 3 pontos em seu Grau de Poder um novo Aprimoramento Monstruoso" — one Habilidade. */
    public static final int ABILITY_POWER_DEGREE_STEP = 3;

    /** "Para cada 4 pontos … uma Perícia é aprimorada em GD +1 nível". */
    public static final int SKILL_UPGRADE_POWER_DEGREE_STEP = 4;

    /** "Para cada 5 pontos … um novo Talento Geral ou Monstruoso". */
    public static final int FEAT_POWER_DEGREE_STEP = 5;

    /** "Para cada 10 pontos … um novo ponto de Ego". */
    public static final int EGO_POWER_DEGREE_STEP = 10;

    private static final DefenseService DEFENSE_SERVICE = new DefenseServiceImpl();

    private MonsterRules() {
    }

    // ---- Budgets -------------------------------------------------------------------------------

    /** Atributo points spent above base 1, across every domain. */
    public static int attributePointsSpent(@NonNull final MonsterBlueprint blueprint) {
        int spent = 0;
        for (AttributeDomain domain : AttributeDomain.values()) {
            spent += blueprint.getAttributeBase(domain) - MonsterBlueprint.MINIMUM_ATTRIBUTE_BASE;
        }
        return spent;
    }

    /** One Habilidade per held Modelo, plus one per GP+3. */
    public static int abilityBudget(@NonNull final MonsterBlueprint blueprint) {
        return blueprint.getModels().size() + Math.max(0, blueprint.getPowerDegree()) / ABILITY_POWER_DEGREE_STEP;
    }

    /** How many Perícias may take the Gnose upgrade — the monster's Gnose, Bônus Racial included. */
    public static int gnoseUpgradeBudget(@NonNull final MonsterBlueprint blueprint) {
        return attributeTotal(blueprint, AttributeDomain.GNOSE);
    }

    /** One GD step per GP+4. */
    public static int progressionUpgradeBudget(@NonNull final MonsterBlueprint blueprint) {
        return Math.max(0, blueprint.getPowerDegree()) / SKILL_UPGRADE_POWER_DEGREE_STEP;
    }

    /**
     * One Talento per GP+5; an Exemplar two more, and one more per GP+6; plus any slot a held
     * Habilidade adds ("Recebe um Talento Geral ou Monstruoso adicional").
     */
    public static int featBudget(@NonNull final MonsterBlueprint blueprint) {
        int powerDegree = Math.max(0, blueprint.getPowerDegree());
        MonsterKind kind = blueprint.getKind();
        int budget = powerDegree / FEAT_POWER_DEGREE_STEP + kind.getBonusStartingFeats();
        if (kind.getExtraFeatPowerDegreeStep() > 0) {
            budget += powerDegree / kind.getExtraFeatPowerDegreeStep();
        }
        MonsterCategory category = blueprint.getCategory();
        for (MonstrousAbilitySelection selection : blueprint.getAbilities()) {
            budget += selection.ability().resolveBonusFeatSlots(blueprint.contextFor(selection));
        }
        return budget;
    }

    /**
     * The Talento categories a monster may take: Gerais, Monstruosos, and whatever a held Habilidade
     * opens ("um Talento Racial da raça escolhida"). Talentos a Habilidade grants outright are not
     * counted against the budget and are never in the blueprint's own list.
     */
    public static Set<FeatCategory> allowedFeatCategories(@NonNull final MonsterBlueprint blueprint) {
        Set<FeatCategory> allowed = new HashSet<>();
        for (FeatCategory category : FeatCategory.values()) {
            if (category.getType() == FeatCategory.Type.GERAL || category == FeatCategory.MONSTRUOSO) {
                allowed.add(category);
            }
        }
        MonsterCategory category = blueprint.getCategory();
        for (MonstrousAbilitySelection selection : blueprint.getAbilities()) {
            allowed.addAll(selection.ability().resolveAllowedFeatCategories(blueprint.contextFor(selection)));
        }
        return allowed;
    }

    /** Ego points to allocate — the Categoria's running total plus one per GP+10. */
    public static int egoPointBudget(@NonNull final MonsterBlueprint blueprint) {
        return blueprint.getCategory().getBonusEgoPoints()
                + Math.max(0, blueprint.getPowerDegree()) / EGO_POWER_DEGREE_STEP;
    }

    // ---- Validation ----------------------------------------------------------------------------

    /** Every way blueprint breaks the rules; empty when it is legal. {@link MonsterAdjustments} are never checked. */
    public static List<MonsterViolation> validate(@NonNull final MonsterBlueprint blueprint) {
        List<MonsterViolation> violations = new ArrayList<>();
        MonsterCategory category = blueprint.getCategory();

        if (blueprint.getName().isBlank()) {
            violations.add(MonsterViolation.of(NAME_BLANK));
        }
        if (blueprint.getPowerDegree() < 0) {
            violations.add(MonsterViolation.of(NEGATIVE_POWER_DEGREE, null, blueprint.getPowerDegree(), 0));
        }

        for (AttributeDomain domain : AttributeDomain.values()) {
            int base = blueprint.getAttributeBase(domain);
            if (base < MonsterBlueprint.MINIMUM_ATTRIBUTE_BASE || base > MonsterBlueprint.MAXIMUM_ATTRIBUTE_BASE) {
                violations.add(MonsterViolation.of(ATTRIBUTE_BASE_OUT_OF_RANGE, domain.name(), base,
                        MonsterBlueprint.MAXIMUM_ATTRIBUTE_BASE));
            }
        }
        int spent = attributePointsSpent(blueprint);
        if (spent > MonsterBlueprint.ATTRIBUTE_POINTS) {
            violations.add(MonsterViolation.of(ATTRIBUTE_POINTS_EXCEEDED, null, spent, MonsterBlueprint.ATTRIBUTE_POINTS));
        }

        validateModelsAndAbilities(blueprint, category, violations);
        validateSkills(blueprint, violations);
        validateFeats(blueprint, violations);
        validateEgos(blueprint, violations);
        validateExemplarTraits(blueprint, violations);
        return violations;
    }

    private static void validateModelsAndAbilities(final MonsterBlueprint blueprint, final MonsterCategory category,
                                                   final List<MonsterViolation> violations) {
        List<MonsterModel> models = blueprint.getModels();
        Set<MonsterModel> seenModels = new HashSet<>();
        for (MonsterModel model : models) {
            if (!seenModels.add(model)) {
                violations.add(MonsterViolation.of(DUPLICATE_MODEL, model.name()));
            }
        }
        if (seenModels.size() > category.getMaximumModels()) {
            violations.add(MonsterViolation.of(TOO_MANY_MODELS, null, seenModels.size(), category.getMaximumModels()));
        }

        Set<MonstrousAbility> seenAbilities = new HashSet<>();
        for (MonstrousAbilitySelection selection : blueprint.getAbilities()) {
            MonstrousAbility ability = selection.ability();
            if (!seenAbilities.add(ability)) {
                violations.add(MonsterViolation.of(DUPLICATE_ABILITY, ability.name()));
            }
            if (!seenModels.contains(ability.getModel())) {
                violations.add(MonsterViolation.of(ABILITY_MODEL_NOT_HELD, ability.name()));
            }
            if (!category.isAtLeast(ability.getTier())) {
                violations.add(MonsterViolation.of(ABILITY_TIER_TOO_HIGH, ability.name()));
            }
            validateChoices(selection, category, violations);
        }
        for (MonsterModel model : seenModels) {
            boolean hasStarting = seenAbilities.stream()
                    .anyMatch(ability -> ability.getModel() == model && ability.getTier() == MonsterCategory.PRESA);
            if (!hasStarting) {
                violations.add(MonsterViolation.of(MODEL_WITHOUT_STARTING_ABILITY, model.name()));
            }
        }
        int budget = abilityBudget(blueprint);
        if (blueprint.getAbilities().size() > budget) {
            violations.add(MonsterViolation.of(TOO_MANY_ABILITIES, null, blueprint.getAbilities().size(), budget));
        }
    }

    /**
     * Every spec the Habilidade asks at category takes exactly its count of distinct picks, each one
     * of its options; a pick under a spec it doesn't ask is refused too. {@code actual}/{@code limit}
     * report the picks made against the count asked.
     */
    private static void validateChoices(final MonstrousAbilitySelection selection, final MonsterCategory category,
                                        final List<MonsterViolation> violations) {
        MonstrousAbility ability = selection.ability();
        List<ChoiceSpec> specs = ability.getChoiceSpecs(category);
        Set<String> asked = new HashSet<>();
        for (ChoiceSpec spec : specs) {
            asked.add(spec.id());
            List<String> picks = selection.choices().getOrDefault(spec.id(), List.of());
            boolean valid = picks.size() == spec.count()
                    && new HashSet<>(picks).size() == picks.size()
                    && spec.options().containsAll(picks);
            if (!valid) {
                violations.add(MonsterViolation.of(INVALID_ABILITY_CHOICE, ability.name(), picks.size(), spec.count()));
            }
        }
        for (String id : selection.choices().keySet()) {
            if (!asked.contains(id) && !selection.choices().get(id).isEmpty()) {
                violations.add(MonsterViolation.of(INVALID_ABILITY_CHOICE, ability.name(), selection.choices().get(id).size(), 0));
            }
        }
    }

    private static void validateSkills(final MonsterBlueprint blueprint, final List<MonsterViolation> violations) {
        Set<SkillType> trained = blueprint.getTrainedSkills();
        for (SkillType skill : blueprint.getGnoseUpgrades()) {
            if (!trained.contains(skill)) {
                violations.add(MonsterViolation.of(UPGRADE_ON_UNTRAINED_SKILL, skill.name()));
            }
        }
        int gnoseBudget = gnoseUpgradeBudget(blueprint);
        if (blueprint.getGnoseUpgrades().size() > gnoseBudget) {
            violations.add(MonsterViolation.of(GNOSE_UPGRADES_EXCEEDED, null, blueprint.getGnoseUpgrades().size(), gnoseBudget));
        }

        int progressionSpent = 0;
        for (Map.Entry<SkillType, Integer> entry : blueprint.getProgressionUpgrades().entrySet()) {
            if (entry.getValue() < 0) {
                violations.add(MonsterViolation.of(NEGATIVE_AMOUNT, entry.getKey().name(), entry.getValue(), 0));
                continue;
            }
            if (entry.getValue() > 0 && !trained.contains(entry.getKey())) {
                violations.add(MonsterViolation.of(UPGRADE_ON_UNTRAINED_SKILL, entry.getKey().name()));
            }
            progressionSpent += entry.getValue();
        }
        int progressionBudget = progressionUpgradeBudget(blueprint);
        if (progressionSpent > progressionBudget) {
            violations.add(MonsterViolation.of(PROGRESSION_UPGRADES_EXCEEDED, null, progressionSpent, progressionBudget));
        }
    }

    private static void validateFeats(final MonsterBlueprint blueprint, final List<MonsterViolation> violations) {
        Set<FeatCategory> allowed = allowedFeatCategories(blueprint);
        for (Feat feat : blueprint.getFeats()) {
            if (!allowed.contains(feat.getFeatCategory())) {
                violations.add(MonsterViolation.of(FEAT_NOT_ALLOWED, feat.toString()));
            }
        }
        int budget = featBudget(blueprint);
        if (blueprint.getFeats().size() > budget) {
            violations.add(MonsterViolation.of(TOO_MANY_FEATS, null, blueprint.getFeats().size(), budget));
        }
    }

    private static void validateEgos(final MonsterBlueprint blueprint, final List<MonsterViolation> violations) {
        int allocated = 0;
        for (Map.Entry<EgoDomain, Integer> entry : blueprint.getEgoAllocation().entrySet()) {
            if (entry.getValue() < 0) {
                violations.add(MonsterViolation.of(NEGATIVE_AMOUNT, entry.getKey().name(), entry.getValue(), 0));
                continue;
            }
            allocated += entry.getValue();
        }
        int budget = egoPointBudget(blueprint);
        if (allocated > budget) {
            violations.add(MonsterViolation.of(EGO_POINTS_EXCEEDED, null, allocated, budget));
        }
    }

    private static void validateExemplarTraits(final MonsterBlueprint blueprint, final List<MonsterViolation> violations) {
        if (blueprint.getKind().mayHoldCharacterTraits()) {
            return;
        }
        if (!blueprint.getAttributeAbilities().isEmpty()) {
            violations.add(MonsterViolation.of(EXEMPLAR_ONLY_TRAIT, "ATTRIBUTE_ABILITIES"));
        }
        if (!blueprint.getSkillCompetencyAbilities().isEmpty()) {
            violations.add(MonsterViolation.of(EXEMPLAR_ONLY_TRAIT, "SKILL_COMPETENCY_ABILITIES"));
        }
        if (blueprint.getSkillSpecializations().values().stream().anyMatch(list -> !list.isEmpty())) {
            violations.add(MonsterViolation.of(EXEMPLAR_ONLY_TRAIT, "SKILL_SPECIALIZATIONS"));
        }
        if (!blueprint.getEgoAdvantages().isEmpty()) {
            violations.add(MonsterViolation.of(EXEMPLAR_ONLY_TRAIT, "EGO_ADVANTAGES"));
        }
        if (blueprint.getFamaPositiva() != 0 || blueprint.getFamaNegativa() != 0) {
            violations.add(MonsterViolation.of(EXEMPLAR_ONLY_TRAIT, "FAMA"));
        }
    }

    // ---- Derived numbers -----------------------------------------------------------------------

    /**
     * The Bônus Racial its Habilidades grant domain, trimmed so base + racial never passes the
     * Categoria's "Atributo Base Máximo" (an Exemplar's one higher).
     */
    public static int racialBonus(@NonNull final MonsterBlueprint blueprint, @NonNull final AttributeDomain domain) {
        MonsterCategory category = blueprint.getCategory();
        int granted = 0;
        for (MonstrousAbilitySelection selection : blueprint.getAbilities()) {
            granted += selection.ability().resolveRacialAttributeBonuses(blueprint.contextFor(selection)).getOrDefault(domain, 0);
        }
        int ceiling = category.getMaximumAttribute() + blueprint.getKind().getBonusMaximumAttribute();
        int base = blueprint.getAttributeBase(domain);
        return Math.max(0, Math.min(granted, ceiling - base));
    }

    /** Base plus Bônus Racial — the permanent total, before anything live on a sheet. */
    public static int attributeTotal(@NonNull final MonsterBlueprint blueprint, @NonNull final AttributeDomain domain) {
        return blueprint.getAttributeBase(domain) + racialBonus(blueprint, domain);
    }

    /** "Por padrão os Monstros possuem 3 Pontos de Ação", + the Categoria's bonus, + the Mestre's. Habilidades add theirs through {@code ActionPointsService}. */
    public static int fixedActionPoints(@NonNull final MonsterBlueprint blueprint) {
        return BASE_ACTION_POINTS + blueprint.getCategory().getBonusActionPoints()
                + blueprint.getAdjustments().getActionPoints();
    }

    public static int lifeMultiplier(@NonNull final MonsterBlueprint blueprint) {
        return blueprint.getCategory().getLifeMultiplier() + blueprint.getAdjustments().getLifeMultiplier();
    }

    public static int manaMultiplier(@NonNull final MonsterBlueprint blueprint) {
        return MANA_MULTIPLIER + blueprint.getAdjustments().getManaMultiplier();
    }

    public static int determinationMultiplier(@NonNull final MonsterBlueprint blueprint) {
        return DETERMINATION_MULTIPLIER + blueprint.getAdjustments().getDeterminationMultiplier();
    }

    /**
     * The GD skill presents, read off the blueprint alone — the editor's preview. A live monster
     * asks {@link MonsterSheet#getSkillDifficulty}, which sees what its sheet holds (an Impulso's
     * Destreza).
     */
    public static SkillDifficulty skillDifficulty(@NonNull final MonsterBlueprint blueprint, @NonNull final SkillType skill) {
        return skillDifficulty(blueprint, skill, attributeTotalFor(blueprint, skill));
    }

    /** The GD skill presents on a live monster — the governing Atributo read from its sheet. */
    public static SkillDifficulty skillDifficulty(@NonNull final MonsterBlueprint blueprint, @NonNull final SkillType skill,
                                                  @NonNull final Character character, final CombatantSheet sheet) {
        AttributeDomain domain = governingAttribute(skill);
        int attribute = domain == null ? 0 : character.getEffectiveAttributeTotal(domain, sheet);
        SkillDifficulty atRest = skillDifficulty(blueprint, skill, attribute);
        if (sheet == null) {
            return atRest;
        }
        // What the sheet holds right now: a timed GD shift (Mimetizar Competência) and timed roll
        // bonuses (Domínio dos Céus' "+2 enquanto voando") — a foe's roll bonus is its GD's bonus.
        int steps = sheet.getSkillDifficultyShift(skill);
        int bonus = sheet.getTemporaryBonus(skill.getRollBonusType()) + sheet.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS);
        return SkillDifficulty.of(atRest.level().shift(steps), atRest.bonus() + bonus);
    }

    private static SkillDifficulty skillDifficulty(final MonsterBlueprint blueprint, final SkillType skill,
                                                   final int governingAttributeTotal) {
        MonsterCategory category = blueprint.getCategory();
        MonsterKind kind = blueprint.getKind();
        AttributeDomain domain = governingAttribute(skill);

        DifficultyLevel level = kind.getBaseSkillLevel();
        if (blueprint.getTrainedSkills().contains(skill)) {
            int steps = (blueprint.getGnoseUpgrades().contains(skill) ? 1 : 0) + blueprint.getProgressionUpgrades(skill);
            level = level.harder(steps);
            DifficultyLevel ceiling = category.getMaximumSkillLevel().harder(kind.getBonusMaximumSkillSteps());
            if (level.compareTo(ceiling) > 0) {
                level = ceiling;
            }
        }

        int abilitySteps = 0;
        int abilityBonus = 0;
        for (MonstrousAbilitySelection selection : blueprint.getAbilities()) {
            MonstrousAbility ability = selection.ability();
            AbilityContext context = blueprint.contextFor(selection);
            abilitySteps += ability.resolveSkillLevelShift(skill, domain, context);
            abilityBonus += ability.resolveModifier(skill.getRollBonusType(), context)
                    + ability.resolveModifier(ModifierType.SKILL_ROLL_BONUS, context);
        }
        MonsterAdjustments adjustments = blueprint.getAdjustments();
        level = level.shift(abilitySteps + adjustments.getSkillLevelShift(skill));

        int bonus = governingAttributeTotal / 2 + abilityBonus + adjustments.getSkillBonus(skill);
        return SkillDifficulty.of(level, bonus);
    }

    /** Every Perícia's GD, read off the blueprint alone. */
    public static Map<SkillType, SkillDifficulty> skillDifficulties(@NonNull final MonsterBlueprint blueprint) {
        Map<SkillType, SkillDifficulty> result = new EnumMap<>(SkillType.class);
        for (SkillType skill : SkillType.values()) {
            result.put(skill, skillDifficulty(blueprint, skill));
        }
        return result;
    }

    /**
     * Its DF or DM on a live monster: the Esquiva e Aparar GD's value, plus {@link DefenseService}'s
     * modifiers for the sheet, plus the Mestre's.
     */
    public static int defense(@NonNull final MonsterBlueprint blueprint, @NonNull final CombatantSheet sheet,
                              @NonNull final DefenseType defenseType) {
        return skillDifficulty(blueprint, SkillType.ESQUIVA_E_APARAR, sheet.getCharacter(), sheet).getValue()
                + DEFENSE_SERVICE.getTotalDefense(sheet, defenseType)
                + blueprint.getAdjustments().getDefense(defenseType);
    }

    /** The Atributo governing skill, or {@code null} for one governed by none. */
    public static AttributeDomain governingAttribute(@NonNull final SkillType skill) {
        return skill.newSkillInstance().getAttributeDomain();
    }

    private static int attributeTotalFor(final MonsterBlueprint blueprint, final SkillType skill) {
        AttributeDomain domain = governingAttribute(skill);
        return domain == null ? 0 : attributeTotal(blueprint, domain);
    }
}
