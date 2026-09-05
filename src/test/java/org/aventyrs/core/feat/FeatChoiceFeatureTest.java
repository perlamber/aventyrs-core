package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.FeatService;
import org.aventyrs.core.character.services.FeatServiceImpl;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The catalog-identity plumbing that lets a choice-carrying {@link AbstractFeat} (here {@link
 * FocoEmPericiaFeat}) stand in for its enum constant everywhere {@code Feat} identity is
 * compared — {@link Feat#catalogEntry()} feeding {@link Feat#isEligible} and {@code
 * FeatCatalog#availableFor}.
 */
class FeatChoiceFeatureTest {

    private final FeatService featService = new FeatServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character character() {
        return CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>()).build();
    }

    private CharacterSheet fundedSheet(final Character character) {
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(BigDecimal.valueOf(100));
        return sheet;
    }

    @Test
    void holdingTheChoiceInstanceRemovesTheBaseTalentoFromTheAvailableList() throws IllegalOperationException {
        Character character = character();
        CharacterSheet sheet = fundedSheet(character);
        assertTrue(featService.getAvailableFeats(character).contains(PeritoFeat.FOCO_EM_PERICIA));

        featService.grantFeat(character, sheet, FocoEmPericiaFeat.of(SkillType.ATLETISMO));

        assertFalse(featService.getAvailableFeats(character).contains(PeritoFeat.FOCO_EM_PERICIA));
    }

    @Test
    void aDependentTalentoBecomesEligibleOnceTheChoiceInstanceIsHeld() throws IllegalOperationException {
        Character character = character();
        CharacterSheet sheet = fundedSheet(character);
        assertFalse(featService.getAvailableFeats(character).contains(PeritoFeat.DISCRETO),
                "DISCRETO requires FOCO_EM_PERICIA");

        featService.grantFeat(character, sheet, FocoEmPericiaFeat.of(SkillType.ATLETISMO));

        assertTrue(featService.getAvailableFeats(character).contains(PeritoFeat.DISCRETO));
        assertTrue(PeritoFeat.DISCRETO.isEligible(character));
    }
}
