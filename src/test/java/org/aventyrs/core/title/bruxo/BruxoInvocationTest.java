package org.aventyrs.core.title.bruxo;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CriticalDamage;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageSanctity;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.effect.FogoVivo;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.invocation.EnhancedSummonFeat;
import org.aventyrs.core.magic.invocation.InvocationOptions;
import org.aventyrs.core.magic.invocation.InvocationPlan;
import org.aventyrs.core.magic.invocation.NatureInvocationService;
import org.aventyrs.core.magic.invocation.NatureInvocationServiceImpl;
import org.aventyrs.core.magic.invocation.SummonEnhancement;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.monster.summon.NatureSummon;
import org.aventyrs.core.monster.summon.NatureSummonKind;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.EnchantmentWard;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.Regeneration;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.aventyrs.core.util.TranslatableMessages.COMET_REQUIRES_OPEN_SKY;
import static org.aventyrs.core.util.TranslatableMessages.REQUIRED_TITLE_TRAIT_NOT_HELD;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_CHOICE_LOCKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Bruxo's Invocação Maior/Dupla and the Iluminado/Oráculo Abissal clauses, reaching a summoned creature. */
class BruxoInvocationTest {

    private static final List<BruxoSpecialization> BOTH =
            List.of(BruxoSpecialization.ILUMINADO, BruxoSpecialization.ORACULO_ABISSAL);

    private final NatureInvocationService invocations = new NatureInvocationServiceImpl();
    private final Player gm = new Player();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void aBruxoHoldingNothingRelevantChangesNothing() {
        assertEquals(SummonEnhancement.NONE,
                bruxo(List.of()).resolveSummonEnhancement(caster(bruxo(List.of())), InvocationOptions.NONE));
    }

    @Test
    void theEspecializacoesReduceTheSummonsGds() {
        SummonEnhancement enhancement = resolve(BOTH, InvocationOptions.NONE);

        assertEquals(1, enhancement.attackDifficultyReduction());
        assertEquals(1, enhancement.defenseDifficultyReduction());
    }

