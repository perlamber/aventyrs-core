package org.aventyrs.core.character.services;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.util.DiceRoller;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Dano de Queda — acoes-de-combate.txt, including both of its worked examples. */
class FallServiceTest {

    private final FallService service = new FallServiceImpl();
    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    private static CharacterSheet sheet(final SizeCategory size) {
        return CharacterSheet.of(Character.builder()
                .player(new Player())
                .name("Faller")
                .race(new Human())
                .sizeCategory(size)
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .egos(CharacterEgos.builder().build())
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(4).build())
                        .build())
                .build(), new Player());
    }

    @Test
    void resistanceIsTwiceTheCategoryAtLeastTwo() {
        assertEquals(4, service.getFallResistance(sheet(SizeCategory.PLUS_TWO)));
        assertEquals(2, service.getFallResistance(sheet(SizeCategory.ZERO)));
        assertEquals(2, service.getFallResistance(sheet(SizeCategory.MINUS_ONE)));
    }

    /** "Um Gigante … sofre danos quando a queda for igual ou superior à 6UD … então sofre 1 de Dano Primordial". */
    @Test
    void theGiantExample() {
        assertEquals(0, service.preview(sheet(SizeCategory.PLUS_TWO), 5).damage());
        assertEquals(1, service.preview(sheet(SizeCategory.PLUS_TWO), 6).damage());
    }

    /** "um Goblin … e um Elfo … sofrem danos quando a queda for igual ou superior à 4UD … 1 ponto". */
    @Test
    void theGoblinAndElfExample() {
        for (SizeCategory size : new SizeCategory[]{SizeCategory.MINUS_ONE, SizeCategory.ZERO}) {
            assertEquals(0, service.preview(sheet(size), 3).damage());
            assertEquals(1, service.preview(sheet(size), 4).damage());
        }
    }

    @Test
    void aFallDealsUnreducedDamageAndLeavesTheFallerCaido() {
        CharacterSheet elf = sheet(SizeCategory.ZERO);

        FallService.FallResult result = service.fall(elf, 8, null, null);

        assertEquals(3, result.damage());
        assertEquals(3, elf.getDamageTaken());
        assertTrue(elf.hasCondition(ConditionType.CAIDO, null));
        assertFalse(elf.hasCondition(ConditionType.FERIDAS_DOLOROSAS, null));
    }

    @Test
    void aShortFallStillKnocksDown() {
        CharacterSheet elf = sheet(SizeCategory.ZERO);

        assertEquals(0, service.fall(elf, 1, null, null).damage());
        assertEquals(0, elf.getDamageTaken());
        assertTrue(elf.hasCondition(ConditionType.CAIDO, null));
    }

    /** "Para cada 10UD excedente soma-se 1d6 aos danos e aplica Feridas Dolorosas". */
    @Test
    void everyTenExcessUdAddsADieAndFeridasDolorosas() {
        CharacterSheet elf = sheet(SizeCategory.ZERO);

        FallService.FallResult result = service.fall(elf, 2 + 20, DiceRoller.fixed(3, 5), null);

        assertEquals(2, result.extraDice());
        assertEquals(10 + 3 + 5, result.damage());
        assertTrue(elf.hasCondition(ConditionType.FERIDAS_DOLOROSAS, null));
    }

    @Test
    void extraDiceNeedARoller() {
        assertThrows(IllegalArgumentException.class, () -> service.fall(sheet(SizeCategory.ZERO), 12, null, null));
    }

    /** "quedas de alturas excedentes superiores a 40UD indicam morte instantânea". */
    @Test
    void anExcessBeyondFortyKills() {
        CharacterSheet elf = sheet(SizeCategory.ZERO);
        assertFalse(service.preview(elf, 2 + 40).lethal());

        assertTrue(service.fall(elf, 2 + 41, null, null).lethal());
        assertEquals(CharacterStatus.DEAD, hitPointsService.getStatus(elf));
    }
}
