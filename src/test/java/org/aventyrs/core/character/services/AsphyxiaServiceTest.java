package org.aventyrs.core.character.services;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.util.DiceRoller;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Dano de Asfixia — acoes-de-combate.txt, including both of its worked examples. */
class AsphyxiaServiceTest {

    private final AsphyxiaService service = new AsphyxiaServiceImpl();
    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    private static CharacterSheet sheet(final int vigor) {
        return CharacterSheet.of(Character.builder()
                .player(new Player())
                .name("Diver")
                .race(new Human())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .egos(CharacterEgos.builder().build())
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(vigor).build())
                        .build())
                .build(), new Player());
    }

    @Test
    void aBreathingCombatantIsLeftAlone() {
        CharacterSheet dwarf = sheet(4);

        assertSame(AsphyxiaService.AsphyxiaTick.NONE, service.tick(dwarf, null, null));
        assertFalse(service.isBreathless(dwarf));
    }

    /** The Anão, Vigor 4: 1d6 on the 4th Rodada submerged, 2d6 on the 8th, and surfacing ends it. */
    @Test
    void theDwarfExample() {
        CharacterSheet dwarf = sheet(4);
        service.stopBreathing(dwarf);
        DiceRoller dice = DiceRoller.fixed(2, 3, 4);

        for (int round = 1; round <= 3; round++) {
            assertFalse(service.tick(dwarf, dice, null).landed());
        }
        AsphyxiaService.AsphyxiaTick first = service.tick(dwarf, dice, null);
        assertEquals(1, first.instance());
        assertEquals(1, first.dice());
        assertEquals(2, first.damage());
        for (int round = 5; round <= 7; round++) {
            assertFalse(service.tick(dwarf, dice, null).landed());
        }
        AsphyxiaService.AsphyxiaTick second = service.tick(dwarf, dice, null);
        assertEquals(2, second.dice());
        assertEquals(3 + 4, second.damage());
        assertEquals(2 + 3 + 4, dwarf.getDamageTaken());

        service.breathe(dwarf);
        assertFalse(service.isBreathless(dwarf));
        assertSame(AsphyxiaService.AsphyxiaTick.NONE, service.tick(dwarf, dice, null));
    }

    /** The Pequenino, Vigor 2: the first instance on the 2nd Rodada. */
    @Test
    void theHalflingExample() {
        CharacterSheet halfling = sheet(2);
        service.stopBreathing(halfling);

        assertFalse(service.tick(halfling, DiceRoller.fixed(1), null).landed());
        assertEquals(1, service.tick(halfling, DiceRoller.fixed(1), null).instance());
    }

    @Test
    void theDiceCapAtThreeTheFourthAddsFeridasAndTheSixthKills() {
        CharacterSheet diver = sheet(1);
        service.stopBreathing(diver);
        DiceRoller ones = DiceRoller.fixed(1);

        service.tick(diver, ones, null);
        service.tick(diver, ones, null);
        assertEquals(3, service.tick(diver, ones, null).dice());
        AsphyxiaService.AsphyxiaTick fourth = service.tick(diver, ones, null);
        assertEquals(3, fourth.dice());
        assertTrue(fourth.painfulWounds());
        assertTrue(diver.hasCondition(ConditionType.FERIDAS_DOLOROSAS, null));
        assertFalse(service.tick(diver, ones, null).lethal());

        assertTrue(service.tick(diver, null, null).lethal());
        assertEquals(CharacterStatus.DEAD, hitPointsService.getStatus(diver));
    }

    @Test
    void stoppingAgainKeepsTheCount() {
        CharacterSheet diver = sheet(4);
        service.stopBreathing(diver);
        service.tick(diver, null, null);

        service.stopBreathing(diver);

        assertEquals(1, diver.getAsphyxia().orElseThrow().breathlessRounds());
    }
}
