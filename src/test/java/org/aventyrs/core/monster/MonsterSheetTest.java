package org.aventyrs.core.monster;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.item.ArmorItem;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Bleeding;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.DifficultyLevel;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A MonsterSheet has to behave exactly like a CharacterSheet everywhere combat touches it —
 * that's the whole premise of the {@link CombatantSheet} split. These assert the shared half
 * really is shared, and that the player-only half genuinely isn't there.
 */
class MonsterSheetTest {

    private MonsterSheet capanga() {
        return GenericMonster.CAPANGA.spawn(new Player());
    }

    @Test
    void aMonsterIsACombatantSheetButNotACharacterSheet() {
        MonsterSheet monster = capanga();

        assertInstanceOf(CombatantSheet.class, monster);
        // Checked reflectively rather than with instanceof, because `monster instanceof
        // CharacterSheet` does not compile — the types are unrelated. That compile error IS the
        // guarantee: the four XP-spending services take the concrete CharacterSheet, so no
        // monster can be passed to one, and no runtime check or isMonster() flag is needed.
        assertFalse(CharacterSheet.class.isAssignableFrom(MonsterSheet.class));
    }

    @Test
    void damageAndShieldsBehaveAsOnAPlayerSheet() {
        MonsterSheet monster = capanga();

        monster.addShield(4);
        monster.applyDamage(10);

        assertEquals(6, monster.getDamageTaken());
        assertEquals(0, monster.getShieldPoints());

        monster.heal(2);
        assertEquals(4, monster.getDamageTaken());
    }

    @Test
    void curseDamageBypassesShieldsAsOnAPlayerSheet() {
        MonsterSheet monster = capanga();

        monster.addShield(5);
        monster.applyCurseDamage(3);

        assertEquals(3, monster.getDamageTaken());
        assertEquals(5, monster.getShieldPoints());
    }

    @Test
    void temporaryBonusesAndTheirTickingBehaveAsOnAPlayerSheet() {
        MonsterSheet monster = capanga();

        monster.grantTemporaryBonus(ModifierType.SKILL_ROLL_BONUS, 3, 2);
        assertEquals(3, monster.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));

        monster.finishTurn();
        assertEquals(3, monster.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
        monster.finishTurn();
        assertEquals(0, monster.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS));
    }

    @Test
    void anEfeitoAppliedToAMonsterDrainsItPerRodada() {
        MonsterSheet monster = capanga();

        monster.applyEffect(new Bleeding(1, Optional.of(2)));

        monster.finishTurn();
        assertEquals(1, monster.getDamageTaken());
        monster.finishTurn();
        assertEquals(2, monster.getDamageTaken());
    }

    @Test
    void temporaryEgoPointsWorkOnAMonsterSoPrimorCanLandOnOne() {
        MonsterSheet monster = capanga();

        monster.gainTemporaryEgoPoints(EgoDomain.SORTE, 2);
        assertEquals(2, monster.getTemporaryEgoPoints(EgoDomain.SORTE));
        assertEquals(1, monster.spendTemporaryEgoPoints(EgoDomain.SORTE, 1));
    }

    @Test
    void theTurnLifecycleWorksAsOnAPlayerSheet() {
        MonsterSheet monster = capanga();

        assertTrue(monster.consumeFirstRollThisTurn(AttributeDomain.DEXTERITY));
        assertFalse(monster.consumeFirstRollThisTurn(AttributeDomain.DEXTERITY));

        monster.startTurn(1);
        assertTrue(monster.consumeFirstRollThisTurn(AttributeDomain.DEXTERITY));
    }

    @Test
    void aMonsterCanCarryLoot() {
        MonsterSheet monster = capanga();

        monster.addToInventory(ArmorItem.COURACA);
        assertEquals(1, monster.getInventory().size());
        assertTrue(monster.removeFromInventory(ArmorItem.COURACA));
        assertTrue(monster.getInventory().isEmpty());
    }

    @Test
    void everySpawnGetsItsOwnIdentity() {
        assertNotEquals(capanga().getId(), capanga().getId());
    }

    @Test
    void aKnownIdCanBeRestoredForReconstructionFromPersistedState() {
        UUID existing = UUID.randomUUID();
        MonsterSheet reconstructed = MonsterSheet.of(capanga().getCharacter(), new Player(), 13, 11,
                DifficultyLevel.EASY, 0, existing);

        assertEquals(existing, reconstructed.getId());
    }

    @Test
    void getDefenseSelectsTheRightColumn() {
        MonsterSheet conjurador = GenericMonster.CONJURADOR.spawn(new Player());

        assertEquals(conjurador.getPhysicalDefense(), conjurador.getDefense(DefenseType.PHYSICAL));
        assertEquals(conjurador.getMagicDefense(), conjurador.getDefense(DefenseType.MAGIC));
        assertNotEquals(conjurador.getDefense(DefenseType.PHYSICAL), conjurador.getDefense(DefenseType.MAGIC));
    }
}
