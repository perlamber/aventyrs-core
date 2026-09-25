package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.StrengthAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.FormaMetamorfica;
import org.aventyrs.core.feat.MetamorfoseDraculeaFeat;
import org.aventyrs.core.item.AbstractItem;
import org.aventyrs.core.item.DefensiveImprovement;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemImprovement;
import org.aventyrs.core.item.PowerStone;
import org.aventyrs.core.item.PowerStoneQuality;
import org.aventyrs.core.item.PowerStoneType;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.Pequenino;
import org.aventyrs.core.race.Vampiro;
import org.aventyrs.core.scene.Altitude;
import org.aventyrs.core.scene.EnvironmentalState;
import org.aventyrs.core.scene.LightLevel;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.grid.GridPosition;
import org.aventyrs.core.scene.grid.MovementMap;
import org.aventyrs.core.scene.grid.StepRules;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.FormType;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Terreno Difícil as a per-hex cost, the four things that ignore it, and occupancy. */
class MovementTerrainServiceTest {

    private static final GridPosition HEX = new GridPosition(3, 3);

    private final MovementTerrainService service = new MovementTerrainServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character.CharacterBuilder character() {
        return CharacterFixture.blank(CharacterFixture.BLANK).id(UUID.randomUUID()).feats(new ArrayList<>());
    }

    private static CharacterSheet sheet(final Character character) {
        return CharacterSheet.of(character, new Player());
    }

    private static MovementMap difficult(final GridPosition hex) {
        return new MovementMap(10, 10, Set.of(hex), Map.of());
    }

    private static SceneContext enemiesContext(final CombatantSheet... enemies) {
        return new SceneContext(List.of(), List.of(enemies), Map.of(), null, true, 0, false);
    }

    @Test
    void terrenoDificilCostsTwoUdAndOrdinaryGroundOne() {
        StepRules rules = service.stepRules(sheet(character().build()), difficult(HEX), null, 0);

        assertEquals(MovementTerrainService.DIFFICULT_TERRAIN_COST, rules.enterCost(HEX));
        assertEquals(1, rules.enterCost(new GridPosition(4, 4)));
        assertTrue(rules.isDifficult(HEX));
    }

    @Test
    void movimentoLivreIgnoresItOnTheRodadasFirstMovementOnly() {
        CharacterSheet strong = sheet(character().attributeAbility(StrengthAbility.MOVIMENTO_LIVRE).build());

        assertEquals(1, service.stepRules(strong, difficult(HEX), null, 0).enterCost(HEX));
        assertEquals(2, service.stepRules(strong, difficult(HEX), null, 1).enterCost(HEX));
        assertTrue(service.stepRules(strong, difficult(HEX), null, 0).isDifficult(HEX),
                "ignoring the cost does not make the hex ordinary ground");
    }

    @Test
    void cavaloDeChifresIgnoresItWhileWorn() {
        Character vampire = character().race(new Vampiro(Vampiro.VampiroLineage.NOSFERATU, new Human())).build();
        vampire.grantFeat(new MetamorfoseDraculeaFeat(EnumSet.of(
                FormaMetamorfica.CAVALO_DE_CHIFRES, FormaMetamorfica.MORCEGO_ATROZ)));
        CharacterSheet sheet = sheet(vampire);

        assertFalse(service.ignoresDifficultTerrain(sheet, 1, null));
        sheet.enterForm(FormType.CAVALO_DE_CHIFRES);
        assertTrue(service.ignoresDifficultTerrain(sheet, 1, null));
        sheet.enterForm(FormType.MORCEGO_ATROZ);
        assertFalse(service.ignoresDifficultTerrain(sheet, 1, null));
    }

