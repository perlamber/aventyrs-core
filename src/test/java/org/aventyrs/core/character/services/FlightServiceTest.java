package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.Dice;
import org.aventyrs.core.character.FlightActivation;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.AvianoFeat;
import org.aventyrs.core.feat.BestialFeat;
import org.aventyrs.core.feat.DraconicoFeat;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.FeericoFeat;
import org.aventyrs.core.feat.HerancaBestialFeat;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.race.Aviano;
import org.aventyrs.core.race.Bestial;
import org.aventyrs.core.race.Fada;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.race.NascidoDoDragao;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.FlightEffect;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.skill.atletismo.AtletismoCompetencyAbility;
import org.aventyrs.core.title.santo.Santo;
import org.aventyrs.core.util.TranslatableMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Efeito de Voo (core 0.1.7.8): every winged source's price and Duração, the Talentos adjusting
 * them, and the take-off/landing cycle in a Cena de Combate.
 */
class FlightServiceTest {

    private final FlightService flightService = new FlightServiceImpl();
    private final DeterminationPointsService determinationPointsService = new DeterminationPointsServiceImpl();
    private final MagicPointsService magicPointsService = new MagicPointsServiceImpl();

    private static final SceneContext COMBAT = new SceneContext(List.of(), List.of(), Map.of(), null, true, 1, false);
    private static final SceneContext CALM = new SceneContext(List.of(), List.of(), Map.of(), null, false, 0, false);

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /** Every Atributo at 4, so each "Metade do …" term is a visible +2 and the pools have points to spend. */
    private static CharacterSheet sheet(final Race race, final Feat... feats) {
        Map<AttributeDomain, Integer> bases = new java.util.EnumMap<>(AttributeDomain.class);
        Arrays.stream(AttributeDomain.values()).forEach(domain -> bases.put(domain, 4));
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .race(race)
                .attributes(CharacterAttributes.of(bases))
                .feats(new ArrayList<>(List.of(feats)))
                .skillCompetencyAbilities(new ArrayList<>())
                .equipment(new ArrayList<>())
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private static void titled(final CharacterSheet sheet, final TitleSlot... slots) {
        for (TitleSlot slot : slots) {
            sheet.getCharacter().grantTitle(new Santo(List.of(), List.of()), slot);
        }
    }

    private FlightActivation only(final CharacterSheet sheet) {
        List<FlightActivation> activations = flightService.getFlightActivations(sheet);
        assertEquals(1, activations.size(), activations.toString());
        return activations.get(0);
    }

    @Test
    void anAvianosBracosAladosCostOnePdForOneD6PlusHalfDestreza() {
        FlightActivation wings = only(sheet(new Aviano(Aviano.Subtipo.RAPINANTE)));

        assertEquals(new FlightActivation("Braços Alados", ActionCost.NONE, 1, 0, Dice.of(1, 2)), wings);
    }

    @Test
    void asasCostPdToANonFeericoAndPmToAFeerico_lessOnePerTitulo() {
        // Bestiais are Feéricos here, so they pay PM; a plain Humano (builder-granted) shows the PD price.
        CharacterSheet human = sheet(new Human(), FeericoFeat.ASAS);
        CharacterSheet bestial = sheet(new Bestial(), FeericoFeat.ASAS);
        CharacterSheet fada = sheet(new Fada(), FeericoFeat.ASAS);
        titled(fada, TitleSlot.PRIMARY);

        assertEquals(new FlightActivation("Asas", ActionCost.NONE, 3, 0, Dice.of(1, 2)), only(human));
        assertEquals(new FlightActivation("Asas", ActionCost.NONE, 0, 3, Dice.of(1, 2)), only(bestial));
        assertEquals(new FlightActivation("Asas", ActionCost.NONE, 0, 2, Dice.of(1, 2)), only(fada));
    }

    @Test
    void bencaoDeFloraTakesOnePmOffAndReplacesTheDuracao() {
        CharacterSheet fada = sheet(new Fada(), FeericoFeat.ASAS, FeericoFeat.BENCAO_DE_FLORA);

        assertEquals(new FlightActivation("Asas", ActionCost.NONE, 0, 2, Dice.of(2, 2)), only(fada));
    }

    @Test
    void pixieIsAnAcaoLivreOfTwoPm_twoD6PlusHalfCarismaPlusThreePerTitulo() {
        CharacterSheet pixie = sheet(new Fada(), FeericoFeat.PIXIE);
        titled(pixie, TitleSlot.PRIMARY, TitleSlot.SECONDARY);

        assertEquals(new FlightActivation("Pixie", ActionCost.FREE_ACTION, 0, 2, Dice.of(2, 2 + 6)), only(pixie));
    }

    @Test
    void asasDeDragaoCostFourPdLessTwoPerTitulo_neverBelowZero() {
        CharacterSheet dragon = sheet(new NascidoDoDragao(new Human(), ElementalType.FOGO), DraconicoFeat.ASAS_DE_DRAGAO);
        assertEquals(4, only(dragon).determinationPointCost());

        titled(dragon, TitleSlot.PRIMARY, TitleSlot.SECONDARY, TitleSlot.TERTIARY);
        assertEquals(0, only(dragon).determinationPointCost());
    }

    @Test
    void herancaAvianaHeldInItsAcquiredFormCountsEveryHeranca() {
        CharacterSheet bestial = sheet(new Bestial(),
                HerancaBestialFeat.of(BestialFeat.HERANCA_AVIANA, AtletismoCompetencyAbility.PASSO_LARGO),
                HerancaBestialFeat.of(BestialFeat.HERANCA_BOVIDEA, AtletismoCompetencyAbility.PASSO_LARGO));

        assertEquals(new FlightActivation("Herança Aviana", ActionCost.ofActionPoints(2), 3, 0, Dice.of(1, 2)),
                only(bestial));
    }

    @Test
    void coracaoAladoAddsADieAndEternoViajanteRemovesTheLimit() {
        CharacterSheet aviano = sheet(new Aviano(Aviano.Subtipo.RAPINANTE), AvianoFeat.CORACAO_ALADO);
        assertEquals(Dice.of(2, 2), only(aviano).duration());

        aviano.getCharacter().grantFeat(AvianoFeat.ETERNO_VIAJANTE);
        assertTrue(only(aviano).isUnlimited());
    }

    @Test
    void aPricedFlightMustBeBoughtOnlyInCombat() {
        CharacterSheet aviano = sheet(new Aviano(Aviano.Subtipo.RAPINANTE));

        assertTrue(flightService.requiresActivation(aviano, COMBAT));
        assertFalse(flightService.requiresActivation(aviano, CALM));
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> flightService.takeOffFreely(aviano, COMBAT));
        assertEquals(TranslatableMessages.FLIGHT_ACTIVATION_REQUIRED, refused.getMessage());

        flightService.takeOffFreely(aviano, CALM);
        assertTrue(aviano.isFlying());
    }

