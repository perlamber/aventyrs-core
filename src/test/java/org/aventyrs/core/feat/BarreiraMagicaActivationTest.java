package org.aventyrs.core.feat;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.ActiveAbilityService;
import org.aventyrs.core.character.services.ActiveAbilityServiceImpl;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Barreira Mágica end to end — {@code MetamagicoFeat#ARCANISTA_EXPERIENTE}'s activated Defesas
 * bonus, and with it the Resfriamento mechanism ({@code ActiveAbility#getCooldownRounds()} plus
 * the per-sheet ledger {@code CombatantSheet#startCooldown}/{@code getRemainingCooldown}) that
 * this Talento is the catalog's first stated consumer of.
 *
 * <p>Turn number 0 throughout: nothing here is conditioned on where in the order the activation
 * happens, and {@code ActiveAbilityService#activate} only uses it to resolve max Pontos de Ação.
 */
class BarreiraMagicaActivationTest {

    private final ActiveAbilityService activeAbilityService = new ActiveAbilityServiceImpl();
    private final DefenseService defenseService = new DefenseServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /** A Conjurador holding rung and every rung below it, with Pontos de Magia to spend. */
    private static Character arcanista(final MetamagicoFeat... rungs) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>()).build();
        for (MetamagicoFeat rung : rungs) {
            character.grantFeat(rung);
        }
        return character;
    }

    private static ActiveAbility barreiraOf(final Character character) {
        return character.getActiveAbilities().stream()
                .filter(BarreiraMagicaActiveAbility.class::isInstance)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no Barreira Mágica granted"));
    }

    @Test
    void onlyArcanistaExperienteGrantsTheBarreira() {
        assertEquals(0, arcanista(MetamagicoFeat.ARCANISTA).getActiveAbilities().size());
        assertEquals(1, arcanista(MetamagicoFeat.ARCANISTA_EXPERIENTE).getActiveAbilities().size());
        // The upgrades raise the figure rather than each granting a Barreira of their own —
        // three rungs held still means one ability.
        assertEquals(1, arcanista(MetamagicoFeat.ARCANISTA_EXPERIENTE,
                MetamagicoFeat.MESTRE_ARCANISTA, MetamagicoFeat.DESAFIADOR_DA_REALIDADE)
                .getActiveAbilities().size());
    }

    @Test
    void activatingItSpendsThreeManaAndRaisesBothDefesas() throws IllegalOperationException {
        Character character = arcanista(MetamagicoFeat.ARCANISTA_EXPERIENTE);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        int physicalBefore = defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL, null);
        int magicBefore = defenseService.getTotalDefense(sheet, DefenseType.MAGIC, null);

        activeAbilityService.activate(character, sheet, barreiraOf(character), 0);

        assertEquals(3, sheet.getManaSpent());
        assertEquals(physicalBefore + 2, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL, null));
        assertEquals(magicBefore + 2, defenseService.getTotalDefense(sheet, DefenseType.MAGIC, null),
                "\"suas Defesas\" names neither DF nor DM, so both");
    }

    /** Each rung <em>replaces</em> the figure: +2 → +3 → +5, never summing to 10. */
    @Test
    void eachRungReplacesTheDefesasFigureRatherThanAddingToIt() throws IllegalOperationException {
        assertEquals(2, barreiraBonusOf(MetamagicoFeat.ARCANISTA_EXPERIENTE));
        assertEquals(3, barreiraBonusOf(MetamagicoFeat.ARCANISTA_EXPERIENTE, MetamagicoFeat.MESTRE_ARCANISTA));
        assertEquals(5, barreiraBonusOf(MetamagicoFeat.ARCANISTA_EXPERIENTE,
                MetamagicoFeat.MESTRE_ARCANISTA, MetamagicoFeat.DESAFIADOR_DA_REALIDADE));
    }

    private int barreiraBonusOf(final MetamagicoFeat... rungs) throws IllegalOperationException {
        Character character = arcanista(rungs);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        int before = defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL, null);

        activeAbilityService.activate(character, sheet, barreiraOf(character), 0);

        return defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL, null) - before;
    }

    // ---------- Resfriamento ----------

    /**
     * "Resfriamento 1" — one Rodada must pass before it can be raised again. The ledger burns
     * down at the <b>Rodada</b> boundary, not the Turn one, which is why {@code finishTurn} alone
     * leaves it owing.
     */
    @Test
    void theBarreiraCannotBeRaisedAgainUntilItsResfriamentoElapses() throws IllegalOperationException {
        Character character = arcanista(MetamagicoFeat.ARCANISTA_EXPERIENTE);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        ActiveAbility barreira = barreiraOf(character);

        activeAbilityService.activate(character, sheet, barreira, 0);

        assertEquals(1, sheet.getRemainingCooldown(barreira));
        assertThrows(IllegalOperationException.class,
                () -> activeAbilityService.activate(character, sheet, barreira, 0));

        sheet.finishTurn();
        assertEquals(1, sheet.getRemainingCooldown(barreira), "a Turn is not a Rodada");

        sheet.startNewRound();

        assertEquals(0, sheet.getRemainingCooldown(barreira));
        assertDoesNotThrow(() -> activeAbilityService.activate(character, sheet, barreira, 0));
    }

    /** A refused activation must not lock the ability out — the ledger is written on success only. */
    @Test
    void anActivationRefusedForMissingManaStartsNoResfriamento() {
        Character character = arcanista(MetamagicoFeat.ARCANISTA_EXPERIENTE);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        ActiveAbility barreira = barreiraOf(character);
        sheet.spendMagicPoints(magicPointsAvailable(character, sheet));

        assertThrows(IllegalOperationException.class,
                () -> activeAbilityService.activate(character, sheet, barreira, 0));

        assertEquals(0, sheet.getRemainingCooldown(barreira));
    }

    /** An ability stating no Resfriamento may be activated as often as its costs allow. */
    @Test
    void anAbilityWithoutAResfriamentoIsNeverLockedOut() throws IllegalOperationException {
        Character character = arcanista(MetamagicoFeat.ARCANISTA_EXPERIENTE);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        sheet.startCooldown(barreiraOf(character), 0);

        assertEquals(0, sheet.getRemainingCooldown(barreiraOf(character)));
    }

    private static int magicPointsAvailable(final Character character, final CharacterSheet sheet) {
        return new org.aventyrs.core.character.services.MagicPointsServiceImpl()
                .getCurrentMagicPoints(character, sheet);
    }
}
