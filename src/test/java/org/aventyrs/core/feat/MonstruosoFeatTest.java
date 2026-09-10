package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link MonstruosoFeat#FEROCIDADE} — a flat dano bonus of {@code 1 + }Títulos Despertos on an
 * attack the holder makes with an Arma Natural, resolved through the trailing-{@code AttackSource}
 * overload of {@code Feat#resolveDamageBonus}.
 */
class MonstruosoFeatTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet feralSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID()).feats(new ArrayList<>()).build();
        character.grantFeat(MonstruosoFeat.FEROCIDADE);
        return CharacterSheet.of(character, new Player());
    }

    private static CharacterSheet victimSheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).id(UUID.randomUUID()).build(),
                new Player());
    }

    private static SceneContext at(final CombatantSheet other, final Range distance) {
        return new SceneContext(List.of(), List.of(other), Map.of(other, distance));
    }

    private static InteractionResult meleeWith(final CharacterSheet attacker, final CombatantSheet victim,
                                               final Weapon weapon) {
        return new AtaqueCorpoACorpoInteraction().applyTo(attacker, at(victim, Range.ADJACENTE), null, victim, weapon);
    }

    @Test
    void ferocidadeAddsAFlatBonusToANaturalWeaponDanoRoll() {
        CharacterSheet attacker = feralSheet();
        CharacterSheet victim = victimSheet();

        // Blank character has no Títulos: bonus is a flat +1, typed FISICO (untyped → physical).
        InteractionResult result = meleeWith(attacker, victim, NaturalWeapon.GARRAS_AFIADAS);
        assertEquals(1, result.getDamageBonus().getValue());
        assertEquals(DamageType.FISICO, result.getDamageBonus().getType());
    }

    @Test
    void ferocidadeGrantsNothingWithAWieldedNonNaturalWeapon() {
        Weapon dagger = AbstractWeapon.builder().name("Adaga").category(ItemCategory.LIGHT_BLADE)
                .damageBase(DamageBase.of(1, 1)).skillType(SkillType.ATAQUE_CORPO_A_CORPO).build();

        assertNull(meleeWith(feralSheet(), victimSheet(), dagger).getDamageBonus());
    }

    @Test
    void ferocidadeGrantsNothingWhenTheCallerDoesNotSayWhatTheAttackWasMadeWith() {
        assertNull(meleeWith(feralSheet(), victimSheet(), null).getDamageBonus());
    }

    @Test
    void aCharacterWithoutFerocidadeGetsNothingFromANaturalWeapon() {
        CharacterSheet plain = CharacterSheet.of(
                CharacterFixture.blank(CharacterFixture.BLANK).id(UUID.randomUUID()).feats(new ArrayList<>()).build(),
                new Player());

        assertNull(meleeWith(plain, victimSheet(), NaturalWeapon.GARRAS_AFIADAS).getDamageBonus());
    }

    @Test
    void selvageriaRaisesTheDanoBaseOfANaturalWeaponOnly() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>()).build();

        assertEquals(1, MonstruosoFeat.SELVAGERIA.resolveDamageBaseIncrease(character, NaturalWeapon.GARRAS_AFIADAS));
        assertEquals(0, MonstruosoFeat.SELVAGERIA.resolveDamageBaseIncrease(character, (Weapon) null));
    }
}
