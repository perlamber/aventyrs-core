package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.MovementMode;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.AbstractFeat;
import org.aventyrs.core.feat.AvianoFeat;
import org.aventyrs.core.feat.BestialFeat;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.feat.FeatRequirements;
import org.aventyrs.core.feat.FeericoFeat;
import org.aventyrs.core.feat.FormaMetamorfica;
import org.aventyrs.core.feat.HerancaBestialFeat;
import org.aventyrs.core.feat.MetamorfoseDraculeaFeat;
import org.aventyrs.core.feat.MobilidadeFeat;
import org.aventyrs.core.item.BootsItem;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.race.Aviano;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.race.RacialTraitSuppression;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.FormType;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.atletismo.AtletismoCompetencyAbility;
import org.aventyrs.core.skill.atletismo.AtletismoSpecialization;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The movement modes beside land — Voo, Natação, Vertical — as {@link MovementService} resolves
 * them: who has one, and what it is worth per Ponto de Ação.
 */
class MovementModeTest {

    private final MovementService movementService = new MovementServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character.CharacterBuilder character() {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .skillCompetencyAbilities(new ArrayList<>())
                .equipment(new ArrayList<>());
    }

    private static CharacterSheet sheet(final Character character) {
        return CharacterSheet.of(character, new Player());
    }

    private static CharacterSheet holding(final Feat... feats) {
        return sheet(character().feats(new ArrayList<>(List.of(feats))).build());
    }

    /** A Rapinante's "enquanto voando seu Movimento Base aumenta em +2UD". */
    private static final int RAPINANTE_FLIGHT_BONUS = 2;

    /** A Rapinante — so every flight figure below carries its +2UD. */
    private static CharacterSheet aviano(final Feat... feats) {
        return sheet(character().race(new Aviano(Aviano.Subtipo.RAPINANTE))
                .feats(new ArrayList<>(List.of(feats))).build());
    }

    private static CharacterSheet withAbilities(final Map<AttributeDomain, Integer> attributes,
                                                final List<SkillCompetencyAbility> abilities, final Item... equipment) {
        return sheet(character()
                .attributes(CharacterAttributes.of(attributes))
                .skillCompetencyAbilities(new ArrayList<>(abilities))
                .equipment(new ArrayList<>(List.of(equipment)))
                .build());
    }

    // ---------- who has a mode ----------

    @Test
    void everyoneHasLandAndNobodyElseByDefault() {
        CharacterSheet plain = holding();

        assertTrue(movementService.hasMovementMode(plain, MovementMode.LAND));
        for (MovementMode mode : List.of(MovementMode.FLIGHT, MovementMode.SWIM, MovementMode.CLIMB)) {
            assertFalse(movementService.hasMovementMode(plain, mode), mode.name());
            assertEquals(0, movementService.getMovementBase(plain, mode), mode.name());
        }
    }

    /**
     * ⚠️ With no figure stated, a mode starts from the land Movimento Base ("igual à sua velocidade
     * em terra"); the Rapinante's own +2UD then adds on top.
     */
    @Test
    void anAvianoFliesFromItsLandMovimento() {
        CharacterSheet aviano = aviano();

        assertTrue(movementService.hasMovementMode(aviano, MovementMode.FLIGHT));
        assertEquals(movementService.getMovementBase(aviano) + RAPINANTE_FLIGHT_BONUS,
                movementService.getMovementBase(aviano, MovementMode.FLIGHT));
    }

    /** "Enquanto voando seu Movimento Base aumenta em +2UD" (Rapinante) / "+1UD" (Correnuvens). */
    @Test
    void theAvianoSubtypeWidensItsFlight() {
        CharacterSheet rapinante = sheet(character().race(new Aviano(Aviano.Subtipo.RAPINANTE)).build());
        CharacterSheet correnuvens = sheet(character().race(new Aviano(Aviano.Subtipo.CORRENUVENS)).build());

        assertEquals(movementService.getMovementBase(rapinante) + 2,
                movementService.getMovementBase(rapinante, MovementMode.FLIGHT));
        assertEquals(movementService.getMovementBase(correnuvens) + 1,
                movementService.getMovementBase(correnuvens, MovementMode.FLIGHT));
    }

    @Test
    void landIsTheOrdinaryMovimentoBase() {
        CharacterSheet plain = holding();

        assertEquals(movementService.getMovementBase(plain), movementService.getMovementBase(plain, MovementMode.LAND));
    }

    @Test
    void aFormaSuppressingPhysicalTraitsGroundsTheAviano() {
        Feat shapeshift = new AbstractFeat(FeatCategory.DESTINO, "test", FeatRequirements.builder().build()) {
            @Override
            public RacialTraitSuppression resolveRacialTraitSuppression(final Character character,
                                                                        final CombatantSheet sheet) {
                return RacialTraitSuppression.PHYSICAL;
            }
        };

        CharacterSheet shifted = aviano(shapeshift);
        // Suppression is folded only while a Forma is worn.
        shifted.enterForm(FormType.HUMANA);

        assertFalse(movementService.hasMovementMode(shifted, MovementMode.FLIGHT));
    }

    @Test
    void movementPreventedStopsEveryMode() {
        CharacterSheet aviano = aviano();
        aviano.applyCondition(new Condition(ConditionType.AGARRADO, 2, null));

        assertEquals(0, movementService.getMovementBase(aviano, MovementMode.FLIGHT));
    }

    // ---------- Talentos ----------

