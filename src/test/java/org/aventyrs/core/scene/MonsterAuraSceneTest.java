package org.aventyrs.core.scene;

import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.DamageSanctity;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.ActiveAbilityServiceImpl;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.monster.MonsterBlueprint;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.monster.MonstrousAbilitySelection;
import org.aventyrs.core.monster.SampleMonster;
import org.aventyrs.core.monster.model.brotosdemapinguari.BrotosDeMapinguariAbility;
import org.aventyrs.core.monster.model.cireneia.CireneiaAbility;
import org.aventyrs.core.monster.model.escolhidodemacaria.EscolhidoDeMacariaAbility;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.util.DiceRoller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.aventyrs.core.monster.model.MonsterTestKit.held;
import static org.aventyrs.core.monster.model.MonsterTestKit.spawn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The monster aura ledger and pulse (core 0.1.7.4): coverage granted and taken back from the holder's own
 * distances, the Paradoxo's lingering -2PA and +3PA while occupied, an aura that ends after standing empty, and the
 * per-Rodada pulse resolved once for everyone at the holder's Turn start.
 */
class MonsterAuraSceneTest {

    private final ActionPointsServiceImpl actionPoints = new ActionPointsServiceImpl();

    private Scene scene;
    private UUID monsters;
    private UUID party;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        scene = new Scene();
        monsters = UUID.randomUUID();
        party = UUID.randomUUID();
    }

    private CharacterSheet hero() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }

    private MonsterSheet paradoxo() {
        MonsterBlueprint apex = SampleMonster.PANTERA_DE_CIRENEIA.get().toBuilder().powerDegree(50)
                .ability(MonstrousAbilitySelection.of(CireneiaAbility.AURA_DO_PARADOXO)).build();
        return apex.spawn(new Player());
    }

    private SceneContext from(final CombatantSheet holder, final Map<CombatantSheet, Range> distances) {
        return scene.buildContext(holder, distances);
    }

    @Test
    void auraDoParadoxoTakesTwoPaFromEveryoneInsideAndGivesItsHolderThreeWhileAnEnemyIs() {
        MonsterSheet pantera = paradoxo();
        CharacterSheet hero = hero();
        scene.addParticipant(pantera, 20, monsters);
        scene.addParticipant(hero, 10, party);
        int heroBefore = actionPoints.getMaxActionPoints(hero, 1);
        int panteraBefore = actionPoints.getMaxActionPoints(pantera, 1);

        new ActiveAbilityServiceImpl().activate(pantera.getCharacter(), pantera, held(pantera, "Aura do Paradoxo"), 1);
        assertTrue(pantera.isConcentrating());
        scene.refreshMonsterAuras(pantera, from(pantera, Map.of(hero, Range.DISTANCIA_MEDIA)));

        assertEquals(heroBefore - 2, actionPoints.getMaxActionPoints(hero, 1));
        assertEquals(panteraBefore + 3, actionPoints.getMaxActionPoints(pantera, 1));

        // The hero walks out: the holder's +3 goes at once, the hero's -2 lingers for a Rodada.
        scene.refreshMonsterAuras(pantera, from(pantera, Map.of(hero, Range.DISTANCIA_LONGA)));
        assertEquals(panteraBefore, actionPoints.getMaxActionPoints(pantera, 1));
        assertEquals(heroBefore - 2, actionPoints.getMaxActionPoints(hero, 1));
        hero.tickTemporaryEffects();
        assertEquals(heroBefore, actionPoints.getMaxActionPoints(hero, 1));
    }

    @Test
    void anotherParadoxoHolderIsImmune() {
        MonsterSheet one = paradoxo();
        MonsterSheet two = paradoxo();
        scene.addParticipant(one, 20, monsters);
        scene.addParticipant(two, 10, party);
        int before = actionPoints.getMaxActionPoints(two, 1);
        new ActiveAbilityServiceImpl().activate(one.getCharacter(), one, held(one, "Aura do Paradoxo"), 1);
        scene.refreshMonsterAuras(one, from(one, Map.of(two, Range.ADJACENTE)));
        assertEquals(before, actionPoints.getMaxActionPoints(two, 1));
    }

    @Test
    void theParadoxoEndsAfterTwoPulsesWithNobodyElseInside() {
        MonsterSheet pantera = paradoxo();
        CharacterSheet hero = hero();
        scene.addParticipant(pantera, 20, monsters);
        scene.addParticipant(hero, 10, party);
        new ActiveAbilityServiceImpl().activate(pantera.getCharacter(), pantera, held(pantera, "Aura do Paradoxo"), 1);
        SceneContext far = from(pantera, Map.of(hero, Range.DISTANCIA_LONGA));

        scene.pulseMonsterAuras(pantera, far, 1, null, null);
        assertTrue(pantera.hasEffectFrom("Aura do Paradoxo"));
        scene.pulseMonsterAuras(pantera, far, 2, null, null);
        assertFalse(pantera.hasEffectFrom("Aura do Paradoxo"));
        assertTrue(pantera.resolveMonsterAuras().isEmpty());
    }

    @Test
    void presencaDoCeifeiroHurtsOnlyLivingEnemiesAndStealsWhatItDeals() {
        MonsterSheet anunciador = spawn(26, MonstrousAbilitySelection.of(EscolhidoDeMacariaAbility.ANUNCIADOR_DA_ESCURIDAO));
        CharacterSheet hero = hero();
        MonsterSheet undeadFoe = spawn(0, MonstrousAbilitySelection.of(EscolhidoDeMacariaAbility.CARRASCO_MORIMBUNDO));
        scene.addParticipant(anunciador, 20, monsters);
        scene.addParticipant(hero, 10, party);
        scene.addParticipant(undeadFoe, 5, party);
        SceneContext context = from(anunciador, Map.of(hero, Range.ADJACENTE, undeadFoe, Range.ADJACENTE));

        List<AuraPulseResult> results = scene.pulseMonsterAuras(anunciador, context, 1, DiceRoller.fixed(List.of(4)), null);
        assertEquals(1, results.size());
        AuraPulseResult presenca = results.get(0);
        assertTrue(presenca.lifeSteal());
        assertEquals(List.of(hero), presenca.hits().stream().map(AuraHit::target).toList());
        AuraHit hit = presenca.hits().get(0);
        assertEquals(4, hit.damage());
        assertEquals(DamageType.MAGICO, hit.descriptor().damageType());
        assertEquals(DamageSanctity.PROFANO, hit.descriptor().sanctity());
        assertTrue(hit.applyTo(anunciador, context, new DamageServiceImpl()) > 0);
    }

    @Test
    void dadivaDeGaeaHealsAndWardsTheMonsterAndItsMonsterAlliesOnly() {
        MonsterSheet broto = spawn(46, MonstrousAbilitySelection.of(BrotosDeMapinguariAbility.DADIVA_DE_GAEA));
        MonsterSheet monsterAlly = spawn(0, MonstrousAbilitySelection.of(CireneiaAbility.ATRIBUTOS_APRIMORADOS));
        CharacterSheet humanAlly = hero();
        scene.addParticipant(broto, 20, monsters);
        scene.addParticipant(monsterAlly, 15, monsters);
        scene.addParticipant(humanAlly, 10, monsters);
        SceneContext context = from(broto, Map.of(monsterAlly, Range.DISTANCIA_CURTA, humanAlly, Range.DISTANCIA_CURTA));

        scene.refreshMonsterAuras(broto, context);
        assertTrue(monsterAlly.isImmuneToCondition(ConditionType.ASSUSTADO));
        assertTrue(broto.isImmuneToCondition(ConditionType.POSSESSAO));
        assertFalse(humanAlly.isImmuneToCondition(ConditionType.ASSUSTADO));

        List<AuraHit> hits = scene.pulseMonsterAuras(broto, context, 1, null, null).get(0).hits();
        assertEquals(List.of(broto, monsterAlly), hits.stream().map(AuraHit::target).toList());
        assertTrue(hits.stream().allMatch(hit -> hit.heal() > 0));

        // Leaving the aura lifts the ward.
        scene.refreshMonsterAuras(broto, from(broto, Map.of(monsterAlly, Range.DISTANCIA_LONGA)));
        assertFalse(monsterAlly.isImmuneToCondition(ConditionType.ASSUSTADO));
    }

    @Test
    void mestreDasMarionetesDrainsAutocontroleEveryOtherRodadaAndPossessesAtZero() {
        MonsterSheet mestre = spawn(46, MonstrousAbilitySelection.of(EscolhidoDeMacariaAbility.MESTRE_DAS_MARIONETES));
        CharacterSheet hero = hero();
        scene.addParticipant(mestre, 20, monsters);
        scene.addParticipant(hero, 10, party);
        SceneContext context = from(mestre, Map.of(hero, Range.ADJACENTE));

        assertTrue(scene.pulseMonsterAuras(mestre, context, 1, null, null).isEmpty(), "only every 2 Rodadas");
        AuraHit hit = scene.pulseMonsterAuras(mestre, context, 2, null, null).get(0).hits().get(0);
        assertEquals(1, hit.autocontroleLoss());
        assertTrue(hit.possessIfEmptied());
    }

    @Test
    void auraElementalPulsesOnlyWhileItRunsAndInItsElement() {
        MonsterSheet elemental = spawn(12, MonstrousAbilitySelection.of(
                org.aventyrs.core.monster.model.almaelemental.AlmaElementalAbility.SANGUE_ELEMENTAL_DEVIANTE,
                ElementalType.FOGO.name()));
        CharacterSheet hero = hero();
        scene.addParticipant(elemental, 20, monsters);
        scene.addParticipant(hero, 10, party);
        SceneContext context = from(elemental, Map.of(hero, Range.DISTANCIA_CURTA));
        assertTrue(scene.pulseMonsterAuras(elemental, context, 1, DiceRoller.fixed(List.of(3)), null).isEmpty());

        elemental.applyDamage(5);
        new ActiveAbilityServiceImpl().activate(elemental.getCharacter(), elemental, held(elemental, "Aura Elemental"), 1);
        AuraHit hit = scene.pulseMonsterAuras(elemental, context, 1, DiceRoller.fixed(List.of(3)), null).get(0).hits().get(0);
        assertEquals(3, hit.damage());
        assertEquals(ElementalType.FOGO, hit.descriptor().elementalType());
    }
}
