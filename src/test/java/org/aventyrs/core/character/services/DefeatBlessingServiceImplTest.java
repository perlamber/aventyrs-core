package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.AssassinoFeat;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefeatBlessingServiceImplTest {

    private final DefeatBlessingService service = new DefeatBlessingServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet sheet(final AssassinoFeat... feats) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>()).build();
        for (AssassinoFeat feat : feats) {
            character.grantFeat(feat);
        }
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void anAttackerWithNoDefeatTalentoGetsNothing() {
        assertTrue(service.applyDefeatBlessings(sheet(), sheet(), true).isEmpty());
    }

    @Test
    void aGrantedBlessingIsAppliedToTheAttackerNotTheDefeated() {
        CharacterSheet attacker = sheet(AssassinoFeat.SANGUE_QUENTE);
        CharacterSheet defeated = sheet();

        List<Blessing> granted = service.applyDefeatBlessings(attacker, defeated, false);

        assertEquals(1, granted.size());
        assertEquals(1, attacker.getTemporaryBonus(ModifierType.ACTION_POINTS));
        assertEquals(0, defeated.getTemporaryBonus(ModifierType.ACTION_POINTS));
    }
}
