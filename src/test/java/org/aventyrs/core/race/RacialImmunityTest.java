package org.aventyrs.core.race;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.effect.RouboDeVida;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.catalog.AnulacaoSpell;
import org.aventyrs.core.magic.catalog.ProfanarSpell;
import org.aventyrs.core.magic.catalog.VidaSpell;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.DamageReceipt;
import org.aventyrs.core.sheet.HealingSource;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A Raça's own immunities (core 0.0.90) — Vampiro, Nascido da Floresta and Troll. */
class RacialImmunityTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet of(final Race race) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).race(race).feats(new ArrayList<>()).build();
        return CharacterSheet.of(character, new Player());
    }

    private static CharacterSheet vampire() {
        return of(new Vampiro(Vampiro.VampiroLineage.NOSFERATU, new Human()));
    }

    /** Anatomia de Morto-Vivo: "imunes a efeitos Naturais" — Elemental: Natural damage. */
    @Test
    void aVampireIsImmuneToNaturalDamage() {
        CharacterSheet vampire = vampire();

        assertTrue(vampire.isImmuneToDamage(DamageType.ELEMENTAL,
                new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.NATURAL)));
        assertFalse(vampire.isImmuneToDamage(DamageType.ELEMENTAL,
                new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.FOGO)));
        assertFalse(of(new Human()).isImmuneToDamage(DamageType.ELEMENTAL,
                new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.NATURAL)));
    }

    /** "Não podem recuperar PV com magias Divinas" and "Magias Profanas não causam nenhum dano". */
    @Test
    void aVampireIsNotHealedByDivineMagicAndUntouchedByProfane() {
        CharacterSheet vampire = vampire();
        vampire.applyDamage(6);

        vampire.heal(4, HealingSource.spell(VidaSpell.REVIGORAR, null));
        assertEquals(6, vampire.getDamageTaken(), "Vida is a Magia divina");
        vampire.heal(4);
        assertEquals(2, vampire.getDamageTaken(), "an ordinary heal still lands");

        assertTrue(vampire.isImmuneToSpell(ProfanarSpell.LACERAR_A_ALMA));
        assertFalse(vampire.isImmuneToSpell(VidaSpell.REVIGORAR));
    }

    /** Imunidade a Magias: "apenas magias Primordiais e Umbrais os afetam" — and every Encantamento is refused. */
    @Test
    void aNascidoDaFlorestaIsImmuneToAllButPrimordialMagic() {
        CharacterSheet nascido = of(new NascidoDaFloresta(new Human()));

        assertTrue(nascido.isImmuneToSpell(VidaSpell.REVIGORAR));
        assertTrue(nascido.isImmuneToSpell(ProfanarSpell.LACERAR_A_ALMA));
        assertFalse(nascido.isImmuneToSpell(AnulacaoSpell.IDENTIFICACAO), "Anulação is Primordial");
        assertTrue(nascido.getCharacter().isImmuneToEnchantments());
    }

    /** Anatomia Vegetal: "imunes a efeitos de Roubo de Vida de personagens que não tenham Anatomia Vegetal". */
    @Test
    void aTrollRefusesRouboDeVidaFromAnyoneButAnotherTroll() {
        CharacterSheet troll = of(new Troll());
        troll.recordDamageReceived(new DamageReceipt(5, DamageType.FISICO, null));
        CharacterSheet human = of(new Human());
        human.applyDamage(4);
        CharacterSheet otherTroll = of(new Troll());
        otherTroll.applyDamage(4);

        new RouboDeVida(human, 3).applyTo(troll);
        new RouboDeVida(otherTroll, 3).applyTo(troll);

        assertEquals(4, human.getDamageTaken());
        assertEquals(1, otherTroll.getDamageTaken());
    }
}
