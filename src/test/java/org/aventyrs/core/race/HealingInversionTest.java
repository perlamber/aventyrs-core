package org.aventyrs.core.race;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.effect.DamageInteraction;
import org.aventyrs.core.magic.catalog.ProfanarSpell;
import org.aventyrs.core.magic.catalog.VidaSpell;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.monster.summon.Zumbi;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.HealingSource;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Healing inversion (core 0.0.91) — the undead hurt by Divine heals, a Vampiro healed by Profane Magias. */
class HealingInversionTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /** "Sofrem Danos de Magias Divinas que recuperam PV ao invés de se curarem." */
    @Test
    void aDivineHealDamagesTheZumbi() {
        MonsterSheet zumbi = Zumbi.builder().build().spawn(new Player());
        zumbi.applyDamage(2);

        zumbi.heal(5, HealingSource.spell(VidaSpell.REVIGORAR, null));
        assertEquals(7, zumbi.getDamageTaken(), "the 5 it would have healed land as damage");

        zumbi.heal(3, HealingSource.rest(RestType.CURTO));
        assertEquals(4, zumbi.getDamageTaken(), "any other heal still heals");
    }

    /** "Magias Profanas não causam nenhum dano aos mortos, ao invés disso os curam em 1d6+Metade do Vigor." */
    @Test
    void aProfaneMagiaHealsAVampireInsteadOfHurtingIt() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>())
                .race(new Vampiro(Vampiro.VampiroLineage.NOSFERATU, new Human())).build();
        CharacterSheet vampire = CharacterSheet.of(character, new Player());
        vampire.applyDamage(10);
        int vigorHalf = character.getEffectiveAttributeTotal(AttributeDomain.VIGOR) / 2;

        InteractionResult result = new DamageInteraction(new DamageServiceImpl())
                .fromSpell(ProfanarSpell.LACERAR_A_ALMA)
                .withDiceRoller(() -> 4)
                .applyTo(vampire, null, DamageType.MAGICO, null, 8, false, null);

        assertEquals(0, result.getResourceLossValue());
        assertEquals(4 + vigorHalf, result.getResourceGainValue());
        assertEquals(10 - 4 - vigorHalf, vampire.getDamageTaken());
    }

    @Test
    void withNoDiceTheProfaneMagiaSimplyDealsNothing() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>())
                .race(new Vampiro(Vampiro.VampiroLineage.NOSFERATU, new Human())).build();
        CharacterSheet vampire = CharacterSheet.of(character, new Player());
        vampire.applyDamage(10);

        new DamageInteraction(new DamageServiceImpl()).fromSpell(ProfanarSpell.LACERAR_A_ALMA)
                .applyTo(vampire, null, DamageType.MAGICO, null, 8, false, null);

        assertEquals(10, vampire.getDamageTaken());
    }
}
