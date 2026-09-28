package org.aventyrs.core.monster;

import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.ResourceFormula;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.EgoPointsService;
import org.aventyrs.core.character.services.EgoPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.character.services.MagicPointsService;
import org.aventyrs.core.character.services.MagicPointsServiceImpl;
import org.aventyrs.core.item.ArmorItem;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.sheet.EgoPointType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonsterBlueprintTest {

    private final HitPointsService hitPoints = new HitPointsServiceImpl();
    private final DeterminationPointsService determinationPoints = new DeterminationPointsServiceImpl();
    private final MagicPointsService magicPoints = new MagicPointsServiceImpl();
    private final ActionPointsService actionPoints = new ActionPointsServiceImpl();
    private final EgoPointsService egoPoints = new EgoPointsServiceImpl();

    @Test
    void aSpawnedMonsterIsAMonstruosoNobodyPlays() {
        MonsterSheet goblin = SampleMonster.GOBLIN_SELVAGEM.get().spawn(new Player());
        Character character = goblin.getCharacter();
        assertEquals("Goblin Selvagem", character.getName());
        assertEquals(CreatureType.MONSTRUOSO, character.getRace().getCreatureType());
        assertNull(character.getPlayer());
        assertEquals(ResourceFormula.MONSTER, character.getResourceFormula());
        assertEquals(Optional.of(MonsterCategory.PRESA), goblin.getCategory());
        assertTrue(goblin.getBlueprint().isPresent());
    }

    @Test
    void theBonusBasesFollowTheMonsterFormula() {
        // PV = 20 + Vigor 3 × 5 + 2 Habilidades × 2 = 39; PD = 19 + Instinto 3 × 3; PM = 19 + Foco 1 × 2
        MonsterSheet goblin = SampleMonster.GOBLIN_SELVAGEM.get().spawn(new Player());
        Character character = goblin.getCharacter();
        assertEquals(39, hitPoints.getMaxHitPoints(character, goblin));
        assertEquals(28, determinationPoints.getMaxDeterminationPoints(character, goblin));
        assertEquals(21, magicPoints.getMaxMagicPoints(character, goblin));

        // PV = 20 + 3 × 8 + 5 × 2 = 54; PD = 27 + 9; PM = 27 + 2
        MonsterSheet pantera = SampleMonster.PANTERA_DE_CIRENEIA.get().spawn(new Player());
        assertEquals(54, hitPoints.getMaxHitPoints(pantera.getCharacter(), pantera));
        assertEquals(36, determinationPoints.getMaxDeterminationPoints(pantera.getCharacter(), pantera));
        assertEquals(29, magicPoints.getMaxMagicPoints(pantera.getCharacter(), pantera));
    }

    @Test
    void theCharacterOnlyOverloadsAgreeWithTheSheetOnes() {
        Character character = SampleMonster.GOBLIN_SELVAGEM.get().buildCharacter();
        assertEquals(39, hitPoints.getMaxHitPoints(character));
        assertEquals(28, determinationPoints.getMaxDeterminationPoints(character));
        assertEquals(21, magicPoints.getMaxMagicPoints(character));
    }

    @Test
    void aPlayerCharactersPoolsAreUntouched() {
        CharacterFixture.loadTemplates();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        assertEquals(ResourceFormula.CHARACTER, character.getResourceFormula());
        assertEquals(HitPointsService.BASE_HIT_POINTS + character.getEffectiveAttributeTotal(AttributeDomain.VIGOR)
                * HitPointsService.DEFAULT_LIFE_MULTIPLIER, hitPoints.getMaxHitPoints(character));
    }

    @Test
    void theSheetPresentsItsDerivedGdsAndDefesas() {
        MonsterSheet goblin = SampleMonster.GOBLIN_SELVAGEM.get().spawn(new Player());
        assertEquals(SkillDifficulty.of(DifficultyLevel.EASY, 1), goblin.getSkillDifficulty(SkillType.ATAQUE_CORPO_A_CORPO));
        assertEquals(16, goblin.getDefense(DefenseType.PHYSICAL));
        assertEquals(16, goblin.getDefense(DefenseType.MAGIC));
        assertEquals(16, goblin.getPhysicalDefense());
        assertEquals(13, goblin.getPerception());
        assertEquals(SkillType.values().length, goblin.getSkillDifficulties().size());
    }

    @Test
    void equipmentRaisesTheDerivedDefesas() {
        MonsterBlueprint armoured = SampleMonster.GOBLIN_SELVAGEM.get().toBuilder()
                .equipmentItem(ArmorItem.ROUPA_PESADA).build();
        MonsterSheet goblin = armoured.spawn(new Player());
        assertTrue(goblin.getDefense(DefenseType.PHYSICAL) > 16);
    }

    @Test
    void theMestresDefenseAdjustmentLandsLast() {
        MonsterBlueprint adjusted = SampleMonster.GOBLIN_SELVAGEM.get().toBuilder()
                .adjustments(MonsterAdjustments.builder().physicalDefense(3).magicDefense(-2).build()).build();
        MonsterSheet goblin = adjusted.spawn(new Player());
        assertEquals(19, goblin.getDefense(DefenseType.PHYSICAL));
        assertEquals(14, goblin.getDefense(DefenseType.MAGIC));
    }

    @Test
    void egosStartAtTwoPlusWhatWasAllocated() {
        Character pantera = SampleMonster.PANTERA_DE_CIRENEIA.get().buildCharacter();
        assertEquals(5, pantera.getEgos().getEgo(EgoDomain.INICIATIVA).getTotal());
        assertEquals(5, pantera.getEgos().getEgo(EgoDomain.SORTE).getTotal());
        assertEquals(2, pantera.getEgos().getEgo(EgoDomain.AUTOCONTROLE).getTotal());
    }

    @Test
    void thePredadorsExtraActionPointReachesTheService() {
        MonsterSheet pantera = SampleMonster.PANTERA_DE_CIRENEIA.get().spawn(new Player());
        assertEquals(4, pantera.getCharacter().getActionPoints());
        // + Celeridade's PA +1
        assertEquals(5, actionPoints.getMaxActionPoints(pantera, 1));
    }

    @Test
    void trainedPericiasAreOnTheCharacterWithNoGraduacao() {
        Character goblin = SampleMonster.GOBLIN_SELVAGEM.get().buildCharacter();
        assertEquals(4, goblin.getSkills().size());
        assertEquals(0, goblin.getSkills().get(SkillType.ATAQUE_CORPO_A_CORPO).getGraduation().getGraduationValue());
    }

    @Test
    void twoSpawnsFromOneBlueprintAreFullyIndependent() {
        MonsterBlueprint blueprint = SampleMonster.PANTERA_DE_CIRENEIA.get();
        MonsterSheet first = blueprint.spawn(new Player());
        MonsterSheet second = blueprint.spawn(new Player());

        first.applyDamage(10);
        assertEquals(0, second.getDamageTaken());
        assertNotSame(first.getCharacter().getActiveAbilities().get(0), second.getCharacter().getActiveAbilities().get(0));
    }

    @Test
    void aKnownIdIsRestored() {
        UUID id = UUID.randomUUID();
        assertEquals(id, SampleMonster.GOBLIN_SELVAGEM.get().spawn(new Player(), id).getId());
    }

    @Test
    void aRegularMonsterUsesAtMostTwoEgoEffectsPerCena() {
        MonsterSheet pantera = SampleMonster.PANTERA_DE_CIRENEIA.get().spawn(new Player());
        egoPoints.useEgoPointsForEffect(pantera, EgoDomain.SORTE, EgoPointType.TEMPORARY, 1, 1);
        egoPoints.useEgoPointsForEffect(pantera, EgoDomain.SORTE, EgoPointType.TEMPORARY, 1, 1);
        assertEquals(0, pantera.getRemainingEgoEffects());
        int before = pantera.getTemporaryEgoPoints(EgoDomain.SORTE);
        assertThrows(IllegalOperationException.class,
                () -> egoPoints.useEgoPointsForEffect(pantera, EgoDomain.SORTE, EgoPointType.TEMPORARY, 1, 1));
        assertEquals(before, pantera.getTemporaryEgoPoints(EgoDomain.SORTE), "a refused Efeito spends nothing");

        pantera.beginScene();
        egoPoints.useEgoPointsForEffect(pantera, EgoDomain.SORTE, EgoPointType.TEMPORARY, 1, 1);
        assertEquals(1, pantera.getRemainingEgoEffects());
    }

    @Test
    void anExemplarHasNoEgoEffectLimit() {
        MonsterBlueprint blueprint = SampleMonster.PANTERA_DE_CIRENEIA.get().toBuilder().kind(MonsterKind.EXEMPLAR).build();
        MonsterSheet exemplar = blueprint.spawn(new Player());
        for (int i = 0; i < 3; i++) {
            egoPoints.useEgoPointsForEffect(exemplar, EgoDomain.SORTE, EgoPointType.TEMPORARY, 1, 1);
        }
        assertEquals(Integer.MAX_VALUE, exemplar.getRemainingEgoEffects());
    }

    @Test
    void aFixedStatBlockKeepsItsAuthoredNumbers() {
        MonsterSheet capanga = GenericMonster.CAPANGA.spawn(new Player());
        assertEquals(Optional.empty(), capanga.getBlueprint());
        assertEquals(13, capanga.getPhysicalDefense());
        assertEquals(ResourceFormula.CHARACTER, capanga.getCharacter().getResourceFormula());
    }
}