    @Test
    void iluminadoClausesScaleWithIluminadoHabilidadesOnly() {
        SummonEnhancement enhancement = resolve(BOTH, InvocationOptions.builder().comet(true).openSky(true).build(),
                IluminadoAbility.BENCAO_DO_VENTO_DO_LESTE, IluminadoAbility.BENCAO_DO_MAR_DO_SUL,
                IluminadoAbility.MALDICAO_DO_PANTANO_DO_SUDESTE, IluminadoAbility.MALDICAO_DA_NEVASCA_DO_SUDOESTE,
                OraculoAbissalAbility.MALDICAO_DO_DESERTO_DO_OESTE);

        // Four Habilidades de Iluminado: +2 each where "+1 para cada 2", and the comet +2 apiece.
        assertEquals(2, enhancement.flightBonusRounds());
        assertEquals(4, enhancement.regenerationPerRound());
        assertEquals(4, enhancement.sizeIncrease());
        assertEquals(8, enhancement.cometDamageBonus());
        assertEquals(new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.GELO),
                enhancement.comet().orElseThrow().descriptor());
        assertEquals(DamageSanctity.PROFANO, enhancement.profaneAura().orElseThrow().descriptor().sanctity());
        // One Habilidade de Oráculo Abissal: RDS 2 + 1.
        assertEquals(3, enhancement.damageTakenReduction());
    }

    @Test
    void oraculoClausesScaleWithOraculoHabilidades() {
        SummonEnhancement enhancement = resolve(BOTH, InvocationOptions.NONE,
                OraculoAbissalAbility.MALDICAO_DAS_CHAMAS_DO_NORTE, OraculoAbissalAbility.BENCAO_DOS_VULCOES_DO_NOROESTE,
                OraculoAbissalAbility.BENCAO_DOS_RAIOS_DO_NORDESTE);

        assertTrue(enhancement.fireAttacks());
        assertTrue(enhancement.cataclysm());
        assertEquals(2, enhancement.criticalMarginIncrease());
        assertTrue(enhancement.lightning());
        assertEquals(3, enhancement.lightningDamageBonus());
        assertEquals(2, enhancement.extraActionPoints());
        assertTrue(enhancement.arrivalBurst().isPresent());
        assertNull(enhancement.flightBonusRounds());
    }

    @Test
    void optionsNeedTheirTraits() {
        assertRefused(REQUIRED_TITLE_TRAIT_NOT_HELD, InvocationOptions.builder().extraTimeForLife(true).build());
        assertRefused(REQUIRED_TITLE_TRAIT_NOT_HELD, InvocationOptions.builder().doubled(true).build());
        assertRefused(REQUIRED_TITLE_TRAIT_NOT_HELD, InvocationOptions.builder().comet(true).openSky(true).build());
    }

    @Test
    void invocacaoMaiorBoostsOnlyForcaOrDestreza() {
        Bruxo bruxo = bruxo(BOTH, BruxoAbility.INVOCACAO_MAIOR);
        InvocationOptions vigor = InvocationOptions.builder().boostedAttribute(AttributeDomain.VIGOR).build();

        assertEquals(TITLE_ABILITY_CHOICE_LOCKED, assertThrows(IllegalOperationException.class,
                () -> bruxo.resolveSummonEnhancement(caster(bruxo), vigor)).getMessage());
    }

    @Test
    void theCometNeedsAnOpenSky() {
        Bruxo bruxo = bruxo(BOTH, IluminadoAbility.MALDICAO_DA_NEVASCA_DO_SUDOESTE);
        InvocationOptions covered = InvocationOptions.builder().comet(true).build();

        assertEquals(COMET_REQUIRES_OPEN_SKY, assertThrows(IllegalOperationException.class,
                () -> bruxo.resolveSummonEnhancement(caster(bruxo), covered)).getMessage());
    }

    @Test
    void enhancingAPlanReportsTheSurchargeAndDoublesWithInvocacaoDupla() {
        CharacterSheet caster = caster(bruxo(BOTH, BruxoAbility.INVOCACAO_MAIOR, BruxoAbility.INVOCACAO_DUPLA));
        InvocationPlan plan = invocations.planAliado(caster, false);
        InvocationOptions options = InvocationOptions.builder()
                .extraTimeForLife(true).boostedAttribute(AttributeDomain.STRENGTH).doubled(true).build();

        InvocationPlan enhanced = invocations.enhance(plan, caster, options);

        assertEquals(2, enhanced.creatures().size());
        assertEquals(1, enhanced.rounds()); // 3 Rodadas halved, floored.
        assertEquals(3, enhanced.extraActionPoints());
        assertEquals(2, enhanced.extraDeterminationCost());
        assertEquals(1, enhanced.concentrationUpkeepMultiplier()); // Aliado da Natureza is not concentrated.
        enhanced.creatures().forEach(creature -> assertEquals(enhanced.enhancement(), creature.getEnhancement()));
    }

    @Test
    void doublingAConcentratedInvocationDoublesItsUpkeep() {
        CharacterSheet caster = caster(bruxo(BOTH, BruxoAbility.INVOCACAO_MAIOR, BruxoAbility.INVOCACAO_DUPLA));

        InvocationPlan enhanced = invocations.enhance(invocations.planAnciente(caster), caster,
                InvocationOptions.builder().doubled(true).build());

        assertEquals(2, enhanced.concentrationUpkeepMultiplier());
    }

    @Test
    void optionsWithNoTituloToHonourThemAreRefused() {
        CharacterSheet plain = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), gm);

        assertEquals(REQUIRED_TITLE_TRAIT_NOT_HELD, assertThrows(IllegalOperationException.class,
                () -> invocations.enhance(invocations.planAliado(plain, false), plain,
                        InvocationOptions.builder().doubled(true).build())).getMessage());
    }

    @Test
    void theSpawnedCreatureCarriesItsStatBlockChanges() {
        NatureSummon plain = NatureSummon.of(NatureSummonKind.ALIADO_DA_NATUREZA);
        SummonEnhancement enhancement = SummonEnhancement.builder()
                .lifeMultiplierBonus(SummonEnhancement.LIFE_MULTIPLIER_BONUS).boostedAttribute(AttributeDomain.STRENGTH)
                .invocacaoMaior(true).sizeIncrease(2).lightning(true).regenerationPerRound(3).build();
        NatureSummon enhanced = plain.withEnhancement(enhancement);
        MonsterSheet sheet = enhanced.spawn(gm);

        assertEquals(plain.getLifeMultiplier() + 3, enhanced.getLifeMultiplier());
        assertEquals(plain.getAttributeBases().get(AttributeDomain.STRENGTH) + 4,
                enhanced.getAttributeBases().get(AttributeDomain.STRENGTH));
        assertEquals(plain.getSizeCategory().shift(2), enhanced.getSizeCategory());
        assertEquals(plain.getActionPoints() + 2, enhanced.getActionPoints());
        assertTrue(sheet.getRunningEffects().stream().anyMatch(EnchantmentWard.class::isInstance));
        assertTrue(sheet.getRunningEffects().stream().anyMatch(Regeneration.class::isInstance));
        assertTrue(sheet.getCharacter().getFeats().stream().anyMatch(EnhancedSummonFeat.class::isInstance));
    }

    @Test
    void oneInvocacaoMaiorEffectIsNoWard() {
        MonsterSheet sheet = NatureSummon.of(NatureSummonKind.ALIADO_DA_NATUREZA)
                .withEnhancement(SummonEnhancement.builder().lifeMultiplierBonus(3).invocacaoMaior(true).build())
                .spawn(gm);

        assertFalse(sheet.getRunningEffects().stream().anyMatch(EnchantmentWard.class::isInstance));
    }

    @Test
    void theRollHooksReadTheEnhancement() {
        EnhancedSummonFeat feat = new EnhancedSummonFeat(SummonEnhancement.builder()
                .attackDifficultyReduction(1).defenseDifficultyReduction(1).damageTakenReduction(3)
                .cataclysm(true).criticalMarginIncrease(2).fireAttacks(true).build());

        assertEquals(1, feat.resolveDifficultyReduction(SkillType.ATAQUE_CORPO_A_CORPO, null));
        assertEquals(1, feat.resolveDifficultyReduction(SkillType.ESQUIVA_E_APARAR, null));
        assertEquals(0, feat.resolveDifficultyReduction(SkillType.ATTENTION, null));
        assertEquals(3, feat.resolveDamageTakenReduction(null));
        assertEquals(2, feat.resolveCriticalMarginIncrease(SkillType.ATAQUE_CORPO_A_CORPO, null, null));
        assertEquals(CriticalDamage.ofDice(1),
                feat.resolveCriticalDamage(SkillType.ATAQUE_CORPO_A_CORPO, null, null, null, null, null));
        assertEquals(DefenseType.MAGIC,
                feat.resolveTargetDefenseOverride(SkillType.ATAQUE_CORPO_A_CORPO, null, null, null));
        assertEquals(new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.FOGO),
                feat.resolveDamageRetype(null, SkillType.ATAQUE_CORPO_A_CORPO, null));
    }

    @Test
    void raiosTurnFireIntoSagradoAndAddDamage() {
        EnhancedSummonFeat feat = new EnhancedSummonFeat(SummonEnhancement.builder()
                .fireAttacks(true).lightning(true).lightningDamageBonus(2).build());

        assertEquals(new DamageDescriptor(DamageType.MAGICO, null, DamageSanctity.SAGRADO),
                feat.resolveDamageRetype(null, SkillType.ATAQUE_CORPO_A_CORPO, null));
        assertEquals(2, feat.resolveDamageBonus(SkillType.ATAQUE_CORPO_A_CORPO, null, null, null)
                .orElseThrow().getValue());
    }

    @Test
    void chamasDoNorteAddsFogoVivoToEveryAttack() {
        NatureSummon summon = NatureSummon.of(NatureSummonKind.ALIADO_DA_NATUREZA)
                .withEnhancement(SummonEnhancement.builder().fireAttacks(true).build());

        assertTrue(summon.resolveAttackEffectChains(summon.spawn(gm)).stream().anyMatch(FogoVivo.class::isInstance));
    }

    @Test
    void anAllyAdjacentToAnInvocacaoMaiorGetsRmAgainstMagicalDamage() {
        DamageService damage = new DamageServiceImpl();
        MonsterSheet invocation = NatureSummon.of(NatureSummonKind.ALIADO_DA_NATUREZA)
                .withEnhancement(SummonEnhancement.builder().lifeMultiplierBonus(3).invocacaoMaior(true).build())
                .spawn(gm);
        CharacterSheet ally = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), gm);
        SceneContext adjacent = new SceneContext(List.of(invocation), List.of(), Map.of(invocation, Range.ADJACENTE));
        SceneContext apart = new SceneContext(List.of(invocation), List.of(), Map.of(invocation, Range.DISTANCIA_CURTA));

        int beside = damage.calculateFinalDamage(ally, adjacent, DamageType.MAGICO, null, 10, false);
        int away = damage.calculateFinalDamage(ally, apart, DamageType.MAGICO, null, 10, false);

        assertEquals(away - DamageService.DEFAULT_DAMAGE_REDUCTION, beside);
    }

    @Test
    void theEnhancementRoundTripsThroughRestore() {
        SummonEnhancement enhancement = SummonEnhancement.builder().sizeIncrease(3).fireAttacks(true).build();

        NatureSummon restored = NatureSummon.restore(NatureSummonKind.ALIADO_DA_NATUREZA.name(), 4, List.of(), enhancement);

        assertEquals(enhancement, restored.getEnhancement());
        assertEquals(4, restored.getConjuradorManaGraduation());
    }

    private SummonEnhancement resolve(final List<BruxoSpecialization> specializations, final InvocationOptions options,
                                      final AventyrTitleAbility... abilities) {
        Bruxo bruxo = bruxo(specializations, abilities);
        return bruxo.resolveSummonEnhancement(caster(bruxo), options);
    }

    private void assertRefused(final String message, final InvocationOptions options) {
        Bruxo bruxo = bruxo(BOTH);
        assertEquals(message, assertThrows(IllegalOperationException.class,
                () -> bruxo.resolveSummonEnhancement(caster(bruxo), options)).getMessage());
    }

    private static Bruxo bruxo(final List<BruxoSpecialization> specializations, final AventyrTitleAbility... abilities) {
        return new Bruxo(specializations, List.of(abilities));
    }

    private CharacterSheet caster(final Bruxo bruxo) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        character.grantTitle(bruxo, TitleSlot.PRIMARY);
        return CharacterSheet.of(character, gm);
    }
}
