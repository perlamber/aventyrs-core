package org.aventyrs.core.title.santo;

import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.TargetScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Glória Relampejante de Tesla — 3PD reported as +2PA for self and nearby allies. */
class GloriaRelampejanteDeTeslaInteractionTest {

    private final GloriaRelampejanteDeTeslaInteraction interaction = new GloriaRelampejanteDeTeslaInteraction();
    private final ActionPointsService actionPointsService = new ActionPointsServiceImpl();
    private final DeterminationPointsService determinationPointsService = new DeterminationPointsServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet newSheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }

    private Blessing blessingOf(final InteractionResult result, final ModifierType modifierType) {
        return result.getBlessings().stream()
                .filter(blessing -> blessing.getModifierType() == modifierType)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no " + modifierType + " Blessing reported"));
    }

    /** V19 dropped the RA half, so exactly one Blessing is reported. */
    @Test
    void reportsThePaBlessingForSelfAndAlliesAndSpendsThreePd() {
        CharacterSheet actor = newSheet();
        int pdBefore = determinationPointsService.getCurrentDeterminationPoints(actor.getCharacter(), actor);

        InteractionResult result = interaction.applyTo(actor);

        assertEquals(3, result.getDeterminationPointsSpent());
        assertEquals(pdBefore - 3, determinationPointsService.getCurrentDeterminationPoints(actor.getCharacter(), actor));
        assertEquals(1, result.getBlessings().size());
        assertTrue(result.getBlessings().stream().allMatch(blessing ->
                blessing.getScope() == TargetScope.SELF_AND_ALLIES
                        && blessing.getRounds() == 1
                        && AbencoadoPelaLuzAbility.GLORIA_RELAMPEJANTE_DE_TESLA.name().equals(blessing.getSource())));
        assertEquals(2, blessingOf(result, ModifierType.ACTION_POINTS).getValue());
    }

    /** The activation reports; a caller grants. It then lands on a real reader. */
    @Test
    void grantingTheReportedBlessingRaisesPaForOneRodada() {
        CharacterSheet ally = newSheet();
        InteractionResult result = interaction.applyTo(newSheet());
        int paBefore = actionPointsService.getMaxActionPoints(ally, 1);

        result.getBlessings().forEach(ally::grantBlessing);

        assertEquals(paBefore + 2, actionPointsService.getMaxActionPoints(ally, 1));

        ally.finishTurn();

        assertEquals(paBefore, actionPointsService.getMaxActionPoints(ally, 1));
    }

    @Test
    void theAllyRangeIsDistanciaCurta() {
        assertEquals(Range.DISTANCIA_CURTA, GloriaRelampejanteDeTeslaInteraction.ALLY_RANGE);
    }
}