    @Test
    void aFreeFlightNeverNeedsBuying() {
        CharacterSheet aviano = sheet(new Aviano(Aviano.Subtipo.RAPINANTE));
        aviano.grantBlessing(new Blessing(ModifierType.GRANTS_FLIGHT, 1, 3, TargetScope.SELF, "Draconato"));

        assertFalse(flightService.requiresActivation(aviano, COMBAT));
    }

    @Test
    void withNoWingsTakingOffIsRefused() {
        CharacterSheet grounded = sheet(new Fada());

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> flightService.takeOffFreely(grounded, CALM));
        assertEquals(TranslatableMessages.CANNOT_FLY, refused.getMessage());
    }

    @Test
    void takingOffSpendsTheCostAndLandsSafelyAsTheDuracaoLapses() {
        CharacterSheet aviano = sheet(new Aviano(Aviano.Subtipo.RAPINANTE));
        int before = determinationPointsService.getCurrentDeterminationPoints(aviano.getCharacter(), aviano);

        Integer rounds = flightService.takeOff(aviano, only(aviano), List.of(1), 1);

        assertEquals(3, rounds);
        assertEquals(before - 1, determinationPointsService.getCurrentDeterminationPoints(aviano.getCharacter(), aviano));
        assertTrue(aviano.isFlying());
        aviano.tickTemporaryEffects();
        aviano.tickTemporaryEffects();
        assertTrue(aviano.isFlying());
        aviano.tickTemporaryEffects();
        assertFalse(aviano.isFlying());
        assertTrue(aviano.getRunningEffects().stream().noneMatch(FlightEffect.class::isInstance));
    }

    @Test
    void aPmPricedFlightSpendsMana() {
        CharacterSheet pixie = sheet(new Fada(), FeericoFeat.PIXIE);
        int before = magicPointsService.getCurrentMagicPoints(pixie.getCharacter(), pixie);

        flightService.takeOff(pixie, only(pixie), List.of(3, 4), 1);

        assertEquals(before - 2, magicPointsService.getCurrentMagicPoints(pixie.getCharacter(), pixie));
    }

    @Test
    void landingEarlyEndsTheCountdown() {
        CharacterSheet aviano = sheet(new Aviano(Aviano.Subtipo.RAPINANTE));
        flightService.takeOff(aviano, only(aviano), List.of(6), 1);

        aviano.setFlying(false);

        assertTrue(aviano.getRunningEffects().stream().noneMatch(FlightEffect.class::isInstance));
    }

    @Test
    void anUnlimitedFlightHasNoCountdown() {
        CharacterSheet aviano = sheet(new Aviano(Aviano.Subtipo.RAPINANTE),
                AvianoFeat.CORACAO_ALADO, AvianoFeat.ETERNO_VIAJANTE);

        assertNull(flightService.takeOff(aviano, only(aviano), null, 1));
        for (int round = 0; round < 20; round++) {
            aviano.tickTemporaryEffects();
        }
        assertTrue(aviano.isFlying());
    }

    @Test
    void aWrongRollOrAnUnofferedFlightCostsNothing() {
        CharacterSheet aviano = sheet(new Aviano(Aviano.Subtipo.RAPINANTE));
        int before = determinationPointsService.getCurrentDeterminationPoints(aviano.getCharacter(), aviano);

        assertThrows(IllegalOperationException.class, () -> flightService.takeOff(aviano, only(aviano), List.of(), 1));
        IllegalOperationException refused = assertThrows(IllegalOperationException.class, () -> flightService.takeOff(
                aviano, new FlightActivation("Asas", ActionCost.NONE, 0, 0, Dice.of(1)), List.of(1), 1));

        assertEquals(TranslatableMessages.FLIGHT_ACTIVATION_NOT_AVAILABLE, refused.getMessage());
        assertEquals(before, determinationPointsService.getCurrentDeterminationPoints(aviano.getCharacter(), aviano));
        assertFalse(aviano.isFlying());
    }
}
