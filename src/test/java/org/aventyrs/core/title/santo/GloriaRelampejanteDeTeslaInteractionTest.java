package org.aventyrs.core.title.santo;

import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
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

/** Glória Relampejante de Tesla — 2PD reported as +1PA and RA for self and nearby allies. */
class GloriaRelampejanteDeTeslaInteractionTest {

    private final GloriaRelampejanteDeTeslaInteraction interaction = new GloriaRelampejanteDeTeslaInteraction();
    private final ActionPointsService actionPointsService = new ActionPointsServiceImpl();
    private final DamageService damageService = new DamageServiceImpl();
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

    @Test
    void reportsBothHalvesForSelfAndAlliesAndSpendsTwoPd() {
        CharacterSheet actor = newSheet();
        int pdBefore = determinationPointsService.getCurrentDeterminationPoints(actor.getCharacter(), actor);

        InteractionResult result = interaction.applyTo(actor);

        assertEquals(2, result.getDeterminationPointsSpent());
        assertEquals(pdBefore - 2, determinationPointsService.getCurrentDeterminationPoints(actor.getCharacter(), actor));
        assertEquals(2, result.getBlessings().size());
        assertTrue(result.getBlessings().stream().allMatch(blessing ->
                blessing.getScope() == TargetScope.SELF_AND_ALLIES
                        && blessing.getRounds() == 1
                        && AbencoadoPelaLuzAbility.GLORIA_RELAMPEJANTE_DE_TESLA.name().equals(blessing.getSource())));
        assertEquals(1, blessingOf(result, ModifierType.ACTION_POINTS).getValue());
        assertEquals(DamageService.DEFAULT_DAMAGE_REDUCTION,
                blessingOf(result, ModifierType.ABSOLUTE_DAMAGE_REDUCTION).getValue());
    }

    /** The activation reports; a caller grants. Both halves then land on a real reader. */
    @Test
    void grantingTheReportedBlessingsRaisesPaAndRaForOneRodada() {
        CharacterSheet ally = newSheet();
        InteractionResult result = interaction.applyTo(newSheet());
        int paBefore = actionPointsService.getMaxActionPoints(ally, 1);
        int damageBefore = damageService.calculateFinalDamage(ally, null, (DamageType) null, null, 10, false);

        result.getBlessings().forEach(ally::grantBlessing);

        assertEquals(paBefore + 1, actionPointsService.getMaxActionPoints(ally, 1));
        assertEquals(DamageService.DEFAULT_DAMAGE_REDUCTION, damageService.getTotalAbsoluteDamageReduction(ally, null));
        assertEquals(damageBefore - DamageService.DEFAULT_DAMAGE_REDUCTION,
                damageService.calculateFinalDamage(ally, null, (DamageType) null, null, 10, false));

        ally.finishTurn();

        assertEquals(paBefore, actionPointsService.getMaxActionPoints(ally, 1));
        assertEquals(0, damageService.getTotalAbsoluteDamageReduction(ally, null));
    }

    @Test
    void theAllyRangeIsDistanciaCurta() {
        assertEquals(Range.DISTANCIA_CURTA, GloriaRelampejanteDeTeslaInteraction.ALLY_RANGE);
    }
}
