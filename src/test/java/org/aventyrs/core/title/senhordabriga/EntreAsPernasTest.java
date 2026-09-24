package org.aventyrs.core.title.senhordabriga;

import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.services.CriticalService;
import org.aventyrs.core.character.services.CriticalServiceImpl;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.character.services.MovementTerrainService;
import org.aventyrs.core.character.services.MovementTerrainServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.EnvironmentalState;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.grid.GridPosition;
import org.aventyrs.core.scene.grid.MovementMap;
import org.aventyrs.core.scene.grid.MovementPathfinder;
import org.aventyrs.core.scene.grid.StepRules;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Entre as Pernas: trespassing enemy spaces as Terreno Difícil, sharing a larger enemy's space for
 * +1 Defesas per Categoria, and Malícia de Valentão's any-space sharing with +2 Defensive margin.
 */
class EntreAsPernasTest {

    private static final GridPosition FOE_HEX = new GridPosition(4, 4);

    private final MovementTerrainService terrain = new MovementTerrainServiceImpl();
    private final DefenseService defenseService = new DefenseServiceImpl();
    private final CriticalService criticalService = new CriticalServiceImpl();

    @BeforeEach
    void setup() {
        SenhorDaBrigaFixtures.loadTemplates();
    }

    private static SenhorDaBriga fantasma(final AventyrTitleAbility... abilities) {
        return new SenhorDaBriga(List.of(SenhorDaBrigaSpecialization.FANTASMA_DO_RINGUE), List.of(abilities));
    }

    /** A foe grown by categories Categorias de Tamanho for a few Rodadas. */
    private static CharacterSheet foe(final int categories) {
        CharacterSheet foe = SenhorDaBrigaFixtures.combatant();
        if (categories != 0) {
            foe.grantTemporaryBonus(ModifierType.SIZE_CATEGORY, categories, 5);
        }
        return foe;
    }

    private StepRules rules(final CombatantSheet mover, final CombatantSheet foe) {
        MovementMap map = new MovementMap(10, 10, Set.of(), Map.of(FOE_HEX, List.of(foe)));
        return terrain.stepRules(mover, map, SenhorDaBrigaFixtures.context(0, Map.of(foe, Range.ADJACENTE)), 1);
    }

    /** holder's context standing in foe's space, as the defence roll against foe builds it. */
    private static SceneContext sharing(final CombatantSheet foe) {
        EnvironmentalState state = EnvironmentalState.ORDINARY.withPosition(true, Set.of(foe.getId()));
        return new SceneContext(List.of(), List.of(foe), Map.of(foe, Range.ADJACENTE), null, true, 0, false, foe,
                null, state);
    }

    @Test
    void aHolderMayTrespassAnEnemysSpaceAtTerrenoDificilCost() {
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(fantasma(FantasmaDoRingueAbility.ENTRE_AS_PERNAS));
        CharacterSheet plain = SenhorDaBrigaFixtures.combatant();
        CharacterSheet foe = foe(0);

        StepRules holderRules = rules(holder, foe);

        assertTrue(holderRules.canPass(FOE_HEX));
        assertTrue(holderRules.isDifficult(FOE_HEX));
        assertEquals(2, holderRules.enterCost(FOE_HEX));
        assertFalse(rules(plain, foe).canPass(FOE_HEX));
    }

    @Test
    void aHolderMayStopOnlyInALargerEnemysSpace() {
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(fantasma(FantasmaDoRingueAbility.ENTRE_AS_PERNAS));

        assertFalse(rules(holder, foe(0)).canStop(FOE_HEX), "same size");
        assertTrue(rules(holder, foe(2)).canStop(FOE_HEX), "two Categorias larger");
    }

    @Test
    void maliciaDeValentaoSharesAnySpaceWithAnyone() {
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(fantasma(FantasmaDoRingueAbility.ENTRE_AS_PERNAS,
                FantasmaDoRingueAbility.CRUZ_DE_SANGUE, FantasmaDoRingueAbility.FINGIR_FRAQUEZAS,
                FantasmaDoRingueAbility.MALICIA_DE_VALENTAO));
        CharacterSheet ally = SenhorDaBrigaFixtures.combatant();
        MovementMap map = new MovementMap(10, 10, Set.of(), Map.of(FOE_HEX, List.of(ally)));

        assertTrue(rules(holder, foe(0)).canStop(FOE_HEX));
        assertTrue(terrain.stepRules(holder, map, null, 1).canStop(FOE_HEX), "an ally too");
    }

