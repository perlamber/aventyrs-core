package org.aventyrs.core.monster.generator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.FeatCatalog;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.monster.MonsterBlueprint;
import org.aventyrs.core.monster.MonsterCategory;
import org.aventyrs.core.monster.MonsterKind;
import org.aventyrs.core.monster.MonsterRules;
import org.aventyrs.core.monster.MonsterViolation;
import org.aventyrs.core.monster.MonstrousAbilitySelection;
import org.aventyrs.core.monster.model.ChoiceSpec;
import org.aventyrs.core.monster.model.ImplementationStatus;
import org.aventyrs.core.monster.model.MonstrousAbility;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;

/**
 * A plain default monster per {@code criacao-de-monstros.txt}, from a {@link ConjurePreset}, a Grau de
 * Poder and a {@link MonsterKind}. This is what a Mestre conjures straight into a Cena (table ruling,
 * 2026-10-15).
 *
 * <p>It walks the manual's own creation steps. Every budget comes from {@link MonsterRules}, so the
 * result is always legal ({@link MonsterRules#validate} is empty):
 * <ol>
 *   <li><b>Categoria</b> from the GP ({@link MonsterCategory#forPowerDegree}).</li>
 *   <li><b>One Modelo</b>, the preset's own. It is a <i>plain</i> monster, so no second one.</li>
 *   <li><b>Atributos</b> from the preset, with a seeded nudge of a point or two between its strongest ones.</li>
 *   <li><b>Habilidades Monstruosas</b>:
 *     <ul>
 *       <li>a Presa one first, then fill {@link MonsterRules#abilityBudget}, lowest tier first;</li>
 *       <li>within a tier, {@link ImplementationStatus#APPLIED} before {@code PARTIAL};</li>
 *       <li>never {@code TABLE_ONLY} (unless that is the only Presa start);</li>
 *       <li>each {@link ChoiceSpec} is picked by the seed.</li>
 *     </ul></li>
 *   <li><b>Perícias</b>: train the preset's primary ones. Gnose upgrades go to them in order. GP upgrades go
 *       round-robin up to the Categoria's ceiling, opening the secondary ones once the primary are capped.</li>
 *   <li><b>Talentos</b>, only from the preset's trees. Each must be eligible and need no acquisition-time
 *       choice, up to {@link MonsterRules#featBudget}.</li>
 *   <li><b>Egos</b> dealt one at a time in the preset's order, up to {@link MonsterRules#egoPointBudget}.</li>
 * </ol>
 *
 * <p><b>Deterministic for a seed</b>: the same inputs give an equal blueprint, so a client preview
 * and the server's conjure agree, and a "reroll" is just a new seed. It never rolls dice. The seed
 * only chooses among legal options.
 */
public final class MonsterGenerator {

    /** Attribute points the seed may move between the preset's strongest Atributos (0 up to this). */
    static final int MAXIMUM_ATTRIBUTE_NUDGES = 2;

    /** How many of the preset's highest Atributos a nudge may touch. */
    private static final int NUDGEABLE_ATTRIBUTES = 4;

    private MonsterGenerator() {
    }

    /**
     * The blueprint for one conjured monster.
     *
     * @param name the monster's name, or {@code null}/blank for the preset's default name
     * @throws IllegalArgumentException for a negative GP
     * @throws IllegalStateException    if the result would break the rules, which the tests rule out
     */
    public static MonsterBlueprint generate(@NonNull final ConjurePreset preset, final int powerDegree,
                                            @NonNull final MonsterKind kind, final String name, final long seed) {
        if (powerDegree < 0) {
            throw new IllegalArgumentException("Grau de Poder must not be negative: " + powerDegree);
        }
        Random random = new Random(seed);
        MonsterCategory category = MonsterCategory.forPowerDegree(powerDegree);

        MonsterBlueprint.MonsterBlueprintBuilder builder = MonsterBlueprint.builder()
                .name(name == null || name.isBlank() ? preset.getDefaultName() : name.strip())
                .powerDegree(powerDegree)
                .kind(kind)
                .sizeCategory(preset.getSizeCategory())
                .model(preset.getModel());
        attributes(preset, random).forEach(builder::attributeBase);
        abilities(preset, category, powerDegree, random).forEach(builder::ability);

        MonsterBlueprint draft = builder.build();
        draft = withSkills(draft, preset);
        draft = withFeats(draft, preset, random);
        draft = withEgos(draft, preset);

        List<MonsterViolation> violations = MonsterRules.validate(draft);
        if (!violations.isEmpty()) {
            throw new IllegalStateException("Generated " + preset + " GP " + powerDegree + " " + kind
                    + " breaks the rules: " + violations);
        }
        return draft;
    }

