package org.aventyrs.core.magic.invocation;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.monster.summon.Zumbi;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneSummon;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.util.TranslatableMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Corpses, raising and the Foco limit (core 0.1.6.24; table rulings 2026-10-14). */
class RaiseDeadTest {

    private final RaiseDeadService raiseDead = new RaiseDeadServiceImpl();
    private final Player gm = new Player();
    private CharacterSheet necromancer;
    private CharacterSheet fallenHero;
    private Scene scene;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        necromancer = withFocus(3);
        fallenHero = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
        killOutright(fallenHero);
        scene = new Scene();
        scene.addParticipant(necromancer, 20, UUID.randomUUID());
        scene.addParticipant(fallenHero, 10, UUID.randomUUID());
        scene.startCombat();
        assertSame(necromancer, scene.next());
    }

    private static CharacterSheet withFocus(final int focus) {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .focus(AttributeValue.builder().domain(AttributeDomain.FOCUS).base(focus).build())
                        .build())
                .build(), new Player());
    }

    private static void killOutright(final CombatantSheet sheet) {
        int max = new HitPointsServiceImpl().getMaxHitPoints(sheet.getCharacter(), sheet);
        sheet.applyDamage(max * 2);
    }

    private SceneSummon raise(final CombatantSheet corpse) {
        raiseDead.requireRaisable(scene, corpse);
        return scene.raise(necromancer, corpse, Zumbi.summonedBy(0).spawn(gm), null, 2, true);
    }

    @Test
    void aDeadPlayerCharacterCanBeRaisedAndLeavesTheScene() {
        assertTrue(raiseDead.isRaisable(scene, fallenHero));

        SceneSummon zumbi = raise(fallenHero);

        assertFalse(scene.getAllParticipants().contains(fallenHero), "the corpse token is replaced");
        assertTrue(zumbi.isRaisedFromCorpse());
        assertEquals(1, zumbi.getSummon().getTimesRaised());
    }

    @Test
    void theLivingAndTheInvokedAreNoCorpses() {
        assertFalse(raiseDead.isRaisable(scene, necromancer));
        SceneSummon zumbi = raise(fallenHero);
        assertFalse(raiseDead.isRaisable(scene, zumbi.getSummon()), "a standing invocação is not a body");
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> raiseDead.requireRaisable(scene, necromancer));
        assertEquals(TranslatableMessages.RAISE_REQUIRES_CORPSE, refused.getMessage());
    }

    @Test
    void aFallenRaisedZumbiLeavesItsCorpseBehind() {
        CombatantSheet zumbi = raise(fallenHero).getSummon();

        killOutright(zumbi);
        scene.settleSummons();

        assertTrue(scene.getAllParticipants().contains(zumbi), "the body stays where it stood");
        assertTrue(scene.getSummon(zumbi).isEmpty(), "but it is nobody's invocação any more");
        assertTrue(zumbi.isCorpse());
        assertTrue(raiseDead.isRaisable(scene, zumbi));
    }

    @Test
    void aRaisedZumbiWhoseMagiaEndsLeavesItsCorpseToo() {
        CombatantSheet zumbi = scene.raise(necromancer, fallenHero, Zumbi.summonedBy(0).spawn(gm), null, 1, false)
                .getSummon();

        // Turns pass until the Rodada boundary spends its one Rodada.
        while (scene.getSummon(zumbi).isPresent()) {
            scene.next();
        }

        assertTrue(scene.getAllParticipants().contains(zumbi));
        assertTrue(zumbi.isCorpse());
    }

    @Test
    void anInvocationNotRaisedFromACorpseSimplyGoes() {
        MonsterSheet zumbi = Zumbi.summonedBy(0).spawn(gm);
        scene.addSummon(necromancer, zumbi, null, null);

        killOutright(zumbi);
        scene.settleSummons();

        assertFalse(scene.getAllParticipants().contains(zumbi));
    }

    @Test
    void aBodyRaisedFourTimesCanNeverBeRaisedAgain() {
        CombatantSheet body = fallenHero;
        for (int raise = 1; raise <= RaiseDeadService.MAX_RAISES; raise++) {
            CombatantSheet zumbi = raise(body).getSummon();
            assertEquals(raise, zumbi.getTimesRaised());
            killOutright(zumbi);
            scene.settleSummons();
            body = zumbi;
        }

        assertFalse(raiseDead.isRaisable(scene, body));
        final CombatantSheet exhausted = body;
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> raiseDead.requireRaisable(scene, exhausted));
        assertEquals(TranslatableMessages.CORPSE_EXHAUSTED, refused.getMessage());
    }

    @Test
    void theFocoLimitRefusesAnInvocationPastIt() {
        CharacterSheet novice = withFocus(1);
        scene.addParticipant(novice, 5, UUID.randomUUID());
        scene.addSummon(novice, Zumbi.summonedBy(0).spawn(gm), null, null);

        assertEquals(0, SummonLimit.remaining(scene, novice, null));
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> SummonLimit.requireRoom(scene, novice, 1, null));
        assertEquals(TranslatableMessages.SUMMON_LIMIT_REACHED, refused.getMessage());
    }

    @Test
    void anUncontrolledInvocationDoesNotCount() {
        CharacterSheet novice = withFocus(1);
        scene.addParticipant(novice, 5, UUID.randomUUID());
        MonsterSheet hungry = Zumbi.summonedBy(0).spawn(gm);
        scene.addSummon(novice, hungry, null, null);
        scene.releaseControlOf(hungry);

        assertEquals(1, SummonLimit.remaining(scene, novice, null));
    }
}
