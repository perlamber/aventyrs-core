package org.aventyrs.core.monster.summon;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.combat.AttackReceiver;
import org.aventyrs.core.combat.IncomingAttack;
import org.aventyrs.core.combat.IncomingAttackResult;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillRoll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A Zumbi in a real Cena de Combate — the end-to-end check that a summon reaches the same
 * machinery every other combatant does, with nothing monster-specific wired into `Scene`.
 */
class ZumbiInCombatTest {

    private final AttackReceiver attackReceiver = new AttackReceiver();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet hero() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID())
                .build();
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void aZumbiJoinsASceneThroughTheOrdinaryParticipantApi() {
        CharacterSheet hero = hero();
        MonsterSheet zumbi = Zumbi.summonedBy(10).spawn(new Player());

        Scene scene = new Scene();
        scene.setCombatScene(true);
        UUID party = UUID.randomUUID();
        UUID foes = UUID.randomUUID();
        scene.addParticipant(hero, 14, party);
        scene.addParticipant(zumbi, 6, foes);

        // addParticipant is typed CombatantSheet — no monster-specific entry point exists.
        assertEquals(List.of(zumbi), scene.getEnemies(hero));
        assertEquals(List.of(hero), scene.getEnemies(zumbi));
        assertTrue(scene.wonInitiative(hero));
    }

    @Test
    void aFullyEmpoweredZumbiDefendsAgainstAnEasierGrauDeDificuldade() {
        // The Graduação-10 clause, reaching its real consumer: AttackReceiver makes the incoming
        // attack's GD easier by the defender's own difficultyReduction.
        IncomingAttackResult plain = defend(Zumbi.summonedBy(9).spawn(new Player()));
        IncomingAttackResult empowered = defend(Zumbi.summonedBy(10).spawn(new Player()));

        assertEquals(DifficultyLevel.HARD.getBaseValue(), plain.getRequiredTotal());
        assertEquals(DifficultyLevel.MEDIUM.getBaseValue(), empowered.getRequiredTotal());
        assertEquals(1, empowered.getDefenseResult().getDifficultyReduction());
    }

    private IncomingAttackResult defend(final MonsterSheet zumbi) {
        return attackReceiver.resolve(IncomingAttack.builder()
                .defender(zumbi)
                .difficultyLevel(DifficultyLevel.HARD)
                .defenseType(DefenseType.PHYSICAL)
                .defenseRoll(new SkillRoll(List.of(3, 3, 2)))
                .build());
    }
}
