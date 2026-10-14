package org.aventyrs.core.character.services;

import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Bleeding;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.ManaDrain;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.Poisoning;
import org.aventyrs.core.sheet.TemporaryBonus;
import org.aventyrs.core.sheet.TemporaryEffect;
import org.aventyrs.core.sheet.TimedElementalResistance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RD/RM/RE/RA's Duração clause — "Reduz a Duração de Efeitos … contínuos em -1 Unidade" (core 0.1.6.15). */
class ResistanceDurationTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet sheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).id(UUID.randomUUID()).build(),
                new Player());
    }

    private static void grant(final CharacterSheet sheet, final ModifierType type) {
        sheet.applyEffect(TemporaryBonus.openEnded(type, 2, type.name()));
    }

    private static <T extends TemporaryEffect> Optional<T> held(final CharacterSheet sheet, final Class<T> kind) {
        return sheet.getRunningEffects().stream().filter(kind::isInstance).map(kind::cast).findFirst();
    }

    @Test
    void withoutResistanceNothingIsShortened() {
        CharacterSheet sheet = sheet();
        sheet.applyEffect(new Bleeding(2, Optional.of(3)));

        assertEquals(3, held(sheet, Bleeding.class).orElseThrow().getRemainingRounds());
    }

    @Test
    void rdShortensAPhysicalDamagingEffectByOne() {
        CharacterSheet sheet = sheet();
        grant(sheet, ModifierType.DAMAGE_REDUCTION);
        grant(sheet, ModifierType.DAMAGE_REDUCTION);

        sheet.applyEffect(new Bleeding(2, Optional.of(3)));

        assertEquals(2, held(sheet, Bleeding.class).orElseThrow().getRemainingRounds(),
                "two RD instances still take one Unidade");
    }

    /** ⚠️ The reading: each matching resistance takes its own Unidade. */
    @Test
    void rdAndRaEachTakeOne() {
        CharacterSheet sheet = sheet();
        grant(sheet, ModifierType.DAMAGE_REDUCTION);
        grant(sheet, ModifierType.ABSOLUTE_DAMAGE_REDUCTION);

        sheet.applyEffect(new Bleeding(2, Optional.of(3)));

        assertEquals(1, held(sheet, Bleeding.class).orElseThrow().getRemainingRounds());
    }

    @Test
    void anEffectShortenedToNothingNeverTakesHold() {
        CharacterSheet sheet = sheet();
        grant(sheet, ModifierType.DAMAGE_REDUCTION);

        sheet.applyEffect(new Bleeding(2, Optional.of(1)));

        assertTrue(held(sheet, Bleeding.class).isEmpty());
    }

    @Test
    void rdDoesNotReachADrainButRaDoes() {
        CharacterSheet rd = sheet();
        grant(rd, ModifierType.DAMAGE_REDUCTION);
        rd.applyEffect(new ManaDrain(1, Optional.of(3)));
        assertEquals(3, held(rd, ManaDrain.class).orElseThrow().getRemainingRounds());

        CharacterSheet ra = sheet();
        grant(ra, ModifierType.ABSOLUTE_DAMAGE_REDUCTION);
        ra.applyEffect(new ManaDrain(1, Optional.of(3)));
        assertEquals(2, held(ra, ManaDrain.class).orElseThrow().getRemainingRounds());
    }

    @Test
    void reShortensOnlyItsOwnElement() {
        CharacterSheet fire = sheet();
        fire.applyEffect(new TimedElementalResistance(ElementalType.FOGO, 1, 10, false));
        fire.applyEffect(new Poisoning(1, 3, null, List.of()));
        assertEquals(3, held(fire, Poisoning.class).orElseThrow().getRemainingRounds());

        CharacterSheet nature = sheet();
        nature.applyEffect(new TimedElementalResistance(ElementalType.NATURAL, 1, 10, false));
        nature.applyEffect(new Poisoning(1, 3, null, List.of()));
        assertEquals(2, held(nature, Poisoning.class).orElseThrow().getRemainingRounds());
    }

    @Test
    void aBuffIsNeverShortened() {
        CharacterSheet sheet = sheet();
        grant(sheet, ModifierType.ABSOLUTE_DAMAGE_REDUCTION);

        sheet.applyEffect(new TemporaryBonus(ModifierType.DEFESAS, 2, 3));

        assertEquals(3, sheet.getRunningEffects().stream()
                .filter(effect -> effect instanceof TemporaryBonus bonus && bonus.getType() == ModifierType.DEFESAS)
                .findFirst().orElseThrow().getRemainingRounds());
    }
}
