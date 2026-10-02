package org.aventyrs.core.ego;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.EgoValue;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.InitiativeEgoService;
import org.aventyrs.core.character.services.InitiativeEgoService.Span;
import org.aventyrs.core.character.services.InitiativeEgoServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.EgoPointType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.attention.AttentionInteraction;
import org.aventyrs.core.util.TranslatableMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Iniciativa's Ego spends (Ego plan Phase 7, {@code docs/rules/ego.txt}): the order changes, which take hold at
 * the next Rodada boundary and last the Rodadas they were bought for; the action economy; and the roll uses.
 */
class InitiativeSpendingTest {

    private final InitiativeEgoService service = new InitiativeEgoServiceImpl();

    private CharacterSheet quick;
    private CharacterSheet slow;
    private Scene scene;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        quick = sheet(3);
        slow = sheet(2);
        scene = new Scene();
        scene.addParticipant(quick, 10);
        scene.addParticipant(slow, 5);
        scene.startCombat();
        scene.next(); // quick's Turn, Rodada 1
    }

    private static CharacterSheet sheet(final int iniciativa) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .egos(CharacterEgos.builder().iniciativa(EgoValue.builder().base(iniciativa).build()).build())
                .build();
        return CharacterSheet.of(character, new Player());
    }

    /** Plays the rest of this Rodada into the next one's first Turn. */
    private void nextRodada() {
        int round = scene.getCurrentRound();
        while (scene.getCurrentRound() == round) {
            scene.next();
        }
    }

    // ---------- the order ----------

    /** "Reduzir … para qualquer valor menor por uma … Rodada": the next Rodada's order, then back. */
    @Test
    void loweringForARodadaReordersExactlyTheNextRodada() {
        service.lowerInitiative(scene, quick, 3, Span.RODADA);
        assertEquals(List.of(quick, slow), scene.getParticipantsInInitiativeOrder());

        nextRodada();
        assertEquals(List.of(slow, quick), scene.getParticipantsInInitiativeOrder());

        nextRodada();
        assertEquals(List.of(quick, slow), scene.getParticipantsInInitiativeOrder());
        assertEquals(2, quick.getTemporaryEgoPoints(EgoDomain.INICIATIVA));
    }

    @Test
    void aLowerThatIsNotLowerIsRefusedWithNothingSpent() {
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> service.lowerInitiative(scene, quick, 12, Span.RODADA));

        assertEquals(TranslatableMessages.INVALID_INITIATIVE_CHANGE, refused.getMessage());
        assertEquals(3, quick.getTemporaryEgoPoints(EgoDomain.INICIATIVA));
    }

    /** "Alterar seu valor … por uma Cena": holds Rodada after Rodada, and ends with the Cena. */
    @Test
    void settingForTheCenaHoldsUntilTheCenaEnds() {
        service.setInitiative(scene, slow, 20, Span.CENA);
        nextRodada();
        nextRodada();
        assertEquals(List.of(slow, quick), scene.getParticipantsInInitiativeOrder());
        assertEquals(1, slow.getPermanentEgoPoints(EgoDomain.INICIATIVA));

        slow.startNewScene();
        assertEquals(java.util.OptionalInt.empty(), slow.getInitiativeOverride());
    }

    /** A restored override that already started governs its remaining Rodadas, the current one included. */
    @Test
    void aRestoredStartedOverrideGovernsItsRemainingRodadas() {
        quick.overrideInitiative(1, 2, true);   // mid-way through two Rodadas: this one and one more

        nextRodada();
        assertEquals(List.of(slow, quick), scene.getParticipantsInInitiativeOrder());
        nextRodada();
        assertEquals(List.of(quick, slow), scene.getParticipantsInInitiativeOrder());
    }

    /** A Scene rebuilt around sheets already in it keeps their Cena — an override and the roll uses stay. */
    @Test
    void restoringAParticipantKeepsItsCena() {
        service.setInitiative(scene, slow, 20, Span.CENA);
        service.bankRollCharges(slow, InitiativeRollCharge.ADVANTAGE);

        Scene rebuilt = new Scene();
        rebuilt.restoreParticipant(quick, 10, java.util.UUID.randomUUID());
        rebuilt.restoreParticipant(slow, 5, java.util.UUID.randomUUID());

        assertEquals(List.of(slow, quick), rebuilt.getParticipantsInInitiativeOrder());
        assertEquals(2, slow.getCharges(InitiativeRollCharge.ADVANTAGE));
    }

    /** "Refazer sua rolagem de Iniciativa com Vantagem": the new total + 2, for the Cena. */
    @Test
    void aRerollStandsWithItsVantagemForTheCena() {
        service.rerollInitiative(scene, slow, 9);

        assertEquals(9 + Skill.ADVANTAGE_BONUS, scene.effectiveInitiativeOf(slow).getAsInt());
        nextRodada();
        assertEquals(List.of(slow, quick), scene.getParticipantsInInitiativeOrder());
    }

    /** "Alterar o valor de Iniciativa de um PdN por 2 Rodadas" — the payer spends, the foe moves. */
    @Test
    void anOpponentsInitiativeChangesForTwoRodadas() {
        service.setOpponentInitiative(scene, slow, quick, 1);
        nextRodada();
        assertEquals(List.of(slow, quick), scene.getParticipantsInInitiativeOrder());
        nextRodada();
        assertEquals(List.of(slow, quick), scene.getParticipantsInInitiativeOrder());
        nextRodada();
        assertEquals(List.of(quick, slow), scene.getParticipantsInInitiativeOrder());

        assertEquals(1, slow.getPermanentEgoPoints(EgoDomain.INICIATIVA));
        assertEquals(3, quick.getPermanentEgoPoints(EgoDomain.INICIATIVA));
    }

    // ---------- the action economy ----------

    @Test
    void aTemporaryPointBuysAnActionPointOrAReactionThisRodada() {
        service.gainActionPoint(quick);
        service.gainReaction(quick);

        assertEquals(1, quick.getTemporaryBonus(ModifierType.ACTION_POINTS));
        assertEquals(1, quick.getTemporaryBonus(ModifierType.REACTIONS));
        assertEquals(1, quick.getTemporaryEgoPoints(EgoDomain.INICIATIVA));
    }

    @Test
    void aPermanentPointBuysTwoActionPointsAndAReaction() {
        service.gainActionSurge(quick);

        assertEquals(2, quick.getTemporaryBonus(ModifierType.ACTION_POINTS));
        assertEquals(1, quick.getTemporaryBonus(ModifierType.REACTIONS));
        assertEquals(2, quick.getPermanentEgoPoints(EgoDomain.INICIATIVA));
    }

    /** "Não é possível usar um Ponto Temporário para ativar um efeito Permanente." */
    @Test
    void withNoPermanentPointTheSurgeIsRefused() {
        quick.spendEgoPoints(EgoDomain.INICIATIVA, EgoPointType.PERMANENT, 3);
        quick.receiveTemporaryEgoPoints(EgoDomain.INICIATIVA, "gm", 1);

        assertThrows(IllegalOperationException.class, () -> service.gainActionSurge(quick));
        assertEquals(0, quick.getTemporaryBonus(ModifierType.ACTION_POINTS));
    }

    // ---------- the rolls ----------

    /** "Vantagem em até duas rolagens de Perícia (efetuadas na mesma Cena)": two uses, then none. */
    @Test
    void oneTemporaryPointBuysVantagemOnTwoRolls() {
        service.bankRollCharges(quick, InitiativeRollCharge.ADVANTAGE);
        SkillRoll roll = new SkillRoll(List.of(3, 3, 3));
        int plain = new AttentionInteraction().applyTo(quick, null, roll).getSkillRollBonus();

        SkillRoll first = service.useRollCharge(quick, roll, InitiativeRollCharge.ADVANTAGE);
        service.useRollCharge(quick, roll, InitiativeRollCharge.ADVANTAGE);

        assertEquals(plain + Skill.ADVANTAGE_BONUS,
                new AttentionInteraction().applyTo(quick, null, first).getSkillRollBonus());
        IllegalOperationException none = assertThrows(IllegalOperationException.class,
                () -> service.useRollCharge(quick, roll, InitiativeRollCharge.ADVANTAGE));
        assertEquals(TranslatableMessages.NO_INITIATIVE_CHARGE_BANKED, none.getMessage());
    }

    /** "Reduzir o GD de até duas rolagens" — one nível each; the uses end with the Cena. */
    @Test
    void onePermanentPointEasesTwoRollsInTheCena() {
        service.bankRollCharges(quick, InitiativeRollCharge.DIFFICULTY_REDUCTION);
        SkillRoll roll = new SkillRoll(List.of(3, 3, 3));
        int plain = new AttentionInteraction().applyTo(quick, null, roll).getDifficultyReduction();

        SkillRoll eased = service.useRollCharge(quick, roll, InitiativeRollCharge.DIFFICULTY_REDUCTION);

        assertEquals(plain + 1, new AttentionInteraction().applyTo(quick, null, eased).getDifficultyReduction());
        quick.startNewScene();
        assertThrows(IllegalOperationException.class,
                () -> service.useRollCharge(quick, roll, InitiativeRollCharge.DIFFICULTY_REDUCTION));
    }
}