    @Test
    void coracaoAladoAddsTwoPerTituloToFlightOnly() {
        CharacterSheet aviano = aviano(AvianoFeat.CORACAO_ALADO);
        int land = movementService.getMovementBase(aviano);
        aviano.getCharacter().grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);

        assertEquals(land + RAPINANTE_FLIGHT_BONUS + 2, movementService.getMovementBase(aviano, MovementMode.FLIGHT));
        assertEquals(land, movementService.getMovementBase(aviano));
    }

    @Test
    void bracosLivresCostsThreeUdOfFlight() {
        CharacterSheet aviano = aviano(AvianoFeat.BRACOS_LIVRES);

        assertEquals(Math.max(0, movementService.getMovementBase(aviano) + RAPINANTE_FLIGHT_BONUS - 3),
                movementService.getMovementBase(aviano, MovementMode.FLIGHT));
    }

    @Test
    void pixieFliesAtEightUdWhateverItsLandFigure() {
        CharacterSheet pixie = holding(FeericoFeat.PIXIE);

        assertTrue(movementService.hasMovementMode(pixie, MovementMode.FLIGHT));
        assertEquals(8, movementService.getMovementBase(pixie, MovementMode.FLIGHT));
    }

    @Test
    void feericoWingsGrantFlightTwoAboveLand() {
        CharacterSheet winged = holding(FeericoFeat.ASAS);

        assertEquals(movementService.getMovementBase(winged) + 2,
                movementService.getMovementBase(winged, MovementMode.FLIGHT));
    }

    /** "Movimento Base de Natação 6UD, mas em terra seu Movimento Base é reduzido para apenas 2UD". */
    @Test
    void sirenideoSwimsAtSixAndWalksAtTwo() {
        CharacterSheet mermaid = holding(FeericoFeat.SIRENIDEO);

        assertEquals(6, movementService.getMovementBase(mermaid, MovementMode.SWIM));
        assertEquals(2, movementService.getMovementBase(mermaid));
    }

    /** A held Herança is its acquired form, which forwards the mode. */
    @Test
    void anAcquiredHerancaReptilianaClimbs() {
        CharacterSheet reptile = holding(HerancaBestialFeat.of(BestialFeat.HERANCA_REPTILIANA,
                AtletismoSpecialization.ACROBATA));

        assertTrue(movementService.hasMovementMode(reptile, MovementMode.CLIMB));
        assertFalse(movementService.hasMovementMode(reptile, MovementMode.FLIGHT));
    }

    @Test
    void aMorcegoAtrozFliesOnlyWhileTheShapeIsWorn() {
        CharacterSheet vampire = holding(new MetamorfoseDraculeaFeat(Set.of(FormaMetamorfica.MORCEGO_ATROZ)));

        assertFalse(movementService.hasMovementMode(vampire, MovementMode.FLIGHT));
        vampire.enterForm(FormType.MORCEGO_ATROZ);
        assertTrue(movementService.hasMovementMode(vampire, MovementMode.FLIGHT));
    }

    // ---------- Habilidades de Competência and items ----------

    @Test
    void anfibioGrantsSwimmingAndAlpinistaVelozClimbing() {
        CharacterSheet athlete = withAbilities(Map.of(),
                List.of(AtletismoCompetencyAbility.ANFIBIO, AtletismoCompetencyAbility.ALPINISTA_VELOZ));

        assertTrue(movementService.hasMovementMode(athlete, MovementMode.SWIM));
        assertTrue(movementService.hasMovementMode(athlete, MovementMode.CLIMB));
        assertFalse(movementService.hasMovementMode(athlete, MovementMode.FLIGHT));
    }

    @Test
    void finsAddTwoToAnExistingSwimFigure() {
        CharacterSheet swimmer = withAbilities(Map.of(AttributeDomain.STRENGTH, 3),
                List.of(AtletismoCompetencyAbility.ANFIBIO), BootsItem.NADADEIRAS_DECIEMBRANAS);
        CharacterSheet barefoot = withAbilities(Map.of(AttributeDomain.STRENGTH, 3),
                List.of(AtletismoCompetencyAbility.ANFIBIO));

        assertEquals(movementService.getMovementBase(barefoot, MovementMode.SWIM) + 2,
                movementService.getMovementBase(swimmer, MovementMode.SWIM));
    }

    /** ⚠️ "Aumentam" raises a figure the wearer has; boots alone grant no climbing. */
    @Test
    void climbingBootsAloneGrantNoClimbing() {
        CharacterSheet booted = withAbilities(Map.of(AttributeDomain.STRENGTH, 3), List.of(),
                BootsItem.BOTINAS_DE_ESCALADA);

        assertEquals(0, movementService.getMovementBase(booted, MovementMode.CLIMB));
    }

    // ---------- MobilidadeFeat#INVESTIDA_AQUATICA ----------

    @Test
    void investidaAquaticaDiscountsNothingForANonSwimmer() {
        Character landlubber = character().feats(new ArrayList<>(List.of(MobilidadeFeat.INVESTIDA_AQUATICA))).build();

        assertEquals(ChargeService.BASE_ACTION_POINT_COST,
                new ChargeServiceImpl().getActionPointCost(landlubber).actionPoints());
    }

    @Test
    void theCharacterOverloadIsFormaBlind() {
        Race avianoRace = new Aviano(Aviano.Subtipo.values()[0]);

        assertTrue(movementService.hasMovementMode(character().race(avianoRace).build(), MovementMode.FLIGHT));
        assertFalse(movementService.hasMovementMode(character().build(), MovementMode.SWIM));
    }
}
