package org.aventyrs.core.monster.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.aventyrs.core.monster.MonsterBlueprint;
import org.aventyrs.core.monster.MonsterCategory;
import org.aventyrs.core.monster.MonsterKind;
import org.aventyrs.core.monster.MonsterRules;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.monster.MonstrousAbilitySelection;
import org.aventyrs.core.monster.model.ImplementationStatus;
import org.aventyrs.core.monster.model.MonsterModel;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonsterGeneratorTest {

    private static final int[] POWER_DEGREES = {0, 1, 5, 12, 20, 26, 35, 46, 61, 80};

    private static final long[] SEEDS = {0L, 1L, 42L, 20261015L, -7L};

    static Stream<Arguments> matrix() {
        List<Arguments> rows = new ArrayList<>();
        for (ConjurePreset preset : ConjurePreset.values()) {
            for (int powerDegree : POWER_DEGREES) {
                for (MonsterKind kind : MonsterKind.values()) {
                    for (long seed : SEEDS) {
                        rows.add(Arguments.of(preset, powerDegree, kind, seed));
                    }
                }
            }
        }
        return rows.stream();
    }

    @ParameterizedTest(name = "{0} GP {1} {2} seed {3}")
    @MethodSource("matrix")
    void everyPresetGpKindAndSeedIsLegalAndSpawns(ConjurePreset preset, int powerDegree, MonsterKind kind, long seed) {
        MonsterBlueprint blueprint = MonsterGenerator.generate(preset, powerDegree, kind, null, seed);

        assertTrue(MonsterRules.validate(blueprint).isEmpty(), () -> MonsterRules.validate(blueprint).toString());
        MonsterSheet sheet = blueprint.spawn(new Player());
        assertNotNull(sheet.getCharacter());
        assertEquals(List.of(preset.getModel()), blueprint.getModels());
    }

    @ParameterizedTest(name = "{0} GP {1} {2} seed {3}")
    @MethodSource("matrix")
    void abilitiesStayInsideTheModelAndTheCategory(ConjurePreset preset, int powerDegree, MonsterKind kind, long seed) {
        MonsterBlueprint blueprint = MonsterGenerator.generate(preset, powerDegree, kind, null, seed);
        MonsterCategory category = MonsterCategory.forPowerDegree(powerDegree);

        assertFalse(blueprint.getAbilities().isEmpty());
        assertEquals(MonsterCategory.PRESA, blueprint.getAbilities().get(0).ability().getTier());
        for (MonstrousAbilitySelection selection : blueprint.getAbilities()) {
            assertEquals(preset.getModel(), selection.ability().getModel());
            assertTrue(category.isAtLeast(selection.ability().getTier()));
        }
        assertTrue(blueprint.getAbilities().size() <= MonsterRules.abilityBudget(blueprint));
    }

    @Test
    void theSameSeedGivesAnEqualBlueprint() {
        for (ConjurePreset preset : ConjurePreset.values()) {
            MonsterBlueprint first = MonsterGenerator.generate(preset, 30, MonsterKind.REGULAR, "Igual", 99L);
            MonsterBlueprint second = MonsterGenerator.generate(preset, 30, MonsterKind.REGULAR, "Igual", 99L);
            assertEquals(summary(first), summary(second), preset.name());
        }
    }

    @Test
    void differentSeedsVaryTheMonster() {
        long distinct = Stream.iterate(0L, seed -> seed + 1).limit(20)
                .map(seed -> summary(MonsterGenerator.generate(ConjurePreset.ALMA_ELEMENTAL, 30, MonsterKind.REGULAR, null, seed)))
                .distinct()
                .count();
        assertTrue(distinct > 1);
    }

    @Test
    void aBlankNameFallsBackToThePresetsDefault() {
        assertEquals("Mutante Monstruoso",
                MonsterGenerator.generate(ConjurePreset.MUTANTE_MONSTRUOSO, 3, MonsterKind.REGULAR, "  ", 1L).getName());
        assertEquals("Rato",
                MonsterGenerator.generate(ConjurePreset.MUTANTE_MONSTRUOSO, 3, MonsterKind.REGULAR, " Rato ", 1L).getName());
    }

    @Test
    void theAbilityCountFollowsTheBudgetWhileTheModelHasAbilitiesLeft() {
        // GP 0: one Habilidade (the Modelo's). GP 3: two.
        assertEquals(1, MonsterGenerator.generate(ConjurePreset.BESTA_ALADA, 0, MonsterKind.REGULAR, null, 1L)
                .getAbilities().size());
        assertEquals(2, MonsterGenerator.generate(ConjurePreset.BESTA_ALADA, 3, MonsterKind.REGULAR, null, 1L)
                .getAbilities().size());
    }

    @Test
    void neverPicksATableOnlyAbilityBeyondTheStart() {
        for (ConjurePreset preset : ConjurePreset.values()) {
            MonsterBlueprint blueprint = MonsterGenerator.generate(preset, 80, MonsterKind.EXEMPLAR, null, 3L);
            blueprint.getAbilities().stream().skip(1).forEach(selection -> assertNotEquals(
                    ImplementationStatus.TABLE_ONLY, selection.ability().getImplementationStatus(),
                    selection.ability().name()));
        }
    }

    @Test
    void talentosAndEgosAreSpentWhenTheBudgetAllows() {
        for (ConjurePreset preset : ConjurePreset.values()) {
            MonsterBlueprint blueprint = MonsterGenerator.generate(preset, 40, MonsterKind.REGULAR, null, 5L);
            assertFalse(blueprint.getFeats().isEmpty(), preset.name());
            assertEquals(MonsterRules.egoPointBudget(blueprint),
                    blueprint.getEgoAllocation().values().stream().mapToInt(Integer::intValue).sum(), preset.name());
        }
    }

    @Test
    void everyModelHasAPreset() {
        for (MonsterModel model : MonsterModel.values()) {
            assertEquals(model, ConjurePreset.of(model).getModel());
        }
    }

    @Test
    void everyPresetSpendsExactlyTheAttributePoints() {
        for (ConjurePreset preset : ConjurePreset.values()) {
            int spent = preset.getAttributeBases().values().stream()
                    .mapToInt(base -> base - MonsterBlueprint.MINIMUM_ATTRIBUTE_BASE).sum();
            assertEquals(MonsterBlueprint.ATTRIBUTE_POINTS, spent, preset.name());
        }
    }

    @Test
    void aNegativePowerDegreeIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> MonsterGenerator.generate(ConjurePreset.ALMA_ELEMENTAL, -1, MonsterKind.REGULAR, null, 1L));
    }

    /** Every choice a blueprint holds. It has no equals of its own. */
    private static List<Object> summary(MonsterBlueprint blueprint) {
        return List.of(blueprint.getName(), blueprint.getPowerDegree(), blueprint.getKind(),
                blueprint.getAttributeBases(), blueprint.getAbilities(), blueprint.getTrainedSkills(),
                blueprint.getGnoseUpgrades(), blueprint.getProgressionUpgrades(), blueprint.getFeats(),
                blueprint.getEgoAllocation(), blueprint.getEquipment());
    }
}
