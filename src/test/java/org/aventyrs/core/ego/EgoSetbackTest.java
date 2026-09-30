package org.aventyrs.core.ego;

import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.EgoSetbackService;
import org.aventyrs.core.character.services.EgoSetbackServiceImpl;
import org.aventyrs.core.character.services.FreeActionsServiceImpl;
import org.aventyrs.core.character.services.MovementServiceImpl;
import org.aventyrs.core.character.services.ReactionsServiceImpl;
import org.aventyrs.core.effect.EffectChainServiceImpl;
import org.aventyrs.core.scene.InitiativeEntry;
import org.aventyrs.core.scene.InitiativePosition;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.EgoPointType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.attention.AttentionInteraction;
import org.aventyrs.core.util.TranslatableMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An Ego reaching zero (Ego plan Phase 9, {@code docs/rules/ego.txt}; table rulings 2026-09-30: zero is the permanent
 * points, and the setback lasts until the Ego has a point again).
 */
class EgoSetbackTest {

    private final EgoSetbackService service = new EgoSetbackServiceImpl();

    private CharacterSheet sheet;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        sheet = CharacterSheet.of(character, new Player());
    }

    /** Spends every permanent point of domain (the fixture's Egos are 2) and records face. */
    private EgoSetback atZero(final EgoDomain domain, final int face) {
        sheet.spendEgoPoints(domain, EgoPointType.PERMANENT, 2);
        return service.rollSetback(sheet, domain, face);
    }

    // ---------- owing, rolling, lifting ----------

    @Test
    void anEgoOwesItsRollOnlyOnceItsPermanentPointsAreGone() {
        sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.TEMPORARY, 2);
        assertEquals(List.of(), sheet.getOwedEgoSetbacks());

        sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.PERMANENT, 2);
        assertEquals(List.of(EgoDomain.SORTE), sheet.getOwedEgoSetbacks());

        assertEquals(EgoSetback.ATRAIR_A_MORTE, service.rollSetback(sheet, EgoDomain.SORTE, 4));
        assertEquals(List.of(), sheet.getOwedEgoSetbacks());
        assertTrue(sheet.hasEgoSetback(EgoSetback.ATRAIR_A_MORTE));
    }

    /** Recursos has no table — "extrema pobreza" is all it is. */
    @Test
    void recursosNeverOwesARoll() {
        sheet.spendEgoPoints(EgoDomain.RECURSOS, EgoPointType.PERMANENT, 2);

        assertEquals(List.of(), sheet.getOwedEgoSetbacks());
    }

    @Test
    void aRollNotOwedOrOffTheDieIsRefused() {
        IllegalOperationException notOwed = assertThrows(IllegalOperationException.class,
                () -> service.rollSetback(sheet, EgoDomain.SORTE, 1));
        assertEquals(TranslatableMessages.NO_EGO_SETBACK_OWED, notOwed.getMessage());

        sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.PERMANENT, 2);
        assertThrows(IllegalOperationException.class, () -> service.rollSetback(sheet, EgoDomain.SORTE, 7));
    }

    /** It holds only while the Ego is at zero — a sheet with a permanent point has none. */
    @Test
    void aSetbackHoldsOnlyWhileTheEgoIsAtZero() {
        sheet.recordEgoSetback(EgoSetback.REFLEXO_LENTO);

        assertFalse(sheet.hasEgoSetback(EgoSetback.REFLEXO_LENTO));
    }

    // ---------- Iniciativa ----------

    @Test
    void reflexoLentoAndHesitacaoTakeTheReacoesAndAcoesLivres() {
        atZero(EgoDomain.INICIATIVA, 6);
        assertEquals(0, new ReactionsServiceImpl().getTotalReactions(sheet, 1));

        CharacterSheet other = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
        other.spendEgoPoints(EgoDomain.INICIATIVA, EgoPointType.PERMANENT, 2);
        service.rollSetback(other, EgoDomain.INICIATIVA, 3);
        assertEquals(0, new FreeActionsServiceImpl().getTotalFreeActions(other, 1));
    }

    @Test
    void passosReduzidosHalvesTheMovimentoBase() {
        int before = new MovementServiceImpl().getMovementBase(sheet);

        atZero(EgoDomain.INICIATIVA, 5);

        assertEquals(before / 2, new MovementServiceImpl().getMovementBase(sheet));
    }

    /** Distração: no PA in the Cena's first Rodada (Rodada 0); Fraqueza: -1PA in an odd one. */
    @Test
    void distracaoAndFraquezaTakeTheirPontosDeAcao() {
        Scene scene = new Scene();
        scene.addParticipant(sheet, 10);
        scene.startCombat();
        SceneContext firstRodada = scene.buildContext(sheet, Map.of());
        int plain = new ActionPointsServiceImpl().getMaxActionPoints(sheet, 0, firstRodada);

        atZero(EgoDomain.INICIATIVA, 1);

        assertEquals(0, new ActionPointsServiceImpl().getMaxActionPoints(sheet, 0, firstRodada));
        assertTrue(plain > 0);
    }

    /** Lentidão: last in every Cena's order — an Iniciativa override it bought still holds first. */
    @Test
    void lentidaoPutsItLastUnlessAnOverrideHolds() {
        atZero(EgoDomain.INICIATIVA, 4);
        InitiativeEntry entry = new InitiativeEntry(sheet, 18, java.util.UUID.randomUUID());
        assertEquals(InitiativeEntry.LAST_IN_ORDER, entry.getEffectiveInitiativeValue());

        sheet.overrideInitiative(12, null);
        assertEquals(12, entry.getEffectiveInitiativeValue());
    }

    // ---------- Sorte ----------

    @Test
    void atrairAMorteLetsEveryCorrenteThatBeatsTheDefesasLand() {
        atZero(EgoDomain.SORTE, 4);

        assertEquals(0, new EffectChainServiceImpl().getRequiredMargin(null, InitiativePosition.UNKNOWN, sheet,
                InitiativePosition.UNKNOWN));
    }

    @Test
    void superficialidadeHasNoCriticalSuccesses() {
        atZero(EgoDomain.SORTE, 1);

        assertEquals(CriticalResult.NONE,
                new AttentionInteraction().applyTo(sheet, null, new SkillRoll(List.of(6, 6, 6))).getCriticalResult());
    }

    /** Insucesso: a success throws the 1d6 a Cego does, failing on 2 or less. */
    @Test
    void insucessoThrowsTheCheckDieOnEveryRoll() {
        assertEquals(OptionalInt.empty(), sheet.getBlindCheckThreshold(SkillType.ATTENTION, null));

        atZero(EgoDomain.SORTE, 3);

        assertEquals(OptionalInt.of(2), sheet.getBlindCheckThreshold(SkillType.ATTENTION, null));
    }

    /** Azarão: a failure by 5 or more is a Falha Crítica Menor. */
    @Test
    void azaraoTurnsABadFailureIntoACriticalOne() {
        atZero(EgoDomain.SORTE, 2);

        assertEquals(CriticalResult.FALHA_CRITICA_MENOR, new AttentionInteraction()
                .applyTo(sheet, null, new SkillRoll(List.of(2, 2, 3), null, 30)).getCriticalResult());
    }

    // ---------- Autocontrole ----------

    @Test
    void panicoIsApavoradoWhileItHolds() {
        atZero(EgoDomain.AUTOCONTROLE, 3);

        assertTrue(sheet.hasCondition(ConditionType.APAVORADO, null));
    }

    @Test
    void enfurecidosIsCompelledAndEveryFaceBlocksConcentration() {
        assertFalse(sheet.isSkillUsePrevented(SkillType.CONHECIMENTOS, AttributeDomain.GNOSE));

        atZero(EgoDomain.AUTOCONTROLE, 1);

        assertTrue(sheet.isCompelledToAttackNearest());
        assertTrue(sheet.isSkillUsePrevented(SkillType.CONHECIMENTOS, AttributeDomain.GNOSE));
    }
}
