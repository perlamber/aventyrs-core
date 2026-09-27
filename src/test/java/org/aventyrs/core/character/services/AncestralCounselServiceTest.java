package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.OrquicoFeat;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.Orc;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.TerrainType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.attention.AttentionInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.aventyrs.core.util.TranslatableMessages.ANCESTRAL_COUNSEL_IN_COMBAT;
import static org.aventyrs.core.util.TranslatableMessages.ANCESTRAL_COUNSEL_NOT_BANKED;
import static org.aventyrs.core.util.TranslatableMessages.ANCESTRAL_COUNSEL_NOT_HELD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Phase I: the Orc's Agnação Ancestral and Agnação Ancestral Superior. */
class AncestralCounselServiceTest {

    private final AncestralCounselService service = new AncestralCounselServiceImpl();
    private final AttentionInteraction attention = new AttentionInteraction();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet sheetOf(final Race race, final Feat... feats) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .race(race)
                .attributes(CharacterAttributes.builder()
                        .instinct(AttributeValue.builder().domain(AttributeDomain.INSTINCT).base(3).build())
                        .focus(AttributeValue.builder().domain(AttributeDomain.FOCUS).base(5).build())
                        .build())
                .feats(new ArrayList<>(List.of(feats)))
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private static SkillRoll roll() {
        return new SkillRoll(List.of(3, 3, 3));
    }

    @Test
    void theRitualSpendsThreePmAndBanksOneCounsel() {
        CharacterSheet orc = sheetOf(new Orc());
        int spentBefore = orc.getManaSpent();

        AncestralCounselService.AncestralCounsel counsel = service.perform(orc, null);

        assertEquals(spentBefore + AncestralCounselService.MAGIC_POINT_COST, orc.getManaSpent());
        assertEquals(1, orc.getCharges(AncestralCounselService.COUNSEL));
        assertFalse(counsel.pawnSubordinateGranted());
        assertTrue(service.perform(sheetOf(new Orc(), OrquicoFeat.AGNACAO_ANCESTRAL_SUPERIOR), null)
                .pawnSubordinateGranted());
    }

    @Test
    void onlyAnOrcOutsideCombatMayPerformIt() {
        IllegalOperationException notOrc = assertThrows(IllegalOperationException.class,
                () -> service.perform(sheetOf(new Human()), null));
        assertEquals(ANCESTRAL_COUNSEL_NOT_HELD, notOrc.getMessage());

        SceneContext combat = new SceneContext(List.of(), List.of(), Map.of(), TerrainType.values()[0],
                true, 1, false, null);
        IllegalOperationException inCombat = assertThrows(IllegalOperationException.class,
                () -> service.perform(sheetOf(new Orc()), combat));
        assertEquals(ANCESTRAL_COUNSEL_IN_COMBAT, inCombat.getMessage());
    }

    @Test
    void aCounselledRollIsTrainedExpertAndOneNivelEasier() {
        CharacterSheet orc = sheetOf(new Orc());
        InteractionResult plain = attention.applyTo(orc, null, roll());
        service.perform(orc, null);

        InteractionResult counselled = attention.applyTo(orc, null, roll().counselled());

        assertEquals(plain.getDifficultyReduction() + 1, counselled.getDifficultyReduction());
        assertEquals(plain.getSkillRollBonus() + 2, counselled.getSkillRollBonus());

        assertTrue(service.spend(orc));
        assertFalse(service.spend(orc));
    }

    @Test
    void aCounselledRollNeedsABankedCounsel() {
        CharacterSheet orc = sheetOf(new Orc());

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> attention.applyTo(orc, null, roll().counselled()));
        assertEquals(ANCESTRAL_COUNSEL_NOT_BANKED, refused.getMessage());
    }
}
