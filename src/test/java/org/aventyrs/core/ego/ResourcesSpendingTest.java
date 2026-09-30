package org.aventyrs.core.ego;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.EgoValue;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.CharacterCreationService;
import org.aventyrs.core.character.services.CharacterCreationServiceImpl;
import org.aventyrs.core.character.services.EgoPointsService;
import org.aventyrs.core.character.services.EgoPointsServiceImpl;
import org.aventyrs.core.defect.HeldQuality;
import org.aventyrs.core.defect.Quality;
import org.aventyrs.core.defect.QualityClass;
import org.aventyrs.core.defect.QualitySource;
import org.aventyrs.core.item.ItemRarity;
import org.aventyrs.core.item.ItemStore;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.EgoPointType;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Recursos half of the Ego plan (Phases 1–4): the {@link SocialClass} table, spending Recursos for PE,
 * the starting PE and store at creation. Numbers are the book's own, from {@code docs/rules/ego.txt}.
 */
class ResourcesSpendingTest {

    private final EgoPointsService egoPointsService = new EgoPointsServiceImpl();
    private final CharacterCreationService creation = new CharacterCreationServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character withResources(final int base, final int variable, final HeldQuality... qualities) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .egos(CharacterEgos.builder()
                        .recursos(EgoValue.builder().base(base).variable(variable).build())
                        .build())
                .qualities(new ArrayList<>(List.of(qualities)))
                .build();
    }

    private static CharacterSheet sheetOf(final Character character) {
        return CharacterSheet.of(character, new Player());
    }

    // ---------- the table ----------

    @Test
    void everyRowMatchesTheBook() {
        int[][] expected = {
                // resources, PE iniciais, temp, perm, utilidades, serviços/dia
                {0, 0, 3, 0, 1, 0},
                {1, 15, 7, 22, 3, 2},
                {2, 21, 10, 31, 6, 3},
                {3, 28, 14, 42, 10, 5},
                {4, 36, 18, 54, 15, 8},
                {5, 45, 22, 67, 21, 11},
        };
        for (int[] row : expected) {
            SocialClass socialClass = SocialClass.of(row[0]);
            assertEquals(row[0], socialClass.getResources());
            assertEquals(row[1], socialClass.getStartingEquipmentPoints());
            assertEquals(row[2], socialClass.getTemporaryPointValue());
            assertEquals(row[3], socialClass.getPermanentPointValue());
            assertEquals(row[4], socialClass.getUtilityCount());
            assertEquals(row[5], socialClass.getServicesPerDay());
        }
        assertEquals(Optional.empty(), SocialClass.FALENCIA.getStartingRarity());
        assertEquals(Optional.of(ItemRarity.RARE), SocialClass.ALTA_NOBREZA.getStartingRarity());
        assertEquals(ItemRarity.MYTHIC, SocialClass.ALTA_NOBREZA.getMaxUtilityRarity());
    }

    @Test
    void aValueOutsideZeroToFiveIsClamped() {
        assertEquals(SocialClass.ALTA_NOBREZA, SocialClass.of(7));
        assertEquals(SocialClass.FALENCIA, SocialClass.of(-1));
    }

    /** The row is the permanent Recursos left — a spent temporary point doesn't move it. */
    @Test
    void theCurrentRowIsThePermanentRecursosLeft() {
        CharacterSheet sheet = sheetOf(withResources(3, 2));
        sheet.spendEgoPoints(EgoDomain.RECURSOS, EgoPointType.TEMPORARY, 2);
        assertEquals(SocialClass.ALTA_NOBREZA, SocialClass.current(sheet));

        sheet.spendEgoPoints(EgoDomain.RECURSOS, EgoPointType.PERMANENT, 1);
        assertEquals(SocialClass.NOBREZA, SocialClass.current(sheet));
    }

    @Test
    void recursosZeroIsExtremePoverty() {
        CharacterSheet sheet = sheetOf(withResources(0, 0));

        assertTrue(SocialClass.current(sheet).isExtremePoverty());
        assertFalse(SocialClass.POBREZA.isExtremePoverty());
    }

    // ---------- spending Recursos for PE ----------

    /**
     * The book's example: Recursos 5 (3 disponíveis) spends a temporary → 22PE, another → 22PE, a permanent
     * → 67PE, and "fica com Recursos total 4 (1 disponível)", where a temporary is now 18PE.
     */
    @Test
    void theBooksExample() {
        CharacterSheet sheet = sheetOf(withResources(3, 2));
        sheet.spendEgoPoints(EgoDomain.RECURSOS, EgoPointType.TEMPORARY, 2); // "3 disponíveis"

        assertEquals(22, egoPointsService.spendResourcesForEquipmentPoints(sheet, EgoPointType.TEMPORARY, 1));
        assertEquals(22, egoPointsService.spendResourcesForEquipmentPoints(sheet, EgoPointType.TEMPORARY, 1));
        assertEquals(67, egoPointsService.spendResourcesForEquipmentPoints(sheet, EgoPointType.PERMANENT, 1));
        assertEquals(4, sheet.getPermanentEgoPoints(EgoDomain.RECURSOS));
        assertEquals(1, sheet.getTemporaryEgoPoints(EgoDomain.RECURSOS));      // "1 disponível"
        assertEquals(18, egoPointsService.spendResourcesForEquipmentPoints(sheet, EgoPointType.TEMPORARY, 1));

        assertEquals(22 + 22 + 67 + 18, sheet.getEquipmentPoints());
    }

    /** Two permanent points at once are priced at 5 then 4 — each lowers the row for the next. */
    @Test
    void severalPermanentPointsArePricedOneRowLowerEach() {
        CharacterSheet sheet = sheetOf(withResources(3, 2));

        assertEquals(67 + 54, egoPointsService.spendResourcesForEquipmentPoints(sheet, EgoPointType.PERMANENT, 2));
        assertEquals(3, sheet.getPermanentEgoPoints(EgoDomain.RECURSOS));
    }

    /** Only what actually left the pool is paid for. */
    @Test
    void aShortSpendPaysOnlyForThePointsSpent() {
        CharacterSheet sheet = sheetOf(withResources(2, 0));

        assertEquals(2 * 10, egoPointsService.spendResourcesForEquipmentPoints(sheet, EgoPointType.TEMPORARY, 5));
        assertEquals(0, egoPointsService.spendResourcesForEquipmentPoints(sheet, EgoPointType.TEMPORARY, 1));
        assertEquals(20, sheet.getEquipmentPoints());
    }

    /** Recursos 7 = Recursos 5 + 2 extras; an extra is worth row 5, like the rest. */
    @Test
    void anExtraPastFiveIsWorthRowFive() {
        CharacterSheet sheet = sheetOf(withResources(3, 4));

        assertEquals(22, egoPointsService.spendResourcesForEquipmentPoints(sheet, EgoPointType.TEMPORARY, 1));
        assertEquals(1, sheet.getExtraTemporaryEgoPoints(EgoDomain.RECURSOS));
    }

    /** Destinado a Fortuna Menor — "Seus pontos temporários e permanentes de Recursos valem +2PE cada". */
    @Test
    void saberInvestirAddsTwoPePerPoint() {
        HeldQuality saberInvestir = new HeldQuality(Quality.DESTINADO_A_FORTUNA, QualityClass.MENOR, List.of(),
                QualitySource.SUPERACAO);
        CharacterSheet sheet = sheetOf(withResources(2, 0, saberInvestir));

        assertEquals(10 + 2, egoPointsService.spendResourcesForEquipmentPoints(sheet, EgoPointType.TEMPORARY, 1));
        assertEquals(31 + 2, egoPointsService.spendResourcesForEquipmentPoints(sheet, EgoPointType.PERMANENT, 1));
    }

    // ---------- creation ----------

    @Test
    void startingPeFollowTheRecursosTotal() {
        CharacterSheet sheet = sheetOf(withResources(3, 0));

        assertEquals(28, creation.grantStartingEquipmentPoints(sheet));
        assertEquals(28, sheet.getEquipmentPoints());
    }

    /** A total past 5 is still Alta Nobreza — the rest are extras, not PE. */
    @Test
    void startingPeCountRecursosAsFiveAtMost() {
        assertEquals(45, creation.grantStartingEquipmentPoints(sheetOf(withResources(3, 4))));
    }

    @Test
    void theStartingStoreSellsUpToTheRaridadeInicial() {
        Optional<ItemStore> store = creation.getStartingStore(withResources(3, 0));

        assertTrue(store.isPresent());
        assertEquals(ItemRarity.UNCOMMON, store.get().getMaxRarity());
    }

    /** "Nenhum" — Falência buys nothing at creation. */
    @Test
    void falenciaHasNoStartingStore() {
        assertEquals(Optional.empty(), creation.getStartingStore(withResources(0, 0)));
    }
}
