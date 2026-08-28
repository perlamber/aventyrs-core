package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.FeatService;
import org.aventyrs.core.character.services.FeatServiceImpl;
import org.aventyrs.core.race.Gigantes;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What {@code Gigantes}' reduced Talento cost actually does to a character who acquires one —
 * Sobrevivência Talentos cost 2 instead of {@link Race#BASE_NEW_FEAT_COST} 3.
 *
 * <p>{@code GigantesTest} already pins the bare {@code getNewFeatCost} lookup. This class covers
 * the three things that lookup cannot: that every <em>authored</em> Talento is charged the right
 * number (driven off {@link FeatCatalog}, so a Talento added later is covered with no edit
 * here), that the discount reaches the XP wallet through {@link FeatService#grantFeat}, and that
 * it doesn't leak into any other category or any other Race.
 *
 * <p><b>No Sobrevivência Talento is authored yet</b> — {@code FeatCategory#SOBREVIVENCIA} has no
 * enum in this catalog. The catalog-driven tests below therefore pass vacuously over that
 * category today and start covering it the moment one lands; the end-to-end spend is proven with
 * a homebrew {@link AbstractFeat} until then, and should be pointed at the real constant when
 * there is one.
 */
class GigantesFeatCostTest {

    /** The Gigantes discount: Sobrevivência Talentos cost this, not {@link Race#BASE_NEW_FEAT_COST}. */
    private static final int SOBREVIVENCIA_COST = 2;

    private final FeatService featService = new FeatServiceImpl();
    private final Race gigantes = new Gigantes();

    /**
     * A stand-in for the Sobrevivência Talento this tree doesn't author yet — no Pré-requisito,
     * so it isolates the cost question from any eligibility one. Replace with the real constant
     * once a {@code SobrevivenciaFeat} enum exists.
     */
    private static final Feat HOMEBREW_SOBREVIVENCIA_FEAT = AbstractFeat.builder()
            .featCategory(FeatCategory.SOBREVIVENCIA)
            .description("Test-only stand-in for an authored Talento de Sobrevivência.")
            .featRequirements(FeatRequirements.builder().build())
            .build();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character.CharacterBuilder character(final Race race) {
        return CharacterFixture.blank(CharacterFixture.BLANK).race(race).feats(new ArrayList<>());
    }

    /** Meets ARTISTA_MARCIAL's Pré-requisito, so the only variable left in a spend test is the Race. */
    private static Character martialArtistOf(final Race race) {
        return character(race)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(2).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, CharacterSkill.builder()
                        .skill(new AtaqueCorpoACorpo())
                        .graduation(SkillGraduation.builder().graduationValue(2).build())
                        .build())
                .build();
    }

    private static CharacterSheet sheetWith(final Character character, final int experience) {
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(BigDecimal.valueOf(experience));
        return sheet;
    }

    // ---------- every authored Talento, charged the right number ----------

    /** Vacuous while the category has no enum; covers every constant automatically once it does. */
    @Test
    void everyAuthoredSobrevivenciaTalentoCostsTheReducedPrice() {
        for (Feat feat : FeatCatalog.in(FeatCategory.SOBREVIVENCIA)) {
            assertEquals(SOBREVIVENCIA_COST, gigantes.getNewFeatCost(feat.getFeatCategory()),
                    feat.toString());
        }
    }

    @Test
    void everyOtherAuthoredTalentoStillCostsTheBasePrice() {
        for (Feat feat : FeatCatalog.all()) {
            if (feat.getFeatCategory() != FeatCategory.SOBREVIVENCIA) {
                assertEquals(Race.BASE_NEW_FEAT_COST, gigantes.getNewFeatCost(feat.getFeatCategory()),
                        feat.toString());
            }
        }
    }

    /** The discount is the Race's alone — every authored Talento costs a Human the base price. */
    @Test
    void aHumanPaysTheBasePriceForEveryAuthoredTalento() {
        Race human = new Human();
        for (Feat feat : FeatCatalog.all()) {
            assertEquals(Race.BASE_NEW_FEAT_COST, human.getNewFeatCost(feat.getFeatCategory()),
                    feat.toString());
        }
        assertEquals(Race.BASE_NEW_FEAT_COST, human.getNewFeatCost(FeatCategory.SOBREVIVENCIA));
    }

    // ---------- the discount reaching the wallet ----------

    @Test
    void aGigantesSpendsOnlyTheReducedCostAcquiringASobrevivenciaTalento() throws IllegalOperationException {
        Character character = character(gigantes).build();
        CharacterSheet sheet = sheetWith(character, 10);

        featService.grantFeat(character, sheet, HOMEBREW_SOBREVIVENCIA_FEAT);

        assertEquals(BigDecimal.valueOf(10 - SOBREVIVENCIA_COST), sheet.getUnUsedExperience());
    }

    /** Same Talento, same wallet, only the Race differs — the gap is exactly the discount. */
    @Test
    void theSameTalentoCostsAHumanOneMoreExperience() throws IllegalOperationException {
        Character giant = character(gigantes).build();
        Character human = character(new Human()).build();
        CharacterSheet giantSheet = sheetWith(giant, 10);
        CharacterSheet humanSheet = sheetWith(human, 10);

        featService.grantFeat(giant, giantSheet, HOMEBREW_SOBREVIVENCIA_FEAT);
        featService.grantFeat(human, humanSheet, HOMEBREW_SOBREVIVENCIA_FEAT);

        assertEquals(BigDecimal.valueOf(Race.BASE_NEW_FEAT_COST - SOBREVIVENCIA_COST),
                giantSheet.getUnUsedExperience().subtract(humanSheet.getUnUsedExperience()));
    }

    @Test
    void theDiscountDoesNotLeakIntoAnotherCategorysTalento() throws IllegalOperationException {
        Character giant = martialArtistOf(gigantes);
        Character human = martialArtistOf(new Human());
        CharacterSheet giantSheet = sheetWith(giant, 10);
        CharacterSheet humanSheet = sheetWith(human, 10);

        featService.grantFeat(giant, giantSheet, ArtesMarciaisFeat.ARTISTA_MARCIAL);
        featService.grantFeat(human, humanSheet, ArtesMarciaisFeat.ARTISTA_MARCIAL);

        assertEquals(BigDecimal.valueOf(10 - Race.BASE_NEW_FEAT_COST), giantSheet.getUnUsedExperience());
        assertEquals(giantSheet.getUnUsedExperience(), humanSheet.getUnUsedExperience());
    }

    // ---------- what the discount is for ----------

    /**
     * Two XP is exactly a Gigantes' price for a Talento de Sobrevivência and one short of
     * everyone else's — the sharpest statement of what the override buys. Asserted end to end
     * through {@link FeatService#grantFeat}, since {@code getAffordableFeats} only ever lists the
     * authored catalog and this category has no constants in it yet.
     */
    @Test
    void twoExperienceBuysASobrevivenciaTalentoForAGigantesAndNotForAHuman() throws IllegalOperationException {
        Character giant = character(gigantes).build();
        Character human = character(new Human()).build();
        CharacterSheet giantSheet = sheetWith(giant, SOBREVIVENCIA_COST);
        CharacterSheet humanSheet = sheetWith(human, SOBREVIVENCIA_COST);

        featService.grantFeat(giant, giantSheet, HOMEBREW_SOBREVIVENCIA_FEAT);

        assertEquals(BigDecimal.ZERO, giantSheet.getUnUsedExperience());
        assertThrows(IllegalOperationException.class,
                () -> featService.grantFeat(human, humanSheet, HOMEBREW_SOBREVIVENCIA_FEAT));
        assertEquals(BigDecimal.valueOf(SOBREVIVENCIA_COST), humanSheet.getUnUsedExperience());
    }

    @Test
    void oneExperienceIsStillShortEvenForAGigantes() {
        Character giant = character(gigantes).build();
        CharacterSheet sheet = sheetWith(giant, SOBREVIVENCIA_COST - 1);

        assertThrows(IllegalOperationException.class,
                () -> featService.grantFeat(giant, sheet, HOMEBREW_SOBREVIVENCIA_FEAT));
        assertEquals(BigDecimal.valueOf(SOBREVIVENCIA_COST - 1), sheet.getUnUsedExperience());
    }

    /**
     * The catalog-listing path agrees with the spend path: nothing authored is affordable to
     * either Race at 2 XP today (every authored Talento sits in a full-price category), and the
     * moment a Sobrevivência one lands the Gigantes half of this starts carrying weight.
     */
    @Test
    void theAffordableListingAgreesWithTheDiscount() {
        Character giant = character(gigantes).build();
        Character human = character(new Human()).build();

        assertTrue(featService.getAffordableFeats(giant, sheetWith(giant, SOBREVIVENCIA_COST))
                .containsAll(FeatCatalog.in(FeatCategory.SOBREVIVENCIA)));
        assertTrue(featService.getAffordableFeats(human, sheetWith(human, SOBREVIVENCIA_COST)).isEmpty(),
                "no Talento costs a Human as little as " + SOBREVIVENCIA_COST);
    }
}
