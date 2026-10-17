package org.aventyrs.core.scene;

import org.aventyrs.core.ability.InstinctAbility;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.PreInitiativeService;
import org.aventyrs.core.character.services.PreInitiativeServiceImpl;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paranoia Saudável's Rodada 0.0 (core 0.1.7.15, table ruling 2026-10-16): 2PD, once per combat, one action of
 * Tempo ≤ 2PA, Ação Livre or Reação before anyone's first Turn, ignoring Iniciativa — and Rodada 0 untouched.
 */
class PreInitiativePhaseTest {

    private final PreInitiativeService service = new PreInitiativeServiceImpl();

    private CharacterSheet paranoid;
    private CharacterSheet other;
    private Scene scene;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        paranoid = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).id(UUID.randomUUID())
                .attributes(org.aventyrs.core.character.CharacterAttributes.builder()
                        .instinct(org.aventyrs.core.character.AttributeValue.builder()
                                .domain(org.aventyrs.core.character.AttributeDomain.INSTINCT).base(3).build())
                        .build())
                .attributeAbility(InstinctAbility.PARANOIA_SAUDAVEL).build(), new Player());
        other = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).id(UUID.randomUUID()).build(),
                new Player());
        scene = new Scene();
        scene.addParticipant(other, 20);
        scene.addParticipant(paranoid, 5);
    }

    @Test
    void theHolderActsOnceBeforeTheFirstTurnAndRodadaZeroIsUntouched() {
        scene.startCombat();
        assertTrue(service.canTake(paranoid, scene));
        assertEquals(2, service.take(paranoid, scene));

        scene.beginPreInitiativeTurn(paranoid);
        assertSame(paranoid, scene.getPreInitiativeActor());
        assertTrue(scene.permitsPreInitiativeAction(paranoid, ActionCost.ofActionPoints(2)));
        assertFalse(scene.permitsPreInitiativeAction(paranoid, ActionCost.ofActionPoints(3)));
        assertFalse(scene.permitsPreInitiativeAction(other, ActionCost.ofActionPoints(1)));
        scene.takePreInitiativeAction(paranoid, ActionCost.FREE_ACTION);
        assertFalse(scene.permitsPreInitiativeAction(paranoid, ActionCost.FREE_ACTION), "one action");

        assertSame(other, scene.next(), "Rodada 0 still opens on the highest Iniciativa");
        assertEquals(0, scene.getCurrentRound());
        assertNull(scene.getPreInitiativeActor());
    }

    @Test
    void itIsOncePerCombatAndOnlyAtItsStart() {
        assertFalse(service.canTake(paranoid, scene), "not a combat yet");
        scene.startCombat();
        service.take(paranoid, scene);
        assertFalse(service.canTake(paranoid, scene), "once per combat");
        assertThrows(IllegalOperationException.class, () -> service.take(paranoid, scene));

        scene.next();
        assertThrows(IllegalOperationException.class, () -> scene.beginPreInitiativeTurn(paranoid));
    }

    @Test
    void withoutTheHabilidadeNothingIsOffered() {
        scene.startCombat();
        assertEquals(0, service.getCost(other));
        assertFalse(service.canTake(other, scene));
    }
}
