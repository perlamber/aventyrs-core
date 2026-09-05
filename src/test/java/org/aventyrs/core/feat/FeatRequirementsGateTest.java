package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.race.Anao;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.AventyrTitleSpecialization;
import org.aventyrs.core.title.TitleArchetype;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The four prerequisite clauses added for the Talento catalog — the Aventyr-tier title count,
 * its optional {@link TitleArchetype} narrowing, the Race restriction, and the
 * count-of-category clause.
 *
 * <p>Exercised through {@link AbstractFeat} rather than a catalog constant on purpose: these
 * pin {@link Feat#isEligible}'s own gate arithmetic, which is shared by every Talento, the same
 * layer {@code MetamagicoFeatTest} occupies for formulas. What a specific Talento *does* once
 * acquired is tested against its consuming service instead — see the {@code testing-a-feat}
 * skill and {@link MetamagicoFeatIntegrationTest}.
 */
class FeatRequirementsGateTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /** A Título Aventyr of an archetype other than Santo's, so the archetype filter has two sides. */
    private record BrutoTitle() implements AventyrTitle {
        @Override public String getName() { return "Bruto de Teste"; }
        @Override public TitleArchetype getArchetype() { return TitleArchetype.BRUTO; }
        @Override public String getBaseEffectDescription() { return ""; }
        @Override public List<AventyrTitleSpecialization> getSpecializations() { return List.of(); }
        @Override public List<AventyrTitleAbility> getAbilities() { return List.of(); }
        @Override public void grantAbility(final AventyrTitleAbility ability) { }
    }

    private static Character.CharacterBuilder character() {
        return CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>());
    }

    private static Feat featRequiring(final FeatRequirements requirements) {
        return new AbstractFeat(FeatCategory.DESTINO, "test", requirements);
    }

    private static Feat ofCategory(final FeatCategory category) {
        return new AbstractFeat(category, "test", FeatRequirements.builder().build());
    }

    // ---- requiredAwakenedTitles -------------------------------------------------------------

    @Test
    void aTalentoNamingNoAventyrTierIsEligibleWithNoTitulosHeld() {
        assertTrue(featRequiring(FeatRequirements.builder().build()).isEligible(character().build()));
    }

    @Test
    void oneAwakenedTituloIsNotEnoughForATalentoDemandingTwo() {
        Character character = character().build();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);

        Feat feat = featRequiring(FeatRequirements.builder().requiredAwakenedTitles(2).build());

        assertFalse(feat.isEligible(character));

        character.grantTitle(new BrutoTitle(), TitleSlot.SECONDARY);

        assertTrue(feat.isEligible(character));
    }

    @Test
    void aTituloIsDespertoSimplyByFillingASlotWithNoSpecializationsOrSupremas() {
        Character character = character().build();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);

        assertTrue(featRequiring(FeatRequirements.builder().requiredAwakenedTitles(1).build())
                .isEligible(character));
    }

    // ---- requiredTitleArchetype -------------------------------------------------------------

    @Test
    void anArchetypeGateCountsOnlyTitulosOfThatArchetype() {
        Character character = character().build();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);

        Feat centelhaBruta = featRequiring(FeatRequirements.builder()
                .requiredAwakenedTitles(1)
                .requiredTitleArchetype(TitleArchetype.BRUTO)
                .build());

        assertFalse(centelhaBruta.isEligible(character), "Santo is ABENCOADO, not BRUTO");

        character.grantTitle(new BrutoTitle(), TitleSlot.SECONDARY);

        assertTrue(centelhaBruta.isEligible(character));
    }

    @Test
    void santoSatisfiesAnAbencoadoArchetypeGate() {
        Character character = character().build();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);

        assertTrue(featRequiring(FeatRequirements.builder()
                .requiredAwakenedTitles(1)
                .requiredTitleArchetype(TitleArchetype.ABENCOADO)
                .build()).isEligible(character));
    }

    // ---- requiredRace ------------------------------------------------------------------------

    @Test
    void aRaceGateRefusesAnyOtherRace() {
        Feat anaoOnly = featRequiring(FeatRequirements.builder().requiredRace(Anao.class).build());

        assertTrue(anaoOnly.isEligible(character().race(new Anao()).build()));
        assertFalse(anaoOnly.isEligible(character().race(new Human()).build()));
    }

    // ---- requiredFeatCategory / requiredFeatCategoryCount -------------------------------------

    @Test
    void aCountOfCategoryGateCountsOnlyTalentosOfThatCategory() {
        Character character = character().build();
        character.grantFeat(ofCategory(FeatCategory.DESTINO));
        character.grantFeat(ofCategory(FeatCategory.ARTILHARIA));

        Feat needsTwoDestino = featRequiring(FeatRequirements.builder()
                .requiredFeatCategory(FeatCategory.DESTINO)
                .requiredFeatCategoryCount(2)
                .build());

        assertFalse(needsTwoDestino.isEligible(character), "only one held Talento is DESTINO");

        character.grantFeat(ofCategory(FeatCategory.DESTINO));

        assertTrue(needsTwoDestino.isEligible(character));
    }

    // ---- combination -------------------------------------------------------------------------

    @Test
    void everySetClauseMustHoldAtOnce() {
        Feat feat = featRequiring(FeatRequirements.builder()
                .requiredAwakenedTitles(1)
                .requiredRace(Anao.class)
                .build());

        Character anaoWithoutTitulo = character().race(new Anao()).build();
        assertFalse(feat.isEligible(anaoWithoutTitulo));

        Character humanWithTitulo = character().race(new Human()).build();
        humanWithTitulo.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        assertFalse(feat.isEligible(humanWithTitulo));

        Character anaoWithTitulo = character().race(new Anao()).build();
        anaoWithTitulo.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        assertTrue(feat.isEligible(anaoWithTitulo));
    }
}
