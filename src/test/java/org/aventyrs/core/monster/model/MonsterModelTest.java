package org.aventyrs.core.monster.model;

import org.aventyrs.core.monster.MonsterCategory;
import org.aventyrs.core.monster.model.almaelemental.AlmaElementalAbility;
import org.aventyrs.core.monster.model.cireneia.CireneiaAbility;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonsterModelTest {

    @Test
    void everyModeloHasEightHabilidadesThatPointBackToIt() {
        assertEquals(7, MonsterModel.values().length);
        for (MonsterModel model : MonsterModel.values()) {
            List<MonstrousAbility> abilities = model.getAbilities();
            assertEquals(8, abilities.size(), model.name());
            for (MonstrousAbility ability : abilities) {
                assertEquals(model, ability.getModel(), ability.name());
                assertFalse(ability.getDisplayName().isBlank(), ability.name());
                assertFalse(ability.getDescription().isBlank(), ability.name());
            }
        }
    }

    /**
     * Each Habilidade's description is hand-copied from {@code docs/rules/criacao-de-monstros.txt}; this keeps
     * the two in step when the manual is revised. Bullets and whitespace are ignored — the .txt import drops
     * the former and reflows the latter.
     */
    @Test
    void everyDescriptionIsVerbatimInTheRulesText() throws IOException {
        String rules = normalise(Files.readString(Path.of("docs/rules/criacao-de-monstros.txt")));
        for (MonsterModel model : MonsterModel.values()) {
            for (MonstrousAbility ability : model.getAbilities()) {
                assertTrue(rules.contains(normalise(ability.getDescription())),
                        model.name() + "." + ability.name() + " drifted from the rules text");
            }
        }
    }

    private static String normalise(final String text) {
        return text.replace('\u00a0', ' ').replace("\u200b", "").replace("•", "").replaceAll("\\s+", " ").trim();
    }

    @Test
    void everyModeloOffersTwoPresaHabilidadesToStartWith() {
        for (MonsterModel model : MonsterModel.values()) {
            long presa = model.getAbilities().stream().filter(a -> a.getTier() == MonsterCategory.PRESA).count();
            assertEquals(2, presa, model.name());
        }
    }

    @Test
    void constantNamesAreUniqueWithinAModelo() {
        for (MonsterModel model : MonsterModel.values()) {
            Set<String> names = new HashSet<>();
            model.getAbilities().forEach(ability -> assertTrue(names.add(ability.name()), ability.name()));
        }
    }

    @Test
    void theTwoSangueElementalEntriesAreTellApartByTier() {
        assertEquals(MonsterCategory.PRESA, AlmaElementalAbility.SANGUE_ELEMENTAL.getTier());
        assertEquals(MonsterCategory.DEVIANTE, AlmaElementalAbility.SANGUE_ELEMENTAL_DEVIANTE.getTier());
    }

    @Test
    void aHabilidadeIsFoundByItsWireKey() {
        assertEquals(Optional.of(CireneiaAbility.CELERIDADE), MonsterModel.ABENCOADO_DE_CIRENEIA.findAbility("CELERIDADE"));
        assertEquals(Optional.of(CireneiaAbility.CELERIDADE), MonsterModel.anyAbility("CELERIDADE"));
        assertEquals(Optional.empty(), MonsterModel.ALMA_ELEMENTAL.findAbility("CELERIDADE"));
    }

    /** A status is only honest if it explains itself: PARTIAL and TABLE_ONLY name what's missing, APPLIED names nothing. */
    @Test
    void everyStatusExplainsWhatIsNotApplied() {
        for (MonsterModel model : MonsterModel.values()) {
            for (MonstrousAbility ability : model.getAbilities()) {
                ImplementationStatus status = ability.getImplementationStatus();
                String note = ability.getUnappliedNote();
                if (status == ImplementationStatus.APPLIED) {
                    assertEquals(null, note, ability.name());
                } else {
                    assertTrue(note != null && !note.isBlank(), ability.name() + " is " + status + " with no note");
                }
            }
        }
    }

    @Test
    void noModeloIsLeftTableOnly() {
        for (MonsterModel model : MonsterModel.values()) {
            long applied = model.getAbilities().stream()
                    .filter(ability -> ability.getImplementationStatus() != ImplementationStatus.TABLE_ONLY)
                    .count();
            assertTrue(applied >= 6, model.name() + " applies only " + applied + " Habilidades");
        }
        assertEquals(ImplementationStatus.TABLE_ONLY, CireneiaAbility.PREFERIDO_DE_CIRENEIA.getImplementationStatus());
    }
}
