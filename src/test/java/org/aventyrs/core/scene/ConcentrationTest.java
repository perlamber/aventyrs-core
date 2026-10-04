package org.aventyrs.core.scene;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.magic.invocation.NatureInvocationService;
import org.aventyrs.core.magic.invocation.NatureInvocationServiceImpl;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.MaleficioWard;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.dominiodomana.DominioDoManaCompetencyAbility;
import org.aventyrs.core.util.TranslatableMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * "Concentração + 2 Rodadas" (table ruling, 2026-10-03): 1PA per Rodada keeps the Magia active; taking damage or not
 * paying starts the 2 Rodadas, and a lost Concentração cannot be resumed. The Anciente (Despertar do Anciente,
 * Concentração + 2) is the sustained invocation throughout.
 */
class ConcentrationTest {

    private final NatureInvocationService invocations = new NatureInvocationServiceImpl();
    private final Player gm = new Player();
    private CharacterSheet druid;
    private CharacterSheet foe;
    private Scene scene;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        openCombat(List.of());
    }

    /** A druid trained in Domínio do Mana holding abilities, against one foe, on the druid's first Turn. */
    private void openCombat(final List<SkillCompetencyAbility> abilities) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.DOMINIO_DO_MANA, CharacterSkill.builder()
                        .skill(SkillType.DOMINIO_DO_MANA.newSkillInstance()).specializations(List.of())
                        .graduation(SkillGraduation.builder().graduationValue(4).build()).build());
        abilities.forEach(builder::skillCompetencyAbility);
        druid = CharacterSheet.of(builder.build(), new Player());
        foe = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
        scene = new Scene();
        scene.addParticipant(druid, 20, UUID.randomUUID());
        scene.addParticipant(foe, 10, UUID.randomUUID());
        scene.startCombat();
        assertSame(druid, scene.next());
    }

    private SceneSummon awakenAnciente() {
        return invocations.awakenAnciente(scene, druid, gm).summons().get(0);
    }

    /** Ends the druid's Turn and everyone else's, starting the druid's next one. */
    private void toTheDruidsNextTurn() {
        while (scene.next() != druid) {
            // the Anciente's and the foe's Turns
        }
    }

    /** Ends Turns until the foe's begins. */
    private void toTheFoesTurn() {
        while (scene.next() != foe) {
            // the Anciente's Turn, placed right after its caster
        }
    }

    /** Advances across the next Rodada boundary. */
    private void nextRodada() {
        int round = scene.getCurrentRound();
        while (scene.getCurrentRound() == round) {
            scene.next();
        }
    }

    @Test
    void theCastPaysForItsOwnRodada() {
        SceneSummon anciente = awakenAnciente();

        assertTrue(druid.isConcentrating());
        assertFalse(druid.isConcentrationUpkeepDue());
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                druid::payConcentrationUpkeep);
        assertEquals(TranslatableMessages.CONCENTRATION_UPKEEP_NOT_DUE, refused.getMessage());

        toTheDruidsNextTurn();
        assertTrue(anciente.isSustained());
    }

    @Test
    void oneActionPointEachRodadaKeepsItActive() {
        SceneSummon anciente = awakenAnciente();

        for (int rodada = 0; rodada < 3; rodada++) {
            toTheDruidsNextTurn();
            assertTrue(druid.isConcentrationUpkeepDue());
            ActionCost paid = druid.payConcentrationUpkeep();
            assertEquals(1, paid.spentActionPoints());
            assertFalse(druid.isConcentrationUpkeepDue());
        }

        assertTrue(druid.isConcentrating());
        assertTrue(anciente.isSustained());
        assertNull(anciente.getRemainingRounds());
    }

    @Test
    void theUpkeepIsOnlyPaidInTheCastersOwnTurn() {
        awakenAnciente();
        toTheFoesTurn();

        assertFalse(druid.isConcentrationUpkeepDue());
        assertThrows(IllegalOperationException.class, druid::payConcentrationUpkeep);
    }

    @Test
    void anUnpaidTurnStartsTheTrailingRodadas() {
        SceneSummon anciente = awakenAnciente();
        toTheDruidsNextTurn();

        scene.next();   // the druid's Turn ends unpaid

        assertFalse(druid.isConcentrating());
        assertFalse(anciente.isSustained());
        assertEquals(2, anciente.getRemainingRounds());
    }

    @Test
    void takingDamageBreaksItAndTheTrailingRodadasStartFromTheHit() {
        SceneSummon anciente = awakenAnciente();
        toTheFoesTurn();

        druid.applyDamage(1);
        scene.settleConcentration();

        assertFalse(druid.isConcentrating());
        assertEquals(2, anciente.getRemainingRounds());
        assertTrue(scene.getAllParticipants().contains(anciente.getSummon()));
    }

    @Test
    void aBrokenConcentrationCannotBeResumed() {
        awakenAnciente();
        druid.applyDamage(1);
        toTheDruidsNextTurn();

        assertFalse(druid.isConcentrating());
        assertFalse(druid.isConcentrationUpkeepDue());
        assertThrows(IllegalOperationException.class, druid::payConcentrationUpkeep);
    }

    @Test
    void theTrailingRodadasRunOutAndTheInvocationLeaves() {
        SceneSummon anciente = awakenAnciente();
        CombatantSheet summon = anciente.getSummon();
        druid.applyDamage(1);

        scene.next();   // the druid's Turn ends; the loss is settled
        assertEquals(2, anciente.getRemainingRounds());

        nextRodada();   // Rodada 2
        assertTrue(scene.getAllParticipants().contains(summon));
        nextRodada();   // Rodada 3
        assertFalse(scene.getAllParticipants().contains(summon));
    }

    @Test
    void payingAPvCostIsNotTakingDamage() {
        awakenAnciente();

        druid.payWithVitality(1);

        assertTrue(druid.isConcentrating());
    }

    @Test
    void aNewConcentracaoMagiaBeginsAnotherAfterTheLoss() {
        SceneSummon first = awakenAnciente();
        druid.applyDamage(1);

        SceneSummon second = awakenAnciente();

        assertFalse(first.isSustained(), "the lost one is released, never resumed");
        assertTrue(second.isSustained());
        assertTrue(druid.isConcentrating());
    }

    /** Concentração Inabalável: "não perde a Concentração … após sofrer Danos" — the upkeep is still owed. */
    @Test
    void concentracaoInabalavelKeepsItThroughDamageButNotThroughAnUnpaidTurn() {
        openCombat(List.of(DominioDoManaCompetencyAbility.CONCENTRACAO_INABALAVEL));
        SceneSummon anciente = awakenAnciente();

        druid.applyDamage(1);
        scene.settleConcentration();
        assertTrue(druid.isConcentrating());
        assertTrue(anciente.isSustained());

        toTheDruidsNextTurn();
        scene.next();   // the druid's Turn ends unpaid

        assertFalse(druid.isConcentrating());
        assertEquals(2, anciente.getRemainingRounds());
    }

    @Test
    void aWardOnTheCastersOwnSheetStartsCountingAtTheHit() {
        MaleficioWard ward = new MaleficioWard(druid.getId(), 2);
        druid.applyEffect(ward);
        druid.beginConcentration();

        druid.applyDamage(1);

        assertNull(ward.getTrailingRounds());
        assertEquals(2, ward.getRemainingRounds());
    }
}
