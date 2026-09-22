package org.aventyrs.core.sheet;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.GorgonaFeat;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.race.Fada;
import org.aventyrs.core.race.Furia;
import org.aventyrs.core.race.Gorgona;
import org.aventyrs.core.race.Race;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code CombatantSheet#applyEnchantment} — the single door every Efeito de Encantamento comes
 * through, and the one place its tag does any work. Everything asserted here is resolved by the
 * <b>recipient</b>; nothing about the caster is consulted.
 */
class EnchantmentApplicationTest {

    private CharacterSheet enchanter;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        enchanter = sheetOf(null);
    }

    private CharacterSheet sheetOf(final Race race) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK);
        if (race != null) {
            builder.race(race);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    private ForcedTargeting compulsion(final int rounds) {
        return new ForcedTargeting(enchanter, rounds);
    }

    @Test
    void anOrdinaryRecipientTakesItUnchanged() {
        CharacterSheet target = sheetOf(null);

        assertTrue(target.applyEnchantment(compulsion(2)));
        assertEquals(2, target.getForcedTargeting().orElseThrow().getRemainingRounds());
    }

    // --- Immunity, which is the Raça's business ------------------------------------------------

    /** "são imunes aos efeitos diretos de Encantamentos" — Fada, Fúria and Górgona alike. */
    @Test
    void anImmuneRaceTakesNothingAtAll() {
        for (Race race : new Race[] {new Fada(), new Furia(), new Gorgona()}) {
            CharacterSheet target = sheetOf(race);

            assertFalse(target.applyEnchantment(compulsion(2)), race.getClass().getSimpleName());
            assertTrue(target.getForcedTargeting().isEmpty(), race.getClass().getSimpleName());
        }
    }

    /** "não possui a Característica Racial Imunidade a Encantamentos." */
    @Test
    void marcaDaMaldicaoStripsAGorgonasImmunity() {
        Character gorgona = CharacterFixture.blank(CharacterFixture.BLANK)
                .race(new Gorgona())
                .feats(new java.util.ArrayList<>())
                .build();
        assertTrue(gorgona.isImmuneToEnchantments());

        gorgona.grantFeat(GorgonaFeat.MARCA_DA_MALDICAO);

        assertFalse(gorgona.isImmuneToEnchantments());
        assertTrue(CharacterSheet.of(gorgona, new Player()).applyEnchantment(compulsion(2)));
    }

    /** An ordinary Raça is not immune, so the view must not answer true for everyone. */
    @Test
    void anOrdinaryRaceIsNotImmune() {
        assertFalse(sheetOf(null).getCharacter().isImmuneToEnchantments());
    }

    // --- Duração halving, which is the recipient's own ward -------------------------------------

    private void ward(final CharacterSheet target, final ItemCategory... blessed) {
        target.applyEffect(new Ungido(3, EnumSet.copyOf(java.util.List.of(blessed))));
    }

    /** "a Duração de efeitos nocivos de Encantamentos … são reduzidas pela metade." */
    @Test
    void aRecipientWardedByBothItemsTakesAHarmfulOneAtHalfDuracao() {
        CharacterSheet target = sheetOf(null);
        ward(target, ItemCategory.ARMOR, ItemCategory.SHIELD);

        target.applyEnchantment(compulsion(4));

        assertEquals(2, target.getForcedTargeting().orElseThrow().getRemainingRounds());
    }

    /** Rounded up: halving shortens a Duração, and never removes one outright. */
    @Test
    void halvingAnOddDuracaoRoundsUpAndNeverReachesZero() {
        CharacterSheet target = sheetOf(null);
        ward(target, ItemCategory.ARMOR, ItemCategory.SHIELD);

        target.applyEnchantment(compulsion(1));

        assertEquals(1, target.getForcedTargeting().orElseThrow().getRemainingRounds());
    }

    /** "uma Armadura **e** um Escudo" — one blessed item is not the ward. */
    @Test
    void oneBlessedItemAloneDoesNotHalveAnything() {
        CharacterSheet target = sheetOf(null);
        ward(target, ItemCategory.ARMOR);

        target.applyEnchantment(compulsion(4));

        assertEquals(4, target.getForcedTargeting().orElseThrow().getRemainingRounds());
    }

    @Test
    void anUnwardedRecipientTakesTheFullDuracao() {
        CharacterSheet target = sheetOf(null);

        target.applyEnchantment(compulsion(4));

        assertEquals(4, target.getForcedTargeting().orElseThrow().getRemainingRounds());
    }
}
