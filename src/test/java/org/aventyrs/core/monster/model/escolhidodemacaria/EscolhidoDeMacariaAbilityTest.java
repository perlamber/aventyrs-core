package org.aventyrs.core.monster.model.escolhidodemacaria;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageSanctity;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.magic.invocation.UndeadInvocationService;
import org.aventyrs.core.magic.invocation.UndeadInvocationServiceImpl;
import org.aventyrs.core.monster.MonsterCategory;
import org.aventyrs.core.monster.MonsterRules;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.monster.MonstrousAbilitySelection;
import org.aventyrs.core.monster.model.MonsterSummoning;
import org.aventyrs.core.monster.model.MonstrousActiveAbility;
import org.aventyrs.core.monster.summon.CarrascoMorimbundo;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneSummon;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.character.services.ActiveAbilityServiceImpl;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.effect.monstrous.MarcaDaEscuridao;
import org.aventyrs.core.effect.monstrous.SangriaProfana;
import org.aventyrs.core.effect.monstrous.Sanguenomancia;
import org.aventyrs.core.monster.MonstrousAbilityGrant;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.aventyrs.core.monster.model.MonsterTestKit.blueprint;
import static org.aventyrs.core.monster.model.MonsterTestKit.held;
import static org.aventyrs.core.monster.model.MonsterTestKit.spawn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Modelo Escolhido de Macária (core 0.1.6.26). */
class EscolhidoDeMacariaAbilityTest {

    private final UndeadInvocationService undead = new UndeadInvocationServiceImpl();
    private final Player gm = new Player();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static MonstrousAbilitySelection of(final EscolhidoDeMacariaAbility ability) {
        return MonstrousAbilitySelection.of(ability);
    }

    private static int damage(final CombatantSheet sheet, final DamageDescriptor descriptor) {
        return new DamageServiceImpl().calculateFinalDamage(sheet, null, descriptor, null, 10, false);
    }

    @Test
    void everyHabilidadeMakesAMortoVivoWithTheAnatomy() {
        for (EscolhidoDeMacariaAbility ability : EscolhidoDeMacariaAbility.values()) {
            MonsterSheet monster = spawn(61, of(ability));
            assertTrue(monster.hasUndeadAnatomy(), ability.name());
            assertTrue(monster.isUndead(), ability.name());
        }
    }

    @Test
    void theCarrascoIsVoraciousAndSturdier() {
        MonsterSheet presa = spawn(0, of(EscolhidoDeMacariaAbility.CARRASCO_MORIMBUNDO));
        MonsterSheet deviante = spawn(12, of(EscolhidoDeMacariaAbility.CARRASCO_MORIMBUNDO));

        assertEquals(3, presa.getCharacter().getEffectiveAttributeTotal(AttributeDomain.VIGOR));
        assertEquals(1, presa.getTotalLifeSteal());
        assertEquals(3, deviante.getTotalLifeSteal());
        held(presa, "Instinto Predatório Mortis");
    }

    @Test
    void theRenascidoMonstruosoIsStrongerAndTougherOnlyForThreeRodadasAfterAMacariaActivation() {
        MonsterSheet renascido = spawn(0, of(EscolhidoDeMacariaAbility.RENASCIDO));
        assertEquals(1, renascido.getAttributeTotal(AttributeDomain.STRENGTH));
        int unbuffed = damage(renascido, new DamageDescriptor(DamageType.FISICO));

        new ActiveAbilityServiceImpl().activate(renascido.getCharacter(), renascido,
                held(renascido, "Sanguenomancia Inata"), 1);
        assertEquals(3, renascido.getAttributeTotal(AttributeDomain.STRENGTH));
        assertEquals(3, renascido.getAttributeTotal(AttributeDomain.INSTINCT));
        assertTrue(damage(renascido, new DamageDescriptor(DamageType.FISICO)) < unbuffed, "RDS");
        assertEquals(2, renascido.getTotalLifeSteal());

        // A Título activation renews rather than stacks.
        MonstrousAbilityGrant.onTitleActivation(renascido);
        assertEquals(3, renascido.getAttributeTotal(AttributeDomain.STRENGTH));
        assertEquals(2, renascido.getTotalLifeSteal());
    }

