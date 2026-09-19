package org.aventyrs.core.scene;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.title.santo.AbencoadoPelaLuzAbility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The provoking Aura registered on a {@link Scene} — Orgulho Elduriano's shape. */
class ActiveAuraTest {

    private static final AbencoadoPelaLuzAbility SOURCE = AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO;

    private final UUID party = UUID.randomUUID();
    private final UUID foes = UUID.randomUUID();

    private Scene scene;
    private CharacterSheet holder;
    private CharacterSheet ally;
    private CharacterSheet nearFoe;
    private CharacterSheet farFoe;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        scene = new Scene();
        holder = newSheet();
        ally = newSheet();
        nearFoe = newSheet();
        farFoe = newSheet();
        scene.addParticipant(holder, 20, party);
        scene.addParticipant(ally, 15, party);
        scene.addParticipant(nearFoe, 10, foes);
        scene.addParticipant(farFoe, 5, foes);
    }

    private CharacterSheet newSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        return CharacterSheet.of(character, new Player());
    }

    private SceneContext holderContext(final Map<CombatantSheet, Range> distances) {
        return scene.buildContext(holder, distances);
    }

    private ActiveAura activate(final int rounds) {
        ActiveAura aura = new ActiveAura(holder, SOURCE, Range.DISTANCIA_CURTA, rounds);
        scene.addAura(aura);
        return aura;
    }

    private Map<CombatantSheet, Range> standardDistances() {
        Map<CombatantSheet, Range> distances = new HashMap<>();
        distances.put(ally, Range.ADJACENTE);
        distances.put(nearFoe, Range.DISTANCIA_CURTA);
        distances.put(farFoe, Range.DISTANCIA_MEDIA);
        return distances;
    }

    /** Starts combat and the first participant's Turn, so each {@link #finishRound()} wraps exactly once. */
    private void startCombat() {
        scene.startCombat();
        scene.next();
    }

    /** Advances the Scene through one whole Rodada of its four participants. */
    private void finishRound() {
        for (int i = 0; i < 4; i++) {
            scene.next();
        }
    }

    @Test
    void refreshBindsOnlyEnemiesWithinTheRadius() {
        ActiveAura aura = activate(3);

        List<CombatantSheet> bound = scene.refreshAura(holder, holderContext(standardDistances()));

        assertEquals(List.of(nearFoe), bound);
        assertTrue(aura.isBound(nearFoe.getId()));
        assertFalse(aura.isBound(farFoe.getId()));
        assertFalse(aura.isBound(ally.getId()));
        assertTrue(nearFoe.isAffectedUntilRest(SOURCE));
        assertFalse(farFoe.isAffectedUntilRest(SOURCE));
    }

    @Test
    void aFoeIsBoundOnlyOnce() {
        activate(3);
        scene.refreshAura(holder, holderContext(standardDistances()));

        assertEquals(List.of(), scene.refreshAura(holder, holderContext(standardDistances())));
    }

    @Test
    void aFoeEnteringRangeLaterIsBoundThen() {
        ActiveAura aura = activate(3);
        scene.refreshAura(holder, holderContext(standardDistances()));

        Map<CombatantSheet, Range> moved = standardDistances();
        moved.put(farFoe, Range.DISTANCIA_MUITO_CURTA);

        assertEquals(List.of(farFoe), scene.refreshAura(holder, holderContext(moved)));
        assertTrue(aura.isBound(farFoe.getId()));
    }

    @Test
    void aBoundFoeLeavingRangeStaysBound() {
        ActiveAura aura = activate(3);
        scene.refreshAura(holder, holderContext(standardDistances()));

        Map<CombatantSheet, Range> moved = standardDistances();
        moved.put(nearFoe, Range.DISTANCIA_LONGA);
        scene.refreshAura(holder, holderContext(moved));

        assertTrue(aura.isBound(nearFoe.getId()));
        assertEquals(Optional.of(holder), scene.getForcedAttackTarget(nearFoe));
    }

    @Test
    void aFoeAlreadyAffectedIsNotBoundAgainUntilADescansoLongo() {
        nearFoe.markAffectedUntilRest(SOURCE, RestType.LONGO);
        ActiveAura aura = activate(3);

        assertEquals(List.of(), scene.refreshAura(holder, holderContext(standardDistances())));
        assertFalse(aura.isBound(nearFoe.getId()));

        nearFoe.clearRestCooldowns(RestType.CURTO);
        assertTrue(nearFoe.isAffectedUntilRest(SOURCE));

        nearFoe.clearRestCooldowns(RestType.LONGO);
        assertFalse(nearFoe.isAffectedUntilRest(SOURCE));
        assertEquals(List.of(nearFoe), scene.refreshAura(holder, holderContext(standardDistances())));
    }

    @Test
    void aDescansoTotalAlsoClearsTheMark() {
        nearFoe.markAffectedUntilRest(SOURCE, RestType.LONGO);

        nearFoe.clearRestCooldowns(RestType.TOTAL);

        assertFalse(nearFoe.isAffectedUntilRest(SOURCE));
    }

    @Test
    void aBoundFoeMustAttackTheHolderFirstEachRodada() {
        startCombat();
        activate(3);
        scene.refreshAura(holder, holderContext(standardDistances()));

        assertEquals(Optional.of(holder), scene.getForcedAttackTarget(nearFoe));
        assertEquals(Optional.empty(), scene.getForcedAttackTarget(farFoe));

        scene.recordAttack(nearFoe, holder);
        assertEquals(Optional.empty(), scene.getForcedAttackTarget(nearFoe));

        finishRound();
        assertEquals(Optional.of(holder), scene.getForcedAttackTarget(nearFoe));
    }

    @Test
    void attackingSomeoneElseDoesNotDischargeTheObligation() {
        activate(3);
        scene.refreshAura(holder, holderContext(standardDistances()));

        scene.recordAttack(nearFoe, ally);

        assertEquals(Optional.of(holder), scene.getForcedAttackTarget(nearFoe));
    }

    @Test
    void afterAttackingTheHolderLaterAttacksOnOthersThatRodadaArePenalized() {
        startCombat();
        activate(3);
        scene.refreshAura(holder, holderContext(standardDistances()));

        assertEquals(0, scene.getAuraAttackPenalty(nearFoe, ally));

        scene.recordAttack(nearFoe, holder);
        assertEquals(Skill.DISADVANTAGE_MALUS, scene.getAuraAttackPenalty(nearFoe, ally));
        assertEquals(0, scene.getAuraAttackPenalty(nearFoe, holder));
        assertEquals(0, scene.getAuraAttackPenalty(farFoe, ally));

        finishRound();
        assertEquals(0, scene.getAuraAttackPenalty(nearFoe, ally));
    }

    @Test
    void theAuraExpiresAfterItsDuration() {
        startCombat();
        activate(2);
        scene.refreshAura(holder, holderContext(standardDistances()));

        finishRound();
        assertEquals(1, scene.getActiveAuras().size());

        finishRound();
        assertEquals(List.of(), scene.getActiveAuras());
        assertEquals(Optional.empty(), scene.getForcedAttackTarget(nearFoe));
        // Expiry ends the binding, but not the "once until a Descanso Longo" mark.
        assertTrue(nearFoe.isAffectedUntilRest(SOURCE));
    }

    @Test
    void removingTheHolderEndsTheirAura() {
        activate(3);
        scene.refreshAura(holder, holderContext(standardDistances()));

        scene.removeParticipant(holder);

        assertEquals(List.of(), scene.getActiveAuras());
        assertEquals(Optional.empty(), scene.getForcedAttackTarget(nearFoe));
    }

    @Test
    void refreshingForSomeoneWhoHoldsNoAuraBindsNobody() {
        activate(3);

        assertEquals(List.of(), scene.refreshAura(ally, scene.buildContext(ally, Map.of(nearFoe, Range.ADJACENTE))));
    }

    @Test
    void anAuraMustLastAtLeastOneRodada() {
        assertThrows(IllegalArgumentException.class, () -> new ActiveAura(holder, SOURCE, Range.DISTANCIA_CURTA, 0));
    }
}
