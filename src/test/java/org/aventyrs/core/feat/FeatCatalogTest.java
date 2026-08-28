package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.conhecimentos.Conhecimentos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatCatalogTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character.CharacterBuilder character() {
        return CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>());
    }

    private static Character withConhecimentos(final int graduation) {
        return character()
                .skill(SkillType.CONHECIMENTOS, CharacterSkill.builder()
                        .skill(new Conhecimentos())
                        .graduation(SkillGraduation.builder().graduationValue(graduation).build())
                        .build())
                .build();
    }

    // ---------- discovery ----------

    @Test
    void theCatalogHoldsEveryConstantOfEveryAuthoredTree() {
        List<Feat> expected = new ArrayList<>();
        expected.addAll(Arrays.asList(ArtesMarciaisFeat.values()));
        expected.addAll(Arrays.asList(MetamagicoFeat.values()));

        assertTrue(FeatCatalog.all().containsAll(expected));
        assertEquals(expected.size(), FeatCatalog.all().size());
    }

    /**
     * The permits clause is the catalog's source of truth, so it must name every enum that
     * implements Feat. The compiler already refuses an unlisted implementation — this pins the
     * opposite slip: a tree listed in permits but silently emptied, or a new tree whose
     * constants never reach the catalog.
     */
    @Test
    void everyPermittedEnumContributesItsConstants() {
        for (Class<?> permitted : Feat.class.getPermittedSubclasses()) {
            if (permitted.isEnum()) {
                for (Object constant : permitted.getEnumConstants()) {
                    assertTrue(FeatCatalog.all().contains(constant),
                            permitted.getSimpleName() + "." + constant + " missing from the catalog");
                }
            }
        }
    }

    @Test
    void abstractFeatIsPermittedSoConsumersCanExtendItButContributesNothing() {
        assertTrue(Arrays.asList(Feat.class.getPermittedSubclasses()).contains(AbstractFeat.class));
        assertFalse(AbstractFeat.class.isEnum());
        assertTrue(FeatCatalog.all().stream().noneMatch(feat -> feat instanceof AbstractFeat));
    }

    @Test
    void aHomebrewFeatIsAValidFeatButNotPartOfTheCatalog() {
        Feat homebrew = AbstractFeat.builder()
                .featCategory(FeatCategory.METAMAGICO)
                .description("A homebrew Talento.")
                .featRequirements(FeatRequirements.builder().build())
                .build();

        assertTrue(homebrew.isEligible(character().build()));
        assertFalse(FeatCatalog.all().contains(homebrew));
    }

    @Test
    void theCatalogIsImmutable() {
        assertThrows(UnsupportedOperationException.class,
                () -> FeatCatalog.all().add(ArtesMarciaisFeat.ARTISTA_MARCIAL));
    }

    // ---------- by category ----------

    @Test
    void inReturnsOnlyThatTreesTalentos() {
        assertEquals(List.of(MetamagicoFeat.values()), FeatCatalog.in(FeatCategory.METAMAGICO));
        assertEquals(List.of(ArtesMarciaisFeat.values()), FeatCatalog.in(FeatCategory.ARTE_MARCIAL));
    }

    @Test
    void aCategoryWithNoEnumAuthoredYetIsEmptyRatherThanAbsent() {
        assertTrue(FeatCatalog.in(FeatCategory.ASSASSINO).isEmpty());
    }

    @Test
    void everyCatalogedTalentoReportsTheCategoryItIsIndexedUnder() {
        for (FeatCategory category : FeatCategory.values()) {
            for (Feat feat : FeatCatalog.in(category)) {
                assertEquals(category, feat.getFeatCategory());
            }
        }
    }

    // ---------- availableFor ----------

    @Test
    void aBlankCharacterQualifiesForNothingWithPrerequisites() {
        assertFalse(FeatCatalog.availableFor(character().build()).contains(MetamagicoFeat.ARCANISTA));
    }

    @Test
    void trainingInConhecimentosUnlocksTheEntryLevelTalentos() {
        List<Feat> available = FeatCatalog.availableFor(withConhecimentos(1));

        assertTrue(available.contains(MetamagicoFeat.ARCANISTA));
        assertTrue(available.contains(MetamagicoFeat.MENTE_EXPANDIDA));
    }

    @Test
    void aTalentoDeeperInTheChainStaysLockedUntilItsPredecessorIsHeld() {
        Character character = withConhecimentos(9);

        assertFalse(FeatCatalog.availableFor(character).contains(MetamagicoFeat.ARCANISTA_EXPERIENTE));

        character.grantFeat(MetamagicoFeat.ARCANISTA);

        assertTrue(FeatCatalog.availableFor(character).contains(MetamagicoFeat.ARCANISTA_EXPERIENTE));
    }

    @Test
    void anAlreadyHeldTalentoDropsOutOfTheAvailableList() {
        Character character = withConhecimentos(1);
        assertTrue(FeatCatalog.availableFor(character).contains(MetamagicoFeat.ARCANISTA));

        character.grantFeat(MetamagicoFeat.ARCANISTA);

        assertFalse(FeatCatalog.availableFor(character).contains(MetamagicoFeat.ARCANISTA));
    }

    @Test
    void theWholeChainOpensOneRungAtATime() {
        Character character = withConhecimentos(9);
        List<MetamagicoFeat> ladder = List.of(MetamagicoFeat.ARCANISTA,
                MetamagicoFeat.ARCANISTA_EXPERIENTE, MetamagicoFeat.MESTRE_ARCANISTA,
                MetamagicoFeat.DESAFIADOR_DA_REALIDADE);

        for (MetamagicoFeat rung : ladder) {
            assertTrue(FeatCatalog.availableFor(character).contains(rung), rung.name() + " should be open");
            for (MetamagicoFeat later : ladder.subList(ladder.indexOf(rung) + 1, ladder.size())) {
                assertFalse(FeatCatalog.availableFor(character).contains(later), later.name() + " should be locked");
            }
            character.grantFeat(rung);
        }
    }
}