    @Test
    void sanguenomanciasHitTakesVigorDownToOneAndIsRefusedByTheUndead() {
        MonsterSheet predador = spawn(26, of(EscolhidoDeMacariaAbility.RENASCIDO));
        new ActiveAbilityServiceImpl().activate(predador.getCharacter(), predador,
                held(predador, "Sanguenomancia Inata"), 1);
        assertTrue(MonstrousAbilityGrant.attackEffectChains(predador, SkillType.ATAQUE_CORPO_A_CORPO, null).stream()
                .anyMatch(Sanguenomancia.class::isInstance));

        MonsterSheet living = spawn(0, MonstrousAbilitySelection.of(
                org.aventyrs.core.monster.model.cireneia.CireneiaAbility.ATRIBUTOS_APRIMORADOS));
        int vigor = living.getAttributeTotal(AttributeDomain.VIGOR);
        new Sanguenomancia(2).applyTo(living);
        assertEquals(Math.max(1, vigor - 2), living.getAttributeTotal(AttributeDomain.VIGOR));

        int undeadVigor = predador.getAttributeTotal(AttributeDomain.VIGOR);
        new Sanguenomancia(2).applyTo(predador);
        assertEquals(undeadVigor, predador.getAttributeTotal(AttributeDomain.VIGOR), "a Doença; the dead are immune");
    }

    @Test
    void garrasEspectraisCarrySangriaProfanaAndSangramentoForThreeRodadas() {
        MonsterSheet espectral = spawn(12, of(EscolhidoDeMacariaAbility.ESPECTRAL));
        new ActiveAbilityServiceImpl().activate(espectral.getCharacter(), espectral, held(espectral, "Garras Espectrais"), 1);
        assertEquals(3, ((MonstrousActiveAbility) held(espectral, "Garras Espectrais")).getDurationInRounds());
        assertTrue(MonstrousAbilityGrant.attackEffectChains(espectral, SkillType.ATAQUE_CORPO_A_CORPO, null).stream()
                .anyMatch(SangriaProfana.class::isInstance));
        assertEquals(List.of(CriticalEffectType.SANGRAMENTO),
                MonstrousAbilityGrant.additionalCriticalEffects(espectral, SkillType.ATAQUE_CORPO_A_CORPO, null));
    }

    @Test
    void marcaDaEscuridaoCursesQueuesThreeD6AndStealsTheTargetsVigor() {
        MonsterSheet anunciador = spawn(26, of(EscolhidoDeMacariaAbility.ANUNCIADOR_DA_ESCURIDAO));
        MonstrousActiveAbility marca = (MonstrousActiveAbility) held(anunciador, "Marca da Escuridão");
        assertEquals(4, marca.getDeterminationPointCost());
        assertEquals(2, marca.getCooldownRounds());
        new ActiveAbilityServiceImpl().activate(anunciador.getCharacter(), anunciador, marca, 1);
        assertTrue(MonstrousAbilityGrant.attackEffectChains(anunciador, SkillType.ATAQUE_CORPO_A_CORPO, null).stream()
                .anyMatch(MarcaDaEscuridao.class::isInstance));

        MonsterSheet living = spawn(0, MonstrousAbilitySelection.of(
                org.aventyrs.core.monster.model.cireneia.CireneiaAbility.ATRIBUTOS_APRIMORADOS));
        int lifeSteal = anunciador.getTotalLifeSteal();
        new MarcaDaEscuridao(anunciador).applyTo(living);
        assertTrue(living.hasCondition(org.aventyrs.core.sheet.ConditionType.AMALDICOADO, null));
        assertEquals(lifeSteal + living.getAttributeTotal(AttributeDomain.VIGOR), anunciador.getTotalLifeSteal());
    }

    @Test
    void titerePossessesForTwoRodadas() {
        MonsterSheet mestre = spawn(46, of(EscolhidoDeMacariaAbility.MESTRE_DAS_MARIONETES));
        new ActiveAbilityServiceImpl().activate(mestre.getCharacter(), mestre, held(mestre, "Títere"), 1);
        MonsterSheet living = spawn(0, MonstrousAbilitySelection.of(
                org.aventyrs.core.monster.model.cireneia.CireneiaAbility.ATRIBUTOS_APRIMORADOS));
        MonstrousAbilityGrant.attackEffectChains(mestre, SkillType.ATAQUE_A_DISTANCIA, null).forEach(c -> c.applyTo(living));
        assertTrue(living.getHeldConditions().stream().anyMatch(c ->
                c.getType() == org.aventyrs.core.sheet.ConditionType.POSSESSAO && c.getRemainingRounds() == 2));
    }

    @Test
    void anEspectralIsIncorporealAndFliesFromPredador() {
        MonsterSheet deviante = spawn(12, of(EscolhidoDeMacariaAbility.ESPECTRAL));
        MonsterSheet predador = spawn(26, of(EscolhidoDeMacariaAbility.ESPECTRAL));

        assertEquals(0, damage(deviante, new DamageDescriptor(DamageType.FISICO)));
        assertEquals(10, damage(deviante, new DamageDescriptor(DamageType.PRIMORDIAL)));
        assertFalse(deviante.isFlying());
        assertTrue(predador.isFlying());
    }

    @Test
    void anAbominacaoFilacteriaHalvesAllButSagradoPrimordialAndFogo() {
        MonsterSheet lich = spawn(61, of(EscolhidoDeMacariaAbility.FILACTERIA));
        int physical = damage(lich, new DamageDescriptor(DamageType.FISICO));
        int sacred = damage(lich, new DamageDescriptor(DamageType.FISICO, null, DamageSanctity.SAGRADO));
        assertTrue(physical < sacred, physical + " should be halved, " + sacred + " not");
    }

