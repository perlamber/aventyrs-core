package org.aventyrs.core.scene;

import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.util.TranslatableMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Invocations in a Scene (core 0.0.92) — {@link Scene#addSummon}. */
class SceneSummonTest {

    private CharacterSheet caster;
    private CharacterSheet foe;
    private Scene scene;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        caster = sheet();
        foe = sheet();
        scene = new Scene();
        scene.addParticipant(caster, 20, UUID.randomUUID());
        scene.addParticipant(foe, 10, UUID.randomUUID());
        scene.startCombat();
        assertSame(caster, scene.next());
    }

    private static CharacterSheet sheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }

    /** "Own token, caster controls": it acts right after its caster, in the caster's group. */
    @Test
    void aSummonActsRightAfterItsCasterInItsGroup() {
        CharacterSheet summon = sheet();
        scene.addSummon(caster, summon, null, null);

        assertEquals(List.of(caster, summon, foe), scene.getParticipantsInInitiativeOrder());
        assertTrue(scene.getAllies(caster).contains(summon));
        assertSame(summon, scene.next());
        assertSame(foe, scene.next());
        assertSame(caster, scene.next());
        assertSame(summon, scene.next(), "keeps its place after the Rodada re-sorts");
    }

    @Test
    void aSummonLeavesAtTheBoundaryThatExhaustsItsDuracao() {
        CharacterSheet summon = sheet();
        scene.addSummon(caster, summon, null, 2);
        scene.next();
        scene.next();

        assertSame(caster, scene.next());   // Rodada 2
        assertTrue(scene.getSummon(summon).isPresent());
        scene.next();
        scene.next();

        assertSame(caster, scene.next());   // Rodada 3
        assertTrue(scene.getSummon(summon).isEmpty());
        assertEquals(List.of(caster, foe), scene.getParticipantsInInitiativeOrder());
    }

    /** "Se o animal fosse sofrer um dano letal, o animal desaparece." */
    @Test
    void aFallenSummonIsSentAway() {
        CharacterSheet summon = sheet();
        scene.addSummon(caster, summon, null, null);
        summon.applyDamage(1000);

        assertEquals(List.of(summon), scene.settleSummons());
        assertFalse(scene.getAllParticipants().contains(summon));
        assertTrue(scene.getSummons().isEmpty());
    }

    /** "Caso um novo … seja invocado o anterior desaparece ao final do Turno." */
    @Test
    void aNewerSummonOfTheGroupReplacesTheOlderAtTheEndOfTheCastersTurn() {
        CharacterSheet first = sheet();
        CharacterSheet second = sheet();
        CharacterSheet other = sheet();
        scene.addSummon(caster, first, "aliado", null);
        scene.addSummon(caster, other, "predador", null);
        scene.addSummon(caster, second, "aliado", null);
        assertTrue(scene.getSummon(first).orElseThrow().isReplaced());
        assertTrue(scene.getAllParticipants().contains(first), "still here until the Turn ends");

        // Summons act in the order they were invoked, behind their caster.
        assertSame(other, scene.next());

        assertFalse(scene.getAllParticipants().contains(first));
        assertTrue(scene.getAllParticipants().contains(other), "another group is untouched");
        assertEquals(List.of(caster, other, second, foe), scene.getParticipantsInInitiativeOrder());
    }

    @Test
    void aCasterLeavingTakesItsSummonsAlong() {
        CharacterSheet summon = sheet();
        scene.addSummon(caster, summon, null, null);

        scene.removeParticipant(caster);

        assertEquals(List.of(foe), scene.getAllParticipants());
        assertTrue(scene.getSummons().isEmpty());
    }

    /** A caster who joined mid-Rodada is pending, and so is what it invokes; one not in the Scene can't invoke. */
    @Test
    void aPendingCastersSummonWaitsWithItAndAnAbsentOneIsRefused() {
        CharacterSheet latecomer = sheet();
        CharacterSheet summon = sheet();
        scene.addParticipant(latecomer, 15, UUID.randomUUID());

        scene.addSummon(latecomer, summon, null, null);

        assertEquals(List.of(latecomer, summon), scene.getPendingParticipants());
        IllegalOperationException absent = assertThrows(IllegalOperationException.class,
                () -> scene.addSummon(sheet(), sheet(), null, null));
        assertEquals(TranslatableMessages.CHARACTER_SHEET_NOT_IN_SCENE, absent.getMessage());
    }
}