    // ---- Atributos ---------------------------------------------------------------------------

    private static Map<AttributeDomain, Integer> attributes(final ConjurePreset preset, final Random random) {
        Map<AttributeDomain, Integer> bases = new EnumMap<>(preset.getAttributeBases());
        List<AttributeDomain> strongest = bases.keySet().stream()
                .sorted(Comparator.comparing((AttributeDomain domain) -> bases.get(domain)).reversed()
                        .thenComparing(Enum::ordinal))
                .limit(NUDGEABLE_ATTRIBUTES)
                .toList();
        int nudges = random.nextInt(MAXIMUM_ATTRIBUTE_NUDGES + 1);
        for (int i = 0; i < nudges; i++) {
            AttributeDomain from = strongest.get(random.nextInt(strongest.size()));
            AttributeDomain to = strongest.get(random.nextInt(strongest.size()));
            if (from != to && bases.get(from) > MonsterBlueprint.MINIMUM_ATTRIBUTE_BASE
                    && bases.get(to) < MonsterBlueprint.MAXIMUM_ATTRIBUTE_BASE) {
                bases.merge(from, -1, Integer::sum);
                bases.merge(to, 1, Integer::sum);
            }
        }
        return bases;
    }

    // ---- Habilidades Monstruosas -------------------------------------------------------------

    private static List<MonstrousAbilitySelection> abilities(final ConjurePreset preset, final MonsterCategory category,
                                                             final int powerDegree, final Random random) {
        int budget = 1 + powerDegree / MonsterRules.ABILITY_POWER_DEGREE_STEP;
        List<MonstrousAbility> legal = preset.getModel().getAbilities().stream()
                .filter(ability -> category.isAtLeast(ability.getTier()))
                .filter(ability -> pickable(ability, category))
                .toList();

        List<MonstrousAbility> starters = legal.stream()
                .filter(ability -> ability.getTier() == MonsterCategory.PRESA)
                .filter(ability -> ability.getImplementationStatus() != ImplementationStatus.TABLE_ONLY)
                .toList();
        if (starters.isEmpty()) {
            starters = legal.stream().filter(ability -> ability.getTier() == MonsterCategory.PRESA).toList();
        }
        if (starters.isEmpty()) {
            throw new IllegalStateException(preset.getModel() + " has no Presa Habilidade a monster can start with");
        }
        MonstrousAbility starter = starters.get(random.nextInt(starters.size()));

        // Shuffle first, then sort stably by (tier, status): the seed decides only among equals.
        List<MonstrousAbility> rest = new ArrayList<>(legal);
        rest.remove(starter);
        rest.removeIf(ability -> ability.getImplementationStatus() == ImplementationStatus.TABLE_ONLY);
        java.util.Collections.shuffle(rest, random);
        rest.sort(Comparator.comparing((MonstrousAbility ability) -> ability.getTier().ordinal())
                .thenComparing(ability -> ability.getImplementationStatus() == ImplementationStatus.APPLIED ? 0 : 1));

        List<MonstrousAbilitySelection> selections = new ArrayList<>();
        selections.add(select(starter, category, random));
        for (MonstrousAbility ability : rest) {
            if (selections.size() >= budget) {
                break;
            }
            selections.add(select(ability, category, random));
        }
        return selections;
    }

    /** Whether every pick it asks at category has enough options to be made at all. */
    private static boolean pickable(final MonstrousAbility ability, final MonsterCategory category) {
        return ability.getChoiceSpecs(category).stream().allMatch(spec -> spec.options().size() >= spec.count());
    }

    private static MonstrousAbilitySelection select(final MonstrousAbility ability, final MonsterCategory category,
                                                    final Random random) {
        List<ChoiceSpec> specs = ability.getChoiceSpecs(category);
        if (specs.isEmpty()) {
            return MonstrousAbilitySelection.of(ability);
        }
        Map<String, List<String>> picks = new LinkedHashMap<>();
        for (ChoiceSpec spec : specs) {
            List<String> options = new ArrayList<>(spec.options());
            java.util.Collections.shuffle(options, random);
            picks.put(spec.id(), List.copyOf(options.subList(0, spec.count())));
        }
        return MonstrousAbilitySelection.of(ability, picks);
    }