    @Test
    void theAnunciadorHasRaRdAndRm() {
        MonsterSheet herald = spawn(26, of(EscolhidoDeMacariaAbility.ANUNCIADOR_DA_ESCURIDAO));
        assertTrue(damage(herald, new DamageDescriptor(DamageType.FISICO)) < 10);
        held(herald, "Marca da Escuridão");
    }

    @Test
    void procissaoOffersItsCarrascosByCategory() {
        MonsterSheet deviante = spawn(12, of(EscolhidoDeMacariaAbility.SENHOR_DOS_MORTOS));
        MonsterSheet apex = spawn(46, of(EscolhidoDeMacariaAbility.SENHOR_DOS_MORTOS));

        List<MonsterSummoning> presa = ((MonstrousActiveAbility) held(deviante, "Procissão dos Desencarnados"))
                .getSummonOptions();
        List<MonsterSummoning> apexOptions = ((MonstrousActiveAbility) held(apex, "Procissão dos Desencarnados"))
                .getSummonOptions();

        assertEquals(List.of(new MonsterSummoning(MonsterCategory.PRESA, 1, 5, true)), presa);
        assertEquals(2, apexOptions.size());
        assertEquals(MonsterCategory.PREDADOR, apexOptions.get(1).category());
    }

    @Test
    void procissaoPlacesStockCarrascosUpToItsCarisma() {
        MonsterSheet lord = blueprint(12, of(EscolhidoDeMacariaAbility.SENHOR_DOS_MORTOS))
                .attributeBase(AttributeDomain.CHARISMA, 2).build().spawn(gm);
        Scene scene = new Scene();
        scene.addParticipant(lord, 10, UUID.randomUUID());
        MonsterSummoning one = new MonsterSummoning(MonsterCategory.PRESA, 1, 5, true);

        SceneSummon carrasco = undead.invokeCarrascos(scene, lord, one, gm).get(0);
        assertTrue(carrasco.isSustained());
        assertTrue(carrasco.getSummon().hasUndeadAnatomy());
        undead.invokeCarrascos(scene, lord, one, gm);
        assertEquals(org.aventyrs.core.util.TranslatableMessages.SUMMON_LIMIT_REACHED,
                org.junit.jupiter.api.Assertions.assertThrows(org.aventyrs.core.sheet.IllegalOperationException.class,
                        () -> undead.invokeCarrascos(scene, lord, one, gm)).getMessage());
    }

    @Test
    void theStockCarrascoIsALegalBlueprintAtEveryCategory() {
        for (MonsterCategory category : MonsterCategory.values()) {
            assertEquals(List.of(), MonsterRules.validate(CarrascoMorimbundo.stock(category)), category.name());
            assertEquals(category, CarrascoMorimbundo.stock(category).getCategory());
        }
    }

    @Test
    void theCuspidorSpitsTwoDeviantesOnOddRodadasUntilCombatEnds() {
        MonsterSheet spitter = spawn(46, of(EscolhidoDeMacariaAbility.CUSPIDOR_DE_PRAGAS));
        MonsterSheet hero = spawn(0, of(EscolhidoDeMacariaAbility.RENASCIDO));
        Scene scene = new Scene();
        scene.addParticipant(spitter, 20, UUID.randomUUID());
        scene.addParticipant(hero, 10, UUID.randomUUID());
        scene.startCombat();
        scene.next();

        undead.startPlagueSpitting(scene, spitter, gm);
        assertEquals(2, scene.getSummonsOf(spitter).size(), "the first Rodada is odd");

        toNextRodada(scene);
        assertEquals(2, scene.getSummonsOf(spitter).size(), "none on an even one");
        toNextRodada(scene);
        assertEquals(4, scene.getSummonsOf(spitter).size());

        scene.endCombat();
        assertTrue(scene.getSummonsOf(spitter).isEmpty(), "destroyed when the combat ends");
        assertTrue(scene.getSummonSpawners().isEmpty());
    }

    @Test
    void titereMakesItsMasterImmuneForARodada() {
        MonsterSheet puppeteer = spawn(46, of(EscolhidoDeMacariaAbility.MESTRE_DAS_MARIONETES));
        new org.aventyrs.core.character.services.ActiveAbilityServiceImpl().activate(puppeteer.getCharacter(), puppeteer, held(puppeteer, "Títere"), 1);
        assertEquals(0, damage(puppeteer, new DamageDescriptor(DamageType.PRIMORDIAL)));
    }

    private static void toNextRodada(final Scene scene) {
        int round = scene.getCurrentRound();
        while (scene.getCurrentRound() == round) {
            scene.next();
        }
    }
}