    @Test
    void thePathThroughAFoeIsPricedAsTerrenoDificil() {
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(fantasma(FantasmaDoRingueAbility.ENTRE_AS_PERNAS));
        CharacterSheet foe = foe(0);
        GridPosition from = new GridPosition(4, 3);
        GridPosition beyond = new GridPosition(4, 5);
        // Wall off every way around the foe so the only route is through their space.
        Set<GridPosition> wall = Set.of(new GridPosition(3, 3), new GridPosition(3, 4), new GridPosition(5, 3),
                new GridPosition(5, 4), new GridPosition(3, 5), new GridPosition(5, 5));
        MovementMap map = new MovementMap(10, 10, Set.of(), Map.of(FOE_HEX, List.of(foe)));
        StepRules through = terrain.stepRules(holder, map, SenhorDaBrigaFixtures.context(0, Map.of(foe, Range.ADJACENTE)), 1);
        StepRules walled = new StepRules() {
            @Override public boolean canPass(final GridPosition hex) { return !wall.contains(hex) && through.canPass(hex); }
            @Override public boolean canStop(final GridPosition hex) { return !wall.contains(hex) && through.canStop(hex); }
            @Override public boolean isDifficult(final GridPosition hex) { return through.isDifficult(hex); }
            @Override public int enterCost(final GridPosition hex) { return through.enterCost(hex); }
        };

        var path = new MovementPathfinder(10, 10).pathTo(from, beyond, walled).orElseThrow();

        assertEquals(3, path.cost(), "the foe's space at 2UD, then the destination at 1UD");
        assertTrue(path.touchesDifficultTerrain());
    }

    @Test
    void sharingALargerFoesSpaceGrantsOneDefesaPerCategoriaAgainstEveryone() {
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(fantasma(FantasmaDoRingueAbility.ENTRE_AS_PERNAS));
        CharacterSheet giant = foe(3);
        int apart = defenseService.getTotalDefense(holder, DefenseType.PHYSICAL,
                SenhorDaBrigaFixtures.context(0, Map.of(giant, Range.ADJACENTE)));

        int together = defenseService.getTotalDefense(holder, DefenseType.PHYSICAL, sharing(giant));

        assertEquals(apart + 3, together);
    }

    @Test
    void aSameSizedFoeGrantsNothing() {
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(fantasma(FantasmaDoRingueAbility.ENTRE_AS_PERNAS));
        CharacterSheet peer = foe(0);

        assertEquals(defenseService.getTotalDefense(holder, DefenseType.PHYSICAL,
                        SenhorDaBrigaFixtures.context(0, Map.of(peer, Range.ADJACENTE))),
                defenseService.getTotalDefense(holder, DefenseType.PHYSICAL, sharing(peer)));
    }

    @Test
    void maliciaWidensTheDefensiveMarginOnlyWhileSharing() {
        SenhorDaBriga title = fantasma(FantasmaDoRingueAbility.ENTRE_AS_PERNAS, FantasmaDoRingueAbility.CRUZ_DE_SANGUE,
                FantasmaDoRingueAbility.FINGIR_FRAQUEZAS, FantasmaDoRingueAbility.MALICIA_DE_VALENTAO);
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(title);
        CharacterSheet foe = foe(0);
        int apart = criticalService.sumCriticalMarginIncrease(holder, SkillType.ESQUIVA_E_APARAR, null,
                SenhorDaBrigaFixtures.context(0, Map.of(foe, Range.ADJACENTE)));

        assertEquals(apart + SenhorDaBriga.MALICIA_SHARED_SPACE_DEFENSE_MARGIN,
                criticalService.sumCriticalMarginIncrease(holder, SkillType.ESQUIVA_E_APARAR, null, sharing(foe)));
        assertEquals(criticalService.sumCriticalMarginIncrease(holder, SkillType.ATAQUE_CORPO_A_CORPO, null, null),
                criticalService.sumCriticalMarginIncrease(holder, SkillType.ATAQUE_CORPO_A_CORPO, null, sharing(foe)),
                "a Defensive margin only");
    }

    @Test
    void withoutMaliciaSharingWidensNoMargin() {
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(fantasma(FantasmaDoRingueAbility.ENTRE_AS_PERNAS));
        CharacterSheet giant = foe(3);

        assertEquals(criticalService.sumCriticalMarginIncrease(holder, SkillType.ESQUIVA_E_APARAR, null,
                        SenhorDaBrigaFixtures.context(0, Map.of(giant, Range.ADJACENTE))),
                criticalService.sumCriticalMarginIncrease(holder, SkillType.ESQUIVA_E_APARAR, null, sharing(giant)));
    }
}