    // ---- Perícias ------------------------------------------------------------------------------

    private static MonsterBlueprint withSkills(final MonsterBlueprint draft, final ConjurePreset preset) {
        MonsterCategory category = draft.getCategory();
        MonsterKind kind = draft.getKind();
        DifficultyLevel ceiling = category.getMaximumSkillLevel().harder(kind.getBonusMaximumSkillSteps());
        int headroom = Math.max(0, ceiling.ordinal() - kind.getBaseSkillLevel().ordinal());

        Set<SkillType> trained = new LinkedHashSet<>(preset.getPrimarySkills());
        int gnoseBudget = MonsterRules.gnoseUpgradeBudget(draft);
        Set<SkillType> gnoseUpgrades = new LinkedHashSet<>();
        for (SkillType skill : trained) {
            if (gnoseUpgrades.size() < gnoseBudget && headroom > 0) {
                gnoseUpgrades.add(skill);
            }
        }

        Map<SkillType, Integer> progression = new LinkedHashMap<>();
        int remaining = MonsterRules.progressionUpgradeBudget(draft);
        List<SkillType> secondary = new ArrayList<>(preset.getSecondarySkills());
        while (remaining > 0) {
            boolean spent = false;
            for (SkillType skill : trained) {
                int room = headroom - (gnoseUpgrades.contains(skill) ? 1 : 0) - progression.getOrDefault(skill, 0);
                if (remaining > 0 && room > 0) {
                    progression.merge(skill, 1, Integer::sum);
                    remaining--;
                    spent = true;
                }
            }
            if (!spent) {
                if (secondary.isEmpty() || headroom == 0) {
                    break;
                }
                SkillType opened = secondary.remove(0);
                trained.add(opened);
                if (gnoseUpgrades.size() < gnoseBudget) {
                    gnoseUpgrades.add(opened);
                }
            }
        }

        MonsterBlueprint.MonsterBlueprintBuilder builder = draft.toBuilder()
                .clearTrainedSkills().trainedSkills(trained)
                .clearGnoseUpgrades().gnoseUpgrades(gnoseUpgrades)
                .clearProgressionUpgrades();
        progression.forEach(builder::progressionUpgrade);
        return builder.build();
    }

    // ---- Talentos ------------------------------------------------------------------------------

    private static MonsterBlueprint withFeats(final MonsterBlueprint draft, final ConjurePreset preset,
                                              final Random random) {
        int budget = MonsterRules.featBudget(draft);
        if (budget <= 0) {
            return draft;
        }
        Set<FeatCategory> allowed = MonsterRules.allowedFeatCategories(draft);
        Character character = draft.buildCharacter();

        List<Feat> picked = new ArrayList<>();
        for (FeatCategory tree : preset.getFeatCategories()) {
            if (!allowed.contains(tree)) {
                continue;
            }
            List<Feat> candidates = new ArrayList<>(FeatCatalog.in(tree));
            java.util.Collections.shuffle(candidates, random);
            for (Feat feat : candidates) {
                if (picked.size() >= budget) {
                    break;
                }
                if (offerable(feat, character, picked)) {
                    picked.add(feat);
                }
            }
        }
        return picked.isEmpty() ? draft : draft.toBuilder().feats(picked).build();
    }

    private static boolean offerable(final Feat feat, final Character character, final List<Feat> picked) {
        return !feat.isAcquirableOnlyAtCreation()
                && !picked.contains(feat)
                && character.getFeats().stream().noneMatch(held -> held.catalogEntry() == feat)
                && feat.resolveRequiredChoices(character).isEmpty()
                && feat.isEligible(character);
    }

    // ---- Egos ----------------------------------------------------------------------------------

    private static MonsterBlueprint withEgos(final MonsterBlueprint draft, final ConjurePreset preset) {
        int budget = MonsterRules.egoPointBudget(draft);
        if (budget <= 0) {
            return draft;
        }
        Map<EgoDomain, Integer> allocation = new EnumMap<>(EgoDomain.class);
        List<EgoDomain> order = preset.getEgoOrder();
        for (int i = 0; i < budget; i++) {
            allocation.merge(order.get(i % order.size()), 1, Integer::sum);
        }
        MonsterBlueprint.MonsterBlueprintBuilder builder = draft.toBuilder().clearEgoAllocation();
        allocation.forEach(builder::egoPoint);
        return builder.build();
    }
}
