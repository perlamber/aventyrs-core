package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.ego.AutocontroleAdvantage;
import org.aventyrs.core.ego.EgoAdvantage;
import org.aventyrs.core.ego.SorteAdvantage;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.EgoPointSpend;
import org.aventyrs.core.sheet.EgoPointType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.TargetScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EgoPointsServiceTest {

    private final EgoPointsService egoPointsService = new EgoPointsServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet sheetHolding(final EgoDomain domain, final EgoAdvantage advantage) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK);
        if (advantage != null) {
            builder.egoAdvantage(domain, advantage);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    /** Empties every domain's temporary pool, so a recovery has something to restore. */
    private void spendEveryTemporaryPoint(final CombatantSheet sheet) {
        for (EgoDomain domain : EgoDomain.values()) {
            sheet.spendEgoPoints(domain, EgoPointType.TEMPORARY, sheet.getTemporaryEgoPoints(domain));
        }
    }

    @Test
    void aCharacterWithNoVantagemRecoversExactlyOnePointInTheChosenDomain() {
        CharacterSheet sheet = sheetHolding(null, null);
        spendEveryTemporaryPoint(sheet);

        egoPointsService.applySessionRecovery(sheet, EgoDomain.SORTE);

        assertEquals(1, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    /** One point per session in total, not one per Ego — every other domain stays empty. */
    @Test
    void theBaselineRecoveryIsOnePointInTotalNotOnePerDomain() {
        CharacterSheet sheet = sheetHolding(null, null);
        spendEveryTemporaryPoint(sheet);

        egoPointsService.applySessionRecovery(sheet, EgoDomain.SORTE);

        assertEquals(0, sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
        assertEquals(0, sheet.getTemporaryEgoPoints(EgoDomain.RECURSOS));
        assertEquals(0, sheet.getTemporaryEgoPoints(EgoDomain.INICIATIVA));
    }

    @Test
    void motivacaoDeMosesRecoversAnExtraAutocontrolePointOnTop() {
        CharacterSheet sheet = sheetHolding(EgoDomain.AUTOCONTROLE, AutocontroleAdvantage.MOTIVACAO_DE_MOSES);
        spendEveryTemporaryPoint(sheet);

        egoPointsService.applySessionRecovery(sheet, EgoDomain.AUTOCONTROLE);

        assertEquals(2, sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
    }

    /** The extra lands in the Vantagem's own domain, whichever domain the player chose. */
    @Test
    void theExtraLandsInTheVantagemsOwnDomainRegardlessOfTheChosenOne() {
        CharacterSheet sheet = sheetHolding(EgoDomain.SORTE, SorteAdvantage.DILETO_DE_TYKHE);
        spendEveryTemporaryPoint(sheet);

        egoPointsService.applySessionRecovery(sheet, EgoDomain.RECURSOS);

        assertEquals(1, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
        assertEquals(1, sheet.getTemporaryEgoPoints(EgoDomain.RECURSOS));
    }

    @Test
    void aVantagemGrantingNoExtraRecoveryAddsNothing() {
        CharacterSheet sheet = sheetHolding(EgoDomain.SORTE, SorteAdvantage.ACE);
        spendEveryTemporaryPoint(sheet);

        egoPointsService.applySessionRecovery(sheet, EgoDomain.SORTE);

        assertEquals(1, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    @Test
    void recoveryNeverPushesAPoolPastItsOwnCeiling() {
        CharacterSheet sheet = sheetHolding(EgoDomain.AUTOCONTROLE, AutocontroleAdvantage.MOTIVACAO_DE_MOSES);
        int ceiling = sheet.getMaxTemporaryEgoPoints(EgoDomain.AUTOCONTROLE);

        egoPointsService.applySessionRecovery(sheet, EgoDomain.AUTOCONTROLE);

        assertEquals(ceiling, sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
    }

    @Test
    void getExtraSessionRecoveryIsZeroForADomainWithNoVantagem() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();

        for (EgoDomain domain : EgoDomain.values()) {
            assertEquals(0, egoPointsService.getExtraSessionRecovery(character, domain));
        }
    }

    // --- DETERMINACAO_HEROICA -------------------------------------------------------------

    private static final int A_ROLLED_FOUR = 4;

    /** Spends PV/PM/PD first, so a recovery has room to land. */
    private void drainAllThreeResources(final CharacterSheet sheet, final int amount) {
        sheet.applyDamage(amount);
        sheet.spendMagicPoints(amount);
        sheet.spendDeterminationPoints(amount);
    }

    @Test
    void usingATemporaryAutocontrolePointRecoversTheRolledValueInAllThreeResources() {
        CharacterSheet sheet = sheetHolding(EgoDomain.AUTOCONTROLE, AutocontroleAdvantage.DETERMINACAO_HEROICA);
        drainAllThreeResources(sheet, 10);

        egoPointsService.useEgoPointsForEffect(sheet, EgoDomain.AUTOCONTROLE, EgoPointType.TEMPORARY, 1, A_ROLLED_FOUR);

        assertEquals(6, sheet.getDamageTaken());
        assertEquals(6, sheet.getManaSpent());
        assertEquals(6, sheet.getDeterminationSpent());
    }

    /** "se o ponto for permanente, o valor recuperado é dobrado" — 4 becomes 8. */
    @Test
    void usingAPermanentAutocontrolePointDoublesTheRecoveredValue() {
        CharacterSheet sheet = sheetHolding(EgoDomain.AUTOCONTROLE, AutocontroleAdvantage.DETERMINACAO_HEROICA);
        drainAllThreeResources(sheet, 10);

        egoPointsService.useEgoPointsForEffect(sheet, EgoDomain.AUTOCONTROLE, EgoPointType.PERMANENT, 1, A_ROLLED_FOUR);

        assertEquals(2, sheet.getDamageTaken());
        assertEquals(2, sheet.getManaSpent());
        assertEquals(2, sheet.getDeterminationSpent());
    }

    @Test
    void theSpendItselfStillHappensAndIsReported() {
        CharacterSheet sheet = sheetHolding(EgoDomain.AUTOCONTROLE, AutocontroleAdvantage.DETERMINACAO_HEROICA);
        int before = sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE);

        EgoPointSpend spend = egoPointsService.useEgoPointsForEffect(
                sheet, EgoDomain.AUTOCONTROLE, EgoPointType.TEMPORARY, 1, A_ROLLED_FOUR);

        assertEquals(EgoDomain.AUTOCONTROLE, spend.getDomain());
        assertEquals(EgoPointType.TEMPORARY, spend.getType());
        assertEquals(1, spend.getValue());
        assertEquals(before - 1, sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
    }

    /** A spend that took nothing was no use of points at all, so it earns no recovery. */
    @Test
    void aSpendAgainstAnEmptyPoolRecoversNothing() {
        CharacterSheet sheet = sheetHolding(EgoDomain.AUTOCONTROLE, AutocontroleAdvantage.DETERMINACAO_HEROICA);
        sheet.spendEgoPoints(EgoDomain.AUTOCONTROLE, EgoPointType.TEMPORARY,
                sheet.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
        drainAllThreeResources(sheet, 10);

        EgoPointSpend spend = egoPointsService.useEgoPointsForEffect(
                sheet, EgoDomain.AUTOCONTROLE, EgoPointType.TEMPORARY, 1, A_ROLLED_FOUR);

        assertEquals(0, spend.getValue());
        assertEquals(10, sheet.getDamageTaken());
    }

    /** DETERMINACAO_HEROICA is an Autocontrole Vantagem: a Sorte spend must not trigger it. */
    @Test
    void usingPointsFromAnotherDomainDoesNotTriggerAnAutocontroleVantagem() {
        CharacterSheet sheet = sheetHolding(EgoDomain.AUTOCONTROLE, AutocontroleAdvantage.DETERMINACAO_HEROICA);
        drainAllThreeResources(sheet, 10);

        egoPointsService.useEgoPointsForEffect(sheet, EgoDomain.SORTE, EgoPointType.TEMPORARY, 1, A_ROLLED_FOUR);

        assertEquals(10, sheet.getDamageTaken());
    }

    @Test
    void aCharacterWithoutTheVantagemRecoversNothingFromUsingPoints() {
        CharacterSheet sheet = sheetHolding(null, null);
        drainAllThreeResources(sheet, 10);

        egoPointsService.useEgoPointsForEffect(sheet, EgoDomain.AUTOCONTROLE, EgoPointType.PERMANENT, 1, A_ROLLED_FOUR);

        assertEquals(10, sheet.getDamageTaken());
    }

    @Test
    void getSpendRecoveryComputesWithoutRecoveringAnything() {
        CharacterSheet sheet = sheetHolding(EgoDomain.AUTOCONTROLE, AutocontroleAdvantage.DETERMINACAO_HEROICA);
        drainAllThreeResources(sheet, 10);
        EgoPointSpend spend = sheet.spendEgoPoints(EgoDomain.AUTOCONTROLE, EgoPointType.PERMANENT, 1);

        assertEquals(8, egoPointsService.getSpendRecovery(sheet.getCharacter(), spend, A_ROLLED_FOUR));
        assertEquals(10, sheet.getDamageTaken());
    }

    /** A negative would otherwise reach heal() and silently damage the character. */
    @Test
    void anIllegalDieFaceIsRejected() {
        CharacterSheet sheet = sheetHolding(EgoDomain.AUTOCONTROLE, AutocontroleAdvantage.DETERMINACAO_HEROICA);
        EgoPointSpend spend = sheet.spendEgoPoints(EgoDomain.AUTOCONTROLE, EgoPointType.TEMPORARY, 1);

        assertThrows(IllegalOperationException.class,
                () -> egoPointsService.getSpendRecovery(sheet.getCharacter(), spend, -1));
        assertThrows(IllegalOperationException.class,
                () -> egoPointsService.getSpendRecovery(sheet.getCharacter(), spend, 0));
        assertThrows(IllegalOperationException.class,
                () -> egoPointsService.getSpendRecovery(sheet.getCharacter(), spend, 7));
    }

    @Test
    void noOtherAutocontroleAdvantageReactsToASpend() {
        CharacterSheet sheet = sheetHolding(EgoDomain.AUTOCONTROLE, AutocontroleAdvantage.RESOLUTO);
        EgoPointSpend spend = sheet.spendEgoPoints(EgoDomain.AUTOCONTROLE, EgoPointType.PERMANENT, 1);

        assertEquals(0, egoPointsService.getSpendRecovery(sheet.getCharacter(), spend, A_ROLLED_FOUR));
    }

    // --- The GM end-of-session button: bulk recovery ---------------------------------------

    @Test
    void bulkRecoveryGivesEachSheetItsOwnChosenDomain() {
        CharacterSheet ana = sheetHolding(null, null);
        CharacterSheet bruno = sheetHolding(null, null);
        spendEveryTemporaryPoint(ana);
        spendEveryTemporaryPoint(bruno);

        egoPointsService.applySessionRecovery(Map.of(
                ana, EgoDomain.SORTE,
                bruno, EgoDomain.AUTOCONTROLE));

        assertEquals(1, ana.getTemporaryEgoPoints(EgoDomain.SORTE));
        assertEquals(0, ana.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
        assertEquals(1, bruno.getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE));
        assertEquals(0, bruno.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    /** The map is the whole selection — this is how a consumer excludes a foe or an absentee. */
    @Test
    void aSheetAbsentFromTheMapIsUntouched() {
        CharacterSheet present = sheetHolding(null, null);
        CharacterSheet absent = sheetHolding(null, null);
        spendEveryTemporaryPoint(present);
        spendEveryTemporaryPoint(absent);

        egoPointsService.applySessionRecovery(Map.of(present, EgoDomain.SORTE));

        assertEquals(1, present.getTemporaryEgoPoints(EgoDomain.SORTE));
        assertEquals(0, absent.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    @Test
    void anEmptyMapIsANoOp() {
        CharacterSheet sheet = sheetHolding(null, null);
        spendEveryTemporaryPoint(sheet);

        egoPointsService.applySessionRecovery(Map.of());

        assertEquals(0, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    /** A Vantagem's extra ignores the chosen domain and lands in its own, through the bulk path. */
    @Test
    void bulkRecoveryStillRoutesAVantagemsExtraToItsOwnDomain() {
        CharacterSheet sheet = sheetHolding(EgoDomain.SORTE, SorteAdvantage.DILETO_DE_TYKHE);
        spendEveryTemporaryPoint(sheet);

        egoPointsService.applySessionRecovery(Map.of(sheet, EgoDomain.RECURSOS));

        assertEquals(1, sheet.getTemporaryEgoPoints(EgoDomain.RECURSOS));
        assertEquals(1, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    /**
     * Pinned deliberately: recovery is <em>not</em> idempotent, and preventing a double-click is
     * the consuming app's job. Nobody should quietly "fix" this into a quiet guard — this core
     * has no session boundary to hang one on.
     */
    @Test
    void applyingTheSameSessionRecoveryTwiceRecoversTwice() {
        CharacterSheet sheet = sheetHolding(null, null);
        spendEveryTemporaryPoint(sheet);
        Map<CombatantSheet, EgoDomain> choices = Map.of(sheet, EgoDomain.SORTE);

        egoPointsService.applySessionRecovery(choices);
        egoPointsService.applySessionRecovery(choices);

        assertEquals(2, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    /**
     * The whole GM click, end to end: a Scene holding two players and a foe, the roster read off
     * {@code Scene#getAllParticipants()}, and a map carrying only the players.
     */
    @Test
    void theGmEndOfSessionButtonRecoversOnlyTheCharactersInTheMap() {
        CharacterSheet ana = sheetHolding(null, null);
        CharacterSheet bruno = sheetHolding(null, null);
        MonsterSheet foe = GenericMonster.CAPANGA.spawn(new Player());
        spendEveryTemporaryPoint(ana);
        spendEveryTemporaryPoint(bruno);
        spendEveryTemporaryPoint(foe);

        Scene scene = new Scene();
        UUID party = UUID.randomUUID();
        scene.addParticipant(ana, 12, party);
        scene.addParticipant(bruno, 9, party);
        scene.addParticipant(foe, 7, UUID.randomUUID());

        assertEquals(3, scene.getAllParticipants().size());

        egoPointsService.applySessionRecovery(Map.of(
                ana, EgoDomain.SORTE,
                bruno, EgoDomain.RECURSOS));

        assertEquals(1, ana.getTemporaryEgoPoints(EgoDomain.SORTE));
        assertEquals(1, bruno.getTemporaryEgoPoints(EgoDomain.RECURSOS));
        for (EgoDomain domain : EgoDomain.values()) {
            assertEquals(0, foe.getTemporaryEgoPoints(domain));
        }
    }

    // --- AS_NA_MANGA -----------------------------------------------------------------------

    @Test
    void usingAPontoDeSorteGrantsTwoUdOfMovement() {
        CharacterSheet sheet = sheetHolding(EgoDomain.SORTE, SorteAdvantage.AS_NA_MANGA);

        egoPointsService.useEgoPointsForEffect(sheet, EgoDomain.SORTE, EgoPointType.TEMPORARY, 1, A_ROLLED_FOUR);

        assertEquals(2, sheet.getTemporaryBonus(ModifierType.MOVEMENT));
    }

    /** "Utilizar um Ponto de Sorte" doesn't distinguish the pools — a permanent point counts too. */
    @Test
    void aPermanentPontoDeSorteGrantsTheSameMovement() {
        CharacterSheet sheet = sheetHolding(EgoDomain.SORTE, SorteAdvantage.AS_NA_MANGA);

        egoPointsService.useEgoPointsForEffect(sheet, EgoDomain.SORTE, EgoPointType.PERMANENT, 1, A_ROLLED_FOUR);

        assertEquals(2, sheet.getTemporaryBonus(ModifierType.MOVEMENT));
    }

    /** AS_NA_MANGA is a Sorte Vantagem: an Autocontrole spend must not trigger it. */
    @Test
    void usingPointsFromAnotherDomainGrantsNoMovement() {
        CharacterSheet sheet = sheetHolding(EgoDomain.SORTE, SorteAdvantage.AS_NA_MANGA);

        egoPointsService.useEgoPointsForEffect(sheet, EgoDomain.AUTOCONTROLE, EgoPointType.TEMPORARY, 1, A_ROLLED_FOUR);

        assertEquals(0, sheet.getTemporaryBonus(ModifierType.MOVEMENT));
    }

    @Test
    void aSpendThatTookNoPointsGrantsNoMovement() {
        CharacterSheet sheet = sheetHolding(EgoDomain.SORTE, SorteAdvantage.AS_NA_MANGA);
        sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.TEMPORARY,
                sheet.getTemporaryEgoPoints(EgoDomain.SORTE));

        egoPointsService.useEgoPointsForEffect(sheet, EgoDomain.SORTE, EgoPointType.TEMPORARY, 1, A_ROLLED_FOUR);

        assertEquals(0, sheet.getTemporaryBonus(ModifierType.MOVEMENT));
    }

    /** The grant is Rodada-scoped, so it lapses at the end of the spender's Turn. */
    @Test
    void theMovementGrantExpiresAtTheEndOfTheTurn() {
        CharacterSheet sheet = sheetHolding(EgoDomain.SORTE, SorteAdvantage.AS_NA_MANGA);
        egoPointsService.useEgoPointsForEffect(sheet, EgoDomain.SORTE, EgoPointType.TEMPORARY, 1, A_ROLLED_FOUR);

        sheet.finishTurn();

        assertEquals(0, sheet.getTemporaryBonus(ModifierType.MOVEMENT));
    }

    @Test
    void aCharacterWithoutAsNaMangaGetsNoMovementFromASorteSpend() {
        CharacterSheet sheet = sheetHolding(EgoDomain.SORTE, SorteAdvantage.ACE);

        egoPointsService.useEgoPointsForEffect(sheet, EgoDomain.SORTE, EgoPointType.TEMPORARY, 1, A_ROLLED_FOUR);

        assertEquals(0, sheet.getTemporaryBonus(ModifierType.MOVEMENT));
    }

    @Test
    void getSpendBlessingsComputesWithoutGrantingAnything() {
        CharacterSheet sheet = sheetHolding(EgoDomain.SORTE, SorteAdvantage.AS_NA_MANGA);
        EgoPointSpend spend = sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.TEMPORARY, 1);

        List<Blessing> blessings = egoPointsService.getSpendBlessings(sheet.getCharacter(), spend);

        assertEquals(1, blessings.size());
        assertEquals(ModifierType.MOVEMENT, blessings.get(0).getModifierType());
        assertEquals(2, blessings.get(0).getValue());
        assertEquals(TargetScope.SELF, blessings.get(0).getScope());
        assertEquals(SorteAdvantage.AS_NA_MANGA.name(), blessings.get(0).getSource());
        assertEquals(0, sheet.getTemporaryBonus(ModifierType.MOVEMENT));
    }

    /**
     * The grant is genuinely consumed, not inert: {@code MovementService#getMovementBase}
     * reports the permanent Movimento Base, and a caller adds the sheet's own {@code MOVEMENT}
     * temporary bonus on top — which is where AS_NA_MANGA's 2UD lands.
     */
    @Test
    void theGrantedMovementReachesTheCharactersActualMovementTotal() {
        CharacterSheet sheet = sheetHolding(EgoDomain.SORTE, SorteAdvantage.AS_NA_MANGA);
        MovementService movementService = new MovementServiceImpl();
        int before = movementService.getMovementBase(sheet.getCharacter())
                + sheet.getTemporaryBonus(ModifierType.MOVEMENT);

        egoPointsService.useEgoPointsForEffect(sheet, EgoDomain.SORTE, EgoPointType.TEMPORARY, 1, A_ROLLED_FOUR);

        int after = movementService.getMovementBase(sheet.getCharacter())
                + sheet.getTemporaryBonus(ModifierType.MOVEMENT);
        assertEquals(before + 2, after);
    }

    @Test
    void noOtherSorteAdvantageGrantsASpendBlessing() {
        for (SorteAdvantage advantage : SorteAdvantage.values()) {
            if (advantage != SorteAdvantage.AS_NA_MANGA) {
                CharacterSheet sheet = sheetHolding(EgoDomain.SORTE, advantage);
                EgoPointSpend spend = sheet.spendEgoPoints(EgoDomain.SORTE, EgoPointType.TEMPORARY, 1);
                assertEquals(List.of(), egoPointsService.getSpendBlessings(sheet.getCharacter(), spend));
            }
        }
    }
}
