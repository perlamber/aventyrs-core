package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.ArtesMarciaisFeat;
import org.aventyrs.core.feat.AbstractFeat;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.MetamagicoFeat;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.feat.FeatRequirements;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatServiceImplTest {

    private final FeatService featService = new FeatServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSkill trainedAtaqueCorpoACorpo(final int graduationValue) {
        return CharacterSkill.builder()
                .skill(new AtaqueCorpoACorpo())
                .graduation(SkillGraduation.builder().graduationValue(graduationValue).build())
                .build();
    }

    /** Satisfies ARTISTA_MARCIAL's own "Força 2 e 2 Graduações em Ataque Corpo-a-Corpo". */
    private Character characterMeetingArtistaMarcialRequirements() {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(2).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, trainedAtaqueCorpoACorpo(2))
                .feats(new ArrayList<>())
                .build();
    }

    private CharacterSheet sheetWithExperience(final Character character, final BigDecimal experience) {
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(experience);
        return sheet;
    }

    @Test
    void grantFeatRejectsWhenAttributeRequirementIsntMetYet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, trainedAtaqueCorpoACorpo(2))
                .feats(new ArrayList<>())
                .build();
        CharacterSheet sheet = sheetWithExperience(character, BigDecimal.TEN);

        assertThrows(IllegalOperationException.class,
                () -> featService.grantFeat(character, sheet, ArtesMarciaisFeat.ARTISTA_MARCIAL));
    }

    @Test
    void grantFeatRejectsWhenSkillGraduationRequirementIsntMetYet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(2).build())
                        .build())
                .feats(new ArrayList<>())
                .build();
        CharacterSheet sheet = sheetWithExperience(character, BigDecimal.TEN);

        assertThrows(IllegalOperationException.class,
                () -> featService.grantFeat(character, sheet, ArtesMarciaisFeat.ARTISTA_MARCIAL));
    }

    @Test
    void grantFeatRejectsWhenNotEnoughExperience() {
        Character character = characterMeetingArtistaMarcialRequirements();
        CharacterSheet sheet = sheetWithExperience(character, BigDecimal.ONE);

        assertThrows(IllegalOperationException.class,
                () -> featService.grantFeat(character, sheet, ArtesMarciaisFeat.ARTISTA_MARCIAL));
        assertEquals(BigDecimal.ONE, sheet.getUnUsedExperience());
    }

    @Test
    void grantFeatGrantsTheFeatAndSpendsItsRaceCostOnceRequirementsAreMet() throws IllegalOperationException {
        Character character = characterMeetingArtistaMarcialRequirements();
        CharacterSheet sheet = sheetWithExperience(character, BigDecimal.TEN);

        featService.grantFeat(character, sheet, ArtesMarciaisFeat.ARTISTA_MARCIAL);

        assertEquals(List.of(ArtesMarciaisFeat.ARTISTA_MARCIAL), character.getFeats());
        assertEquals(BigDecimal.valueOf(7), sheet.getUnUsedExperience());
    }

    @Test
    void grantFeatRejectsWhenItsOwnRequiredFeatIsntHeldYet() {
        Feat prerequisite = ArtesMarciaisFeat.ARTISTA_MARCIAL;
        Feat gatedFeat = AbstractFeat.builder()
                .featCategory(FeatCategory.ARTE_MARCIAL)
                .description("Test-only Feat requiring ARTISTA_MARCIAL.")
                .featRequirements(FeatRequirements.builder().requiredFeat(prerequisite).build())
                .build();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>()).build();
        CharacterSheet sheet = sheetWithExperience(character, BigDecimal.TEN);

        assertThrows(IllegalOperationException.class, () -> featService.grantFeat(character, sheet, gatedFeat));

        character.grantFeat(prerequisite);
        assertTrue(gatedFeat.isEligible(character));
    }

    // ---------- listing what a character may take ----------

    @Test
    void getAvailableFeatsListsOnlyTalentosWhosePrerequisitesAreMet() {
        Character character = characterMeetingArtistaMarcialRequirements();

        List<Feat> available = featService.getAvailableFeats(character);

        assertTrue(available.contains(ArtesMarciaisFeat.ARTISTA_MARCIAL));
        assertFalse(available.contains(MetamagicoFeat.ARCANISTA), "no Conhecimentos training");
    }

    @Test
    void getAvailableFeatsExcludesTalentosAlreadyHeld() {
        Character character = characterMeetingArtistaMarcialRequirements();
        character.grantFeat(ArtesMarciaisFeat.ARTISTA_MARCIAL);

        assertFalse(featService.getAvailableFeats(character).contains(ArtesMarciaisFeat.ARTISTA_MARCIAL));
    }

    @Test
    void everyAvailableFeatIsActuallyGrantable() {
        Character character = characterMeetingArtistaMarcialRequirements();
        CharacterSheet sheet = sheetWithExperience(character, BigDecimal.valueOf(100));

        for (Feat feat : List.copyOf(featService.getAvailableFeats(character))) {
            featService.grantFeat(character, sheet, feat);
        }

        assertTrue(character.getFeats().contains(ArtesMarciaisFeat.ARTISTA_MARCIAL));
    }

    @Test
    void getAffordableFeatsDropsWhatTheWalletCannotPayFor() {
        Character character = characterMeetingArtistaMarcialRequirements();
        CharacterSheet broke = sheetWithExperience(character, BigDecimal.ZERO);
        CharacterSheet funded = sheetWithExperience(character, BigDecimal.TEN);

        assertTrue(featService.getAvailableFeats(character).contains(ArtesMarciaisFeat.ARTISTA_MARCIAL));
        assertFalse(featService.getAffordableFeats(character, broke).contains(ArtesMarciaisFeat.ARTISTA_MARCIAL));
        assertTrue(featService.getAffordableFeats(character, funded).contains(ArtesMarciaisFeat.ARTISTA_MARCIAL));
    }

    @Test
    void exactlyEnoughExperienceIsAffordable() {
        Character character = characterMeetingArtistaMarcialRequirements();
        int cost = character.getRace().getNewFeatCost(FeatCategory.ARTE_MARCIAL);
        CharacterSheet exact = sheetWithExperience(character, BigDecimal.valueOf(cost));

        assertTrue(featService.getAffordableFeats(character, exact).contains(ArtesMarciaisFeat.ARTISTA_MARCIAL));
    }

    @Test
    void affordableIsAlwaysASubsetOfAvailable() {
        Character character = characterMeetingArtistaMarcialRequirements();
        CharacterSheet sheet = sheetWithExperience(character, BigDecimal.valueOf(100));

        assertTrue(featService.getAvailableFeats(character)
                .containsAll(featService.getAffordableFeats(character, sheet)));
    }
}
