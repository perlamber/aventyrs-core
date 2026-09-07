package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.AnaoFeat;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.TargetScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatStartBlessingServiceImplTest {

    private final CombatStartBlessingService service = new CombatStartBlessingServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet sheet(final AnaoFeat... feats) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>()).build();
        for (AnaoFeat feat : feats) {
            character.grantFeat(feat);
        }
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void aCombatantWithNoCombatStartTalentoGetsNothing() {
        assertTrue(service.applyCombatStartBlessings(sheet()).isEmpty());
    }

    @Test
    void vigorDoInvernoGrantsRdAndCriticalResistanceForHalfTheLifeMultiplier() {
        CharacterSheet combatant = sheet(AnaoFeat.VIGOR_DO_INVERNO);
        // Base Multiplicador de PV 4, +1 from the Talento itself → 5, halved (floored) → 2.
        int expectedRounds = 2;

        List<Blessing> granted = service.applyCombatStartBlessings(combatant);

        assertEquals(2, granted.size());
        assertTrue(granted.stream().allMatch(blessing -> blessing.getRounds() == expectedRounds));
        assertTrue(granted.stream().allMatch(blessing -> blessing.getScope() == TargetScope.SELF));
        assertTrue(granted.stream().allMatch(blessing -> blessing.getSource().equals(AnaoFeat.VIGOR_DO_INVERNO.name())));

        Blessing rd = granted.stream()
                .filter(blessing -> blessing.getModifierType() == ModifierType.DAMAGE_REDUCTION)
                .findFirst().orElseThrow();
        Blessing rc = granted.stream()
                .filter(blessing -> blessing.getModifierType() == ModifierType.CRITICAL_RESISTANCE)
                .findFirst().orElseThrow();

        assertEquals(DamageService.DEFAULT_DAMAGE_REDUCTION, rd.getValue());
        assertEquals(2, rc.getValue());
        // Applied to the combatant itself, as TemporaryBonuses.
        assertEquals(DamageService.DEFAULT_DAMAGE_REDUCTION,
                combatant.getTemporaryBonus(ModifierType.DAMAGE_REDUCTION));
        assertEquals(2, combatant.getTemporaryBonus(ModifierType.CRITICAL_RESISTANCE));
    }
}
