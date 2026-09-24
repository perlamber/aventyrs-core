package org.aventyrs.core.character.services;

import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.MobilidadeFeat;
import org.aventyrs.core.item.BootsItem;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.EnvironmentalState;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.aventyrs.core.util.TranslatableMessages.REPOSITION_AFTER_MOVEMENT;
import static org.aventyrs.core.util.TranslatableMessages.REPOSITION_IN_DIFFICULT_TERRAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reposicionar: 1UD for an Ação Livre, no Reações, never in Terreno Difícil (table ruling). */
class RepositionServiceTest {

    private final RepositionService service = new RepositionServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet sheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID()).feats(new ArrayList<>()).build(), new Player());
    }

    private static SceneContext standing(final boolean inDifficultTerrain) {
        return new SceneContext(List.of(), List.of(), Map.of(), null, true, 0, false, null, null,
                EnvironmentalState.ORDINARY.withPosition(inDifficultTerrain, Set.of()));
    }

    @Test
    void itIsOneUdBoughtWithAnAcaoLivre() {
        assertEquals(1, RepositionService.DISTANCE_UD);
        assertEquals(ActionCost.FREE_ACTION, RepositionService.ACTION_COST);
        assertEquals(1, service.begin(sheet(), 1, standing(false)));
    }

    @Test
    void itIsRefusedFromTerrenoDificil() {
        CharacterSheet sheet = sheet();

        assertFalse(service.canReposition(sheet, 1, standing(true)));
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> service.begin(sheet, 1, standing(true)));
        assertEquals(REPOSITION_IN_DIFFICULT_TERRAIN, refused.getMessage());
    }

    @Test
    void itClaimsNoMovementOfTheRodada() {
        CharacterSheet sheet = sheet();

        service.begin(sheet, 1, standing(false));

        assertEquals(0, sheet.getMovementsTakenThisRound());
    }

    /** Spending Pontos de Ação moving and Reposicionar exclude each other within a Turn (table ruling). */
    @Test
    void itIsRefusedAfterSpendingPontosDeAcaoMoving() {
        CharacterSheet sheet = sheet();
        sheet.consumeMovementThisRound();

        assertFalse(service.canReposition(sheet, 1, standing(false)));
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> service.begin(sheet, 1, standing(false)));
        assertEquals(REPOSITION_AFTER_MOVEMENT, refused.getMessage());
        assertEquals(0, sheet.getRepositionsTakenThisRound(), "a refused Reposicionar claims nothing");
    }

    @Test
    void aNewTurnLiftsTheMovementsBarOnRepositioning() {
        CharacterSheet sheet = sheet();
        sheet.consumeMovementThisRound();

        sheet.startTurn(2);

        assertTrue(service.canReposition(sheet, 2, standing(false)));
    }

    private static CharacterSheet withMovimentoRapido() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).id(UUID.randomUUID())
                .feats(new ArrayList<Feat>(List.of(MobilidadeFeat.MOVIMENTO_RAPIDO))).build(), new Player());
    }

    private static CharacterSheet wearingSandalhasDoCorredor(final int dexterity) {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).id(UUID.randomUUID())
                .feats(new ArrayList<>())
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(dexterity).build())
                        .build())
                .equipment(List.of(BootsItem.SANDALHAS_DO_CORREDOR))
                .build(), new Player());
    }

    /** Core Rodada 1 is the table's second — a Rodada Par. */
    @Test
    void movimentoRapidoWidensTheFirstReposicionarOfARodadaPar() {
        CharacterSheet sheet = withMovimentoRapido();

        assertEquals(2, service.getDistance(sheet, 1));
        assertEquals(2, service.begin(sheet, 1, standing(false)));
        assertEquals(1, service.begin(sheet, 1, standing(false)));
    }

    @Test
    void movimentoRapidoAddsNothingInARodadaImpar() {
        assertEquals(1, service.begin(withMovimentoRapido(), 0, standing(false)));
        assertEquals(1, service.begin(withMovimentoRapido(), 2, standing(false)));
    }

    @Test
    void theFirstReposicionarIsCountedAgainPerTurn() {
        CharacterSheet sheet = withMovimentoRapido();
        service.begin(sheet, 1, standing(false));

        sheet.startTurn(3);

        assertEquals(0, sheet.getRepositionsTakenThisRound());
        assertEquals(2, service.getDistance(sheet, 3));
    }

    @Test
    void getDistanceClaimsNothing() {
        CharacterSheet sheet = withMovimentoRapido();

        service.getDistance(sheet, 1);

        assertEquals(0, sheet.getRepositionsTakenThisRound());
    }

    @Test
    void sandalhasDoCorredorWidenEveryReposicionar() {
        CharacterSheet sheet = wearingSandalhasDoCorredor(3);

        assertEquals(2, service.begin(sheet, 0, standing(false)));
        assertEquals(2, service.begin(sheet, 0, standing(false)));
    }

    @Test
    void sandalhasDoCorredorNeedTheirRequisito() {
        assertEquals(1, service.getDistance(wearingSandalhasDoCorredor(1), 0));
    }

    @Test
    void temporaryBonusesAndEverySourceAddUp() {
        CharacterSheet sheet = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).id(UUID.randomUUID())
                .feats(new ArrayList<Feat>(List.of(MobilidadeFeat.MOVIMENTO_RAPIDO)))
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(3).build())
                        .build())
                .equipment(List.of(BootsItem.SANDALHAS_DO_CORREDOR))
                .build(), new Player());

        sheet.grantTemporaryBonus(ModifierType.REPOSITION_DISTANCE, 1, 1);

        // 1 + Movimento Rápido (Rodada Par, first) + Sandálhas do Corredor + the TemporaryBonus
        assertEquals(4, service.getDistance(sheet, 1));
    }

    @Test
    void itProvokesNoMovementReacoes() {
        assertTrue(new MovementReactionServiceImpl().getProvokedReactors(sheet(), standing(false),
                Manoeuvre.REPOSICIONAR).isEmpty());
    }
}