    @Test
    void aSocketedRutiloSubterraneoIgnoresIt() {
        AbstractItem armor = AbstractItem.builder().name("Peitoral Encaixado").category(ItemCategory.ARMOR).build();
        armor.addImprovement(ItemImprovement.of(DefensiveImprovement.ENCAIXE));
        armor.setPowerStone(PowerStone.of(PowerStoneType.RUTILO_SUBTERRANEO, PowerStoneQuality.JOIA));
        CharacterSheet bearer = sheet(character()
                .attributes(CharacterAttributes.of(Map.of(AttributeDomain.DEXTERITY, 3)))
                .equipment(List.of(armor)).build());

        assertTrue(service.ignoresDifficultTerrain(bearer, 1, null));
    }

    @Test
    void sempreVelozIgnoresItOnLandButNotAloftOrSubmerged() {
        CharacterSheet pequenino = sheet(character().race(new Pequenino()).build());

        assertTrue(service.ignoresDifficultTerrain(pequenino, 1, EnvironmentalState.ORDINARY));
        assertFalse(service.ignoresDifficultTerrain(pequenino, 1,
                new EnvironmentalState(LightLevel.NORMAL, Altitude.ORDINARY, true, false)));
        assertFalse(service.ignoresDifficultTerrain(pequenino, 1,
                new EnvironmentalState(LightLevel.NORMAL, Altitude.ORDINARY, false, true)));
    }

    @Test
    void anAllyMayBePassedButNotStoppedOnAndAnEnemyBlocks() {
        CharacterSheet mover = sheet(character().build());
        CharacterSheet ally = sheet(character().build());
        CharacterSheet enemy = sheet(character().build());
        GridPosition enemyHex = new GridPosition(5, 5);
        MovementMap map = new MovementMap(10, 10, Set.of(),
                Map.of(HEX, List.of(ally), enemyHex, List.of(enemy)));

        StepRules rules = service.stepRules(mover, map, enemiesContext(enemy), 0);

        assertTrue(rules.canPass(HEX));
        assertFalse(rules.canStop(HEX));
        assertFalse(rules.canPass(enemyHex));
        assertFalse(rules.canStop(enemyHex));
        assertFalse(rules.isDifficult(enemyHex), "an enemy's space is only Terreno Difícil to one who may enter it");
    }

    @Test
    void aDefeatedFoeIsPassableTerrenoDificilButNotAPlaceToStop() {
        CharacterSheet mover = sheet(character().build());
        CharacterSheet fallen = sheet(character().build());
        CharacterSheet fallenAlly = sheet(character().build());
        GridPosition allyHex = new GridPosition(5, 5);
        MovementMap map = new MovementMap(10, 10, Set.of(),
                Map.of(HEX, List.of(fallen), allyHex, List.of(fallenAlly)),
                Set.of(fallen.getId(), fallenAlly.getId()));

        StepRules rules = service.stepRules(mover, map, enemiesContext(fallen), 0);

        assertTrue(rules.canPass(HEX));
        assertFalse(rules.canStop(HEX));
        assertTrue(rules.isDifficult(HEX));
        assertEquals(MovementTerrainService.DIFFICULT_TERRAIN_COST, rules.enterCost(HEX));
        assertFalse(rules.isDifficult(allyHex), "only a fallen foe becomes Terreno Difícil");
        assertFalse(rules.avoidingDifficultTerrain().canPass(HEX), "an Investida cannot cross a fallen foe");
    }

    @Test
    void theMoversOwnSpaceIsNeverOccupiedAgainstThem() {
        CharacterSheet mover = sheet(character().build());
        MovementMap map = new MovementMap(10, 10, Set.of(), Map.of(HEX, List.of(mover)));

        assertTrue(service.stepRules(mover, map, null, 0).canStop(HEX));
    }

    @Test
    void anEmptyReachAndRangeContextStillBuildsRules() {
        CharacterSheet mover = sheet(character().build());
        SceneContext context = new SceneContext(List.of(), List.of(), Map.<CombatantSheet, Range>of());

        assertEquals(1, service.stepRules(mover, difficult(HEX), context, 0).enterCost(new GridPosition(0, 0)));
    }
}
