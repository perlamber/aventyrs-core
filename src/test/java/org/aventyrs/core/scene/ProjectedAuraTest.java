package org.aventyrs.core.scene;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.title.santo.Santo;
import org.aventyrs.core.title.santo.SantoAbility;
import org.aventyrs.core.title.santo.SantoSpecialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Santo's Título-Primário clause as a live Aura — "aliados adjacentes a você recebem Bônus em
 * Defesas iguais à metade dos Bônus que você receber em função do Efeito Base deste Título."
 *
 * <p>The case worth reading first is {@link #theAmountIsTheHoldersAdjacencyNotTheRecipients}: it is
 * the whole reason this is granted holder-side rather than scanned from the recipient, and the one
 * a recipient-side scan would get wrong.
 */
class ProjectedAuraTest {

    private final DefenseService defenseService = new DefenseServiceImpl();

    private Scene scene;
    private UUID party;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        scene = new Scene();
        party = UUID.randomUUID();
    }

    private CharacterSheet plainSheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }

    /** A Santo with no Especialização or Suprema: base effect 2 + adjacent allies, ally half is 1+. */
    private CharacterSheet santoSheet(final TitleSlot slot) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        character.grantTitle(new Santo(List.of(), List.of()), slot);
        return CharacterSheet.of(character, new Player());
    }

    private SceneContext holderContextWith(final CombatantSheet holder, final Map<CombatantSheet, Range> distances) {
        return scene.buildContext(holder, distances);
    }

    private int defesas(final CombatantSheet sheet) {
        return defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL);
    }

    @Test
    void anAdjacentAllyOfAPrimarySantoGainsHalfTheBaseEffect() {
        CombatantSheet santo = santoSheet(TitleSlot.PRIMARY);
        CombatantSheet ally = plainSheet();
        scene.addParticipant(santo, 20, party);
        scene.addParticipant(ally, 10, party);
        int before = defesas(ally);

        scene.refreshProjectedAuras(santo, holderContextWith(santo, Map.of(ally, Range.ADJACENTE)));

        // Santo's own base effect with one adjacent ally is 2 + 1 = 3; half, rounded down, is 1.
        assertEquals(before + 1, defesas(ally));
    }

    /**
     * The figure is half of what the <b>holder</b> gets, and the holder's own bonus counts the
     * holder's adjacent allies. With two allies beside the Santo its base effect is 2 + 2 = 4, so
     * each of them receives 2 — not the 1 a scan running from either recipient would have computed
     * from seeing only itself beside the Santo.
     */
    @Test
    void theAmountIsTheHoldersAdjacencyNotTheRecipients() {
        CombatantSheet santo = santoSheet(TitleSlot.PRIMARY);
        CombatantSheet allyOne = plainSheet();
        CombatantSheet allyTwo = plainSheet();
        scene.addParticipant(santo, 20, party);
        scene.addParticipant(allyOne, 15, party);
        scene.addParticipant(allyTwo, 10, party);
        int oneBefore = defesas(allyOne);
        int twoBefore = defesas(allyTwo);

        scene.refreshProjectedAuras(santo, holderContextWith(santo,
                Map.of(allyOne, Range.ADJACENTE, allyTwo, Range.ADJACENTE)));

        assertEquals(oneBefore + 2, defesas(allyOne));
        assertEquals(twoBefore + 2, defesas(allyTwo));
    }

    /** "Se este for seu Título Primário" — structural, and a Secundário Santo projects nothing. */
    @Test
    void aSantoInTheSecondarySlotProjectsNothing() {
        CombatantSheet santo = santoSheet(TitleSlot.SECONDARY);
        CombatantSheet ally = plainSheet();
        scene.addParticipant(santo, 20, party);
        scene.addParticipant(ally, 10, party);
        int before = defesas(ally);

        List<CombatantSheet> covered = scene.refreshProjectedAuras(santo,
                holderContextWith(santo, Map.of(ally, Range.ADJACENTE)));

        assertTrue(covered.isEmpty());
        assertEquals(before, defesas(ally));
    }

    @Test
    void anAllyBeyondAdjacenteReceivesNothing() {
        CombatantSheet santo = santoSheet(TitleSlot.PRIMARY);
        CombatantSheet ally = plainSheet();
        scene.addParticipant(santo, 20, party);
        scene.addParticipant(ally, 10, party);
        int before = defesas(ally);

        scene.refreshProjectedAuras(santo, holderContextWith(santo, Map.of(ally, Range.DISTANCIA_CURTA)));

        assertEquals(before, defesas(ally));
    }

    /** An enemy standing just as close is not an ally, and an Aura only reaches allies. */
    @Test
    void anAdjacentEnemyReceivesNothing() {
        CombatantSheet santo = santoSheet(TitleSlot.PRIMARY);
        CombatantSheet foe = plainSheet();
        scene.addParticipant(santo, 20, party);
        scene.addParticipant(foe, 10, UUID.randomUUID());
        int before = defesas(foe);

        scene.refreshProjectedAuras(santo, holderContextWith(santo, Map.of(foe, Range.ADJACENTE)));

        assertEquals(before, defesas(foe));
    }

    /** The revocation half: walking out of range takes the bonus away on the next refresh. */
    @Test
    void movingOutOfRangeRevokesTheBonus() {
        CombatantSheet santo = santoSheet(TitleSlot.PRIMARY);
        CombatantSheet ally = plainSheet();
        scene.addParticipant(santo, 20, party);
        scene.addParticipant(ally, 10, party);
        int before = defesas(ally);

        scene.refreshProjectedAuras(santo, holderContextWith(santo, Map.of(ally, Range.ADJACENTE)));
        assertEquals(before + 1, defesas(ally));

        scene.refreshProjectedAuras(santo, holderContextWith(santo, Map.of(ally, Range.DISTANCIA_CURTA)));
        assertEquals(before, defesas(ally));
    }

    /**
     * Revocation is by reference, so it takes back exactly what this Aura granted and leaves an
     * unrelated DEFESAS bonus from somewhere else alone.
     */
    @Test
    void revokingDoesNotDisturbAnUnrelatedDefesasBonus() {
        CombatantSheet santo = santoSheet(TitleSlot.PRIMARY);
        CombatantSheet ally = plainSheet();
        scene.addParticipant(santo, 20, party);
        scene.addParticipant(ally, 10, party);
        ally.grantTemporaryBonus(ModifierType.DEFESAS, 3, 5);
        int withUnrelatedBonus = defesas(ally);

        scene.refreshProjectedAuras(santo, holderContextWith(santo, Map.of(ally, Range.ADJACENTE)));
        scene.refreshProjectedAuras(santo, holderContextWith(santo, Map.of(ally, Range.DISTANCIA_MEDIA)));

        assertEquals(withUnrelatedBonus, defesas(ally));
    }

    /** Refreshing twice without anything moving must not stack a second copy. */
    @Test
    void refreshingTwiceGrantsOnlyOnce() {
        CombatantSheet santo = santoSheet(TitleSlot.PRIMARY);
        CombatantSheet ally = plainSheet();
        scene.addParticipant(santo, 20, party);
        scene.addParticipant(ally, 10, party);
        int before = defesas(ally);

        SceneContext adjacent = holderContextWith(santo, Map.of(ally, Range.ADJACENTE));
        scene.refreshProjectedAuras(santo, adjacent);
        scene.refreshProjectedAuras(santo, holderContextWith(santo, Map.of(ally, Range.ADJACENTE)));

        assertEquals(before + 1, defesas(ally));
    }

    /**
     * The bonus is open-ended: what ends it is leaving range, not time passing. A Rodada boundary
     * would silently expire a countdown-bearing one.
     */
    @Test
    void theBonusSurvivesARodadaBoundary() {
        CombatantSheet santo = santoSheet(TitleSlot.PRIMARY);
        CombatantSheet ally = plainSheet();
        scene.addParticipant(santo, 20, party);
        scene.addParticipant(ally, 10, party);
        scene.setCombatScene(true);
        int before = defesas(ally);

        scene.refreshProjectedAuras(santo, holderContextWith(santo, Map.of(ally, Range.ADJACENTE)));
        // Walk a full Rodada: every participant's Turn, then the wrap that starts the next one.
        scene.next();
        santo.finishTurn();
        scene.next();
        ally.finishTurn();
        int roundBeforeWrap = scene.getCurrentRound();
        scene.next();

        // Guard the guard: if the cursor never wrapped, nothing was ticked and this test would
        // pass without testing anything.
        assertTrue(scene.getCurrentRound() > roundBeforeWrap, "the Rodada boundary was not crossed");
        assertEquals(before + 1, defesas(ally));
    }

    /** The Scene-entry half — a combatant joining beside a Santo picks the Aura up as it arrives. */
    @Test
    void joiningTheSceneBesideASantoResolvesTheAuraOnEntry() {
        CombatantSheet santo = santoSheet(TitleSlot.PRIMARY);
        CombatantSheet ally = plainSheet();
        scene.addParticipant(ally, 10, party);
        int before = defesas(ally);

        // The Santo is the one arriving, and its own distances say who it lands next to.
        scene.addParticipant(santo, 20, party, Map.of(ally, Range.ADJACENTE));

        assertEquals(before + 1, defesas(ally));
    }

    /** A holder leaving the Scene takes its Aura with it — nothing else would clear an open-ended bonus. */
    @Test
    void theHolderLeavingTheSceneRevokesTheBonus() {
        CombatantSheet santo = santoSheet(TitleSlot.PRIMARY);
        CombatantSheet ally = plainSheet();
        scene.addParticipant(santo, 20, party);
        scene.addParticipant(ally, 10, party);
        int before = defesas(ally);
        scene.refreshProjectedAuras(santo, holderContextWith(santo, Map.of(ally, Range.ADJACENTE)));

        scene.removeParticipant(santo);

        assertEquals(before, defesas(ally));
    }

    /** And a recipient leaving doesn't walk off with it still on their sheet. */
    @Test
    void theRecipientLeavingTheSceneLosesTheBonus() {
        CombatantSheet santo = santoSheet(TitleSlot.PRIMARY);
        CombatantSheet ally = plainSheet();
        scene.addParticipant(santo, 20, party);
        scene.addParticipant(ally, 10, party);
        int before = defesas(ally);
        scene.refreshProjectedAuras(santo, holderContextWith(santo, Map.of(ally, Range.ADJACENTE)));

        scene.removeParticipant(ally);

        assertEquals(before, defesas(ally));
    }

    /** A decorated Santo projects more: base 2 + 1 adjacent + 2 (Especialização + Suprema) = 5, half 2. */
    @Test
    void especializacoesAndSupremasRaiseWhatTheAuraProjects() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        character.grantTitle(new Santo(List.of(SantoSpecialization.ABENCOADO_PELA_LUZ),
                List.of(SantoAbility.GUARDA_VIDAS)), TitleSlot.PRIMARY);
        CombatantSheet santo = CharacterSheet.of(character, new Player());
        CombatantSheet ally = plainSheet();
        scene.addParticipant(santo, 20, party);
        scene.addParticipant(ally, 10, party);
        int before = defesas(ally);

        scene.refreshProjectedAuras(santo, holderContextWith(santo, Map.of(ally, Range.ADJACENTE)));

        assertEquals(before + 2, defesas(ally));
    }

    /** "Cannot tell" grants nothing — and still revokes, so a stale bonus can't survive on a null context. */
    @Test
    void aNullContextGrantsNothingAndRevokesWhatWasThere() {
        CombatantSheet santo = santoSheet(TitleSlot.PRIMARY);
        CombatantSheet ally = plainSheet();
        scene.addParticipant(santo, 20, party);
        scene.addParticipant(ally, 10, party);
        int before = defesas(ally);
        scene.refreshProjectedAuras(santo, holderContextWith(santo, Map.of(ally, Range.ADJACENTE)));

        assertEquals(List.of(), scene.refreshProjectedAuras(santo, null));
        assertEquals(before, defesas(ally));
    }
}
