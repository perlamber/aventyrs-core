package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.Deity;
import org.aventyrs.core.character.DevotionTier;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.ActiveAbilityServiceImpl;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DevotionService;
import org.aventyrs.core.character.services.DevotionServiceImpl;
import org.aventyrs.core.character.services.MovementServiceImpl;
import org.aventyrs.core.effect.EffectChainServiceImpl;
import org.aventyrs.core.effect.Excomungar;
import org.aventyrs.core.effect.ExplosaoCataclismica;
import org.aventyrs.core.effect.RemoverAflicao;
import org.aventyrs.core.effect.ToqueSombrio;
import org.aventyrs.core.item.ShieldItem;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.catalog.VidaSpell;
import org.aventyrs.core.magic.catalog.VooSpell;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.scene.InitiativePosition;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.HealingSource;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillTraitCatalog;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.attention.AttentionCompetencyAbility;
import org.aventyrs.core.skill.empatiaselvagem.EmpatiaSelvagemCompetencyAbility;
import org.aventyrs.core.util.TranslatableMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Talentos de Devoção (core 0.0.86), each rung applying while the holder's tier reaches it. Table rulings
 * (2026-10-01): the player picks the tier, the Narrador raises or lowers it; a rung's pick is made when first reached
 * and kept while lowered; Resistência às Correntes +2 is Resoluto's margin.
 */
class DevotoFeatTest {

    private final DevotionService devotion = new DevotionServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character devotee(final Deity deity, final DevotionTier tier, final DevotoFeat talento) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .equipment(new ArrayList<>())
                .deity(deity)
                .devotionTier(tier)
                .build();
        character.grantFeat(talento);
        return character;
    }

    private static CharacterSheet sheet(final Character character) {
        return CharacterSheet.of(character, new Player());
    }

    // ---------- the tier and its picks ----------

    @Test
    void aRungAppliesOnlyWhileTheTierReachesIt() {
        Character acolyte = devotee(Deity.LUZ_PRIMORDIAL, DevotionTier.FIEL, DevotoFeat.ACOLITO_DA_LUZ_PRIMORDIAL);

        assertEquals(0, DevotoFeat.ACOLITO_DA_LUZ_PRIMORDIAL.resolveDefenseBonus(DefenseType.PHYSICAL, acolyte));
        devotion.setTier(acolyte, DevotionTier.FUNDAMENTALISTA);
        assertEquals(2, DevotoFeat.ACOLITO_DA_LUZ_PRIMORDIAL.resolveDefenseBonus(DefenseType.PHYSICAL, acolyte));
        devotion.setTier(acolyte, null);
        assertEquals(0, DevotoFeat.ACOLITO_DA_LUZ_PRIMORDIAL
                .resolveEffectChainResistanceIncrease(acolyte, InitiativePosition.UNKNOWN));
    }

    /** "Pré-requisito: Devoto de X" — and Cultista Umbral's any Senhor Umbral. */
    @Test
    void theDivindadeIsThePrerequisite() {
        Character gaean = CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>())
                .deity(Deity.GAEA).build();
        Character cultist = CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>())
                .deity(Deity.TECE_MORTES).build();

        assertTrue(DevotoFeat.ESCOLHIDO_DE_GAEA.isEligible(gaean));
        assertFalse(DevotoFeat.ESCOLHIDO_DE_GAEA.isEligible(cultist));
        assertTrue(DevotoFeat.CULTISTA_UMBRAL.isEligible(cultist));
        assertFalse(DevotoFeat.CULTISTA_UMBRAL.isEligible(gaean));
        assertTrue(FeatCatalog.all().contains(DevotoFeat.CULTISTA_UMBRAL), "bought like any Talento");
    }

    /** A rung's pick is owed when first reached, recorded, and kept — silent — while the tier is lowered. */
    @Test
    void aRungPickIsOwedRecordedAndKeptWhileTheTierIsLowered() {
        Character sylph = devotee(Deity.SYLPH, DevotionTier.ADEPTO, DevotoFeat.ASTUCIA_DE_SYLPH);

        List<DevotionService.OwedPicks> owed = devotion.owedPicks(sylph);
        assertEquals(1, owed.size());
        assertEquals(DevotionTier.ADEPTO, owed.get(0).rung());
        assertEquals(List.copyOf(SkillTraitCatalog.competencyAbilitiesOf(SkillType.ATTENTION)),
                owed.get(0).choices().get(0).options());

        devotion.recordPicks(sylph, DevotoFeat.ASTUCIA_DE_SYLPH, DevotionTier.ADEPTO,
                List.of(AttentionCompetencyAbility.PERCEPCAO_DE_FOXM));
        assertEquals(List.of(), devotion.owedPicks(sylph));
        assertTrue(DevotoFeat.ASTUCIA_DE_SYLPH.getGrantedSkillTraits(sylph)
                .contains(AttentionCompetencyAbility.PERCEPCAO_DE_FOXM));

        devotion.setTier(sylph, null);
        assertEquals(List.of(), DevotoFeat.ASTUCIA_DE_SYLPH.getGrantedSkillTraits(sylph));
        devotion.setTier(sylph, DevotionTier.FIEL);
        assertTrue(DevotoFeat.ASTUCIA_DE_SYLPH.getGrantedSkillTraits(sylph)
                .contains(AttentionCompetencyAbility.PERCEPCAO_DE_FOXM), "the same pick comes back");
        assertEquals(DevotionTier.FIEL, devotion.owedPicks(sylph).get(0).rung(), "Fiel's own pick is owed now");
    }

    @Test
    void aPickNotAmongTheOptionsOrForARungNotReachedIsRefused() {
        Character sylph = devotee(Deity.SYLPH, DevotionTier.ADEPTO, DevotoFeat.ASTUCIA_DE_SYLPH);

        IllegalOperationException wrong = assertThrows(IllegalOperationException.class, () -> devotion.recordPicks(
                sylph, DevotoFeat.ASTUCIA_DE_SYLPH, DevotionTier.ADEPTO,
                List.of(EmpatiaSelvagemCompetencyAbility.ACADEMICO_SELVAGEM)));
        assertEquals(TranslatableMessages.INVALID_DEVOTION_PICK, wrong.getMessage());
        IllegalOperationException notReached = assertThrows(IllegalOperationException.class, () ->
                devotion.recordPicks(sylph, DevotoFeat.ASTUCIA_DE_SYLPH, DevotionTier.FIEL, List.of()));
        assertEquals(TranslatableMessages.DEVOTION_PICK_NOT_OWED, notReached.getMessage());
    }

    // ---------- each Talento ----------

    /** Adepto's +2 is Resoluto's: the margin a Corrente must clear rises by 2. */
    @Test
    void acolitoResistsCorrentesAndChainsRemoverAflicaoAndExcomungar() {
        Character acolyte = devotee(Deity.LUZ_PRIMORDIAL, DevotionTier.FUNDAMENTALISTA,
                DevotoFeat.ACOLITO_DA_LUZ_PRIMORDIAL);
        CharacterSheet sheet = sheet(acolyte);
        CharacterSheet ally = sheet(CharacterFixture.blank(CharacterFixture.BLANK).build());

        assertEquals(new EffectChainServiceImpl().getRequiredMargin(acolyte) + DevotoFeat.EFFECT_CHAIN_RESISTANCE,
                new EffectChainServiceImpl().getRequiredMargin(null, InitiativePosition.UNKNOWN, sheet,
                        InitiativePosition.UNKNOWN));
        assertTrue(DevotoFeat.ACOLITO_DA_LUZ_PRIMORDIAL.resolveSpellEffectChains(VidaSpell.REVIGORAR, sheet, ally,
                false).stream().anyMatch(RemoverAflicao.class::isInstance));
        assertTrue(DevotoFeat.ACOLITO_DA_LUZ_PRIMORDIAL.resolveSpellEffectChains(VidaSpell.REVIGORAR, sheet, ally,
                true).stream().anyMatch(Excomungar.class::isInstance), "Vida is a Magia divina");
        assertEquals(List.of(), DevotoFeat.ACOLITO_DA_LUZ_PRIMORDIAL.resolveSpellEffectChains(VooSpell.QUEDA_LENTA,
                sheet, ally, true));
    }

    @Test
    void escuridaoProfundaTouchesWithShadowStealsLifeAndHealsLess() {
        Character shade = devotee(Deity.ESCURIDAO_PROFUNDA, DevotionTier.FUNDAMENTALISTA,
                DevotoFeat.ADEPTO_DA_ESCURIDAO_PROFUNDA);
        CharacterSheet sheet = sheet(shade);
        sheet.applyDamage(10);

        assertTrue(DevotoFeat.ADEPTO_DA_ESCURIDAO_PROFUNDA.resolveEffectChains(shade, SkillType.ATAQUE_CORPO_A_CORPO,
                null).stream().anyMatch(ToqueSombrio.class::isInstance));
        assertEquals(2, new org.aventyrs.core.character.services.LifeStealServiceImpl().getTotalLifeSteal(shade, sheet));
        sheet.heal(5);
        assertEquals(8, sheet.getDamageTaken(), "a heal of 5 recovers 2");
        sheet.healFromLifeSteal(2);
        assertEquals(6, sheet.getDamageTaken(), "Roubo de Vida is not reduced");
    }

    @Test
    void armamentoVulcanoRaisesDanoBaseAndTheChosenArmamentsDefesas() {
        Character smith = devotee(Deity.VULCANO, DevotionTier.FUNDAMENTALISTA, DevotoFeat.ARMAMENTO_VULCANO);
        devotion.recordPicks(smith, DevotoFeat.ARMAMENTO_VULCANO, DevotionTier.FIEL,
                List.of(DevotoFeat.VulcanArmament.ESCUDOS));

        assertEquals(0, DevotoFeat.ARMAMENTO_VULCANO.resolveDefenseBonus(DefenseType.PHYSICAL, smith));
        smith.equip(ShieldItem.ESCUDO_MEDIO);
        assertEquals(1, DevotoFeat.ARMAMENTO_VULCANO.resolveDefenseBonus(DefenseType.PHYSICAL, smith), "+1 in total");
        assertEquals(1, DevotoFeat.ARMAMENTO_VULCANO.resolveCriticalMarginIncrease(SkillType.ATAQUE_CORPO_A_CORPO,
                null, smith));
        assertEquals(1, DevotoFeat.ARMAMENTO_VULCANO.resolveCriticalResistance(smith, null));
    }

    @Test
    void surtEldurResistsMonstersAndStealsFromOtherFaiths() {
        Character zealot = devotee(Deity.SURT_ELDUR, DevotionTier.FUNDAMENTALISTA, DevotoFeat.BENCAO_DE_SURT_ELDUR);
        CharacterSheet sheet = sheet(zealot);
        CharacterSheet heretic = sheet(CharacterFixture.blank(CharacterFixture.BLANK).deity(Deity.TESLA).build());
        CharacterSheet faithful = sheet(CharacterFixture.blank(CharacterFixture.BLANK).deity(Deity.LUZ_PRIMORDIAL)
                .build());

        assertEquals(DamageService.DEFAULT_DAMAGE_REDUCTION, DevotoFeat.BENCAO_DE_SURT_ELDUR.resolveMagicReduction(zealot));
        assertEquals(1, DevotoFeat.BENCAO_DE_SURT_ELDUR.resolveDamageTakenReductionAgainst(zealot, sheet,
                GenericMonster.CAPANGA.spawn(new Player())));
        assertEquals(1, DevotoFeat.BENCAO_DE_SURT_ELDUR.resolveDamageTakenReductionAgainst(zealot, sheet, heretic));
        assertEquals(0, DevotoFeat.BENCAO_DE_SURT_ELDUR.resolveDamageTakenReductionAgainst(zealot, sheet, faithful));
        assertEquals(1, DevotoFeat.BENCAO_DE_SURT_ELDUR.resolveTargetedLifeSteal(zealot, sheet, heretic));
        assertEquals(0, DevotoFeat.BENCAO_DE_SURT_ELDUR.resolveTargetedLifeSteal(zealot, sheet, faithful));
    }

    @Test
    void caminharDeEponaWidensTheLandStrideAndIgnoresTerrenoDificil() {
        Character walker = devotee(Deity.EPONA, DevotionTier.FIEL, DevotoFeat.CAMINHAR_DE_EPONA);
        Character plain = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet sheet = sheet(walker);

        assertEquals(new MovementServiceImpl().getMovementBase(sheet(plain)) + 2,
                new MovementServiceImpl().getMovementBase(sheet));
        assertTrue(DevotoFeat.CAMINHAR_DE_EPONA.ignoresDifficultTerrain(walker, sheet));
        assertEquals(0, DevotoFeat.CAMINHAR_DE_EPONA.resolveDefenseBonus(DefenseType.PHYSICAL, walker, null, sheet));
        sheet.consumeMovementThisRound();
        assertEquals(2, DevotoFeat.CAMINHAR_DE_EPONA.resolveDefenseBonus(DefenseType.PHYSICAL, walker, null, sheet),
                "while moving");
    }

    @Test
    void escolhidoDeGaeaFavoursItsPericiasAndEasesThem() {
        Character chosen = devotee(Deity.GAEA, DevotionTier.FUNDAMENTALISTA, DevotoFeat.ESCOLHIDO_DE_GAEA);

        assertEquals(2, DevotoFeat.ESCOLHIDO_DE_GAEA.resolveSkillRollBonus(SkillType.EMPATIA_SELVAGEM, null, null,
                chosen));
        assertEquals(0, DevotoFeat.ESCOLHIDO_DE_GAEA.resolveSkillRollBonus(SkillType.CONHECIMENTOS, null, null,
                chosen), "Conhecimentos only for Natureza");
        assertEquals(1, DevotoFeat.ESCOLHIDO_DE_GAEA.resolveDifficultyReduction(SkillType.MEDICINA_E_CURA, chosen,
                null, null));
        assertEquals(2, devotion.owedPicks(chosen).get(0).choices().get(0).picks(), "Fiel owes two Competências");
    }

    @Test
    void impactoYmirianoPicksItsBenefitsAndItsWindowFreezesTheAttack() {
        Character giant = devotee(Deity.YMIR, DevotionTier.FUNDAMENTALISTA, DevotoFeat.IMPACTO_YMIRIANO);
        devotion.recordPicks(giant, DevotoFeat.IMPACTO_YMIRIANO, DevotionTier.ADEPTO,
                List.of(DevotoFeat.YmirianImpact.DANOS));
        devotion.recordPicks(giant, DevotoFeat.IMPACTO_YMIRIANO, DevotionTier.FUNDAMENTALISTA,
                List.of(AttributeDomain.STRENGTH));
        CharacterSheet sheet = sheet(giant);
        int strength = giant.getAttributes().getAttribute(AttributeDomain.STRENGTH).getTotal();

        assertEquals(1, DevotoFeat.IMPACTO_YMIRIANO.resolveDamageBonus(SkillType.ATAQUE_CORPO_A_CORPO, null, null,
                giant).orElseThrow().getValue());
        assertEquals(strength + 1, giant.getEffectiveAttributeTotal(AttributeDomain.STRENGTH));
        assertNull(DevotoFeat.IMPACTO_YMIRIANO.resolveDamageRetype(giant, SkillType.ATAQUE_CORPO_A_CORPO, null, sheet));

        new ActiveAbilityServiceImpl().activate(giant, sheet, DevotoFeat.ImpactoYmirianoActiveAbility.INSTANCE, 1);

        assertEquals(2, sheet.getDeterminationSpent());
        assertEquals(ElementalType.GELO, DevotoFeat.IMPACTO_YMIRIANO.resolveDamageRetype(giant,
                SkillType.ATAQUE_CORPO_A_CORPO, null, sheet).elementalType());
        assertTrue(DevotoFeat.IMPACTO_YMIRIANO.resolveEffectChains(giant, SkillType.ATAQUE_CORPO_A_CORPO, null, sheet,
                null, null).stream().anyMatch(ExplosaoCataclismica.class::isInstance));
    }

    @Test
    void mentalidadeDeTeslaPicksConhecimentosAndEasesThem() {
        Character scholar = devotee(Deity.TESLA, DevotionTier.FUNDAMENTALISTA, DevotoFeat.MENTALIDADE_DE_TESLA);

        assertEquals(List.of(DevotionTier.ADEPTO, DevotionTier.FIEL),
                devotion.owedPicks(scholar).stream().map(DevotionService.OwedPicks::rung).toList());
        assertEquals(1, DevotoFeat.MENTALIDADE_DE_TESLA.resolveDifficultyReduction(SkillType.CONHECIMENTOS, scholar));
    }

    @Test
    void tocadoPorUndineHealsMoreAndIsEmpoweredWhenHealed() {
        Character healer = devotee(Deity.UNDINE_E_HALOI, DevotionTier.FUNDAMENTALISTA,
                DevotoFeat.TOCADO_POR_UNDINE_E_HALOI);
        CharacterSheet healerSheet = sheet(healer);
        CharacterSheet patient = sheet(CharacterFixture.blank(CharacterFixture.BLANK).build());
        patient.applyDamage(10);

        patient.heal(3, HealingSource.spell(VidaSpell.REVIGORAR, healerSheet));
        assertEquals(6, patient.getDamageTaken(), "+1 to a Magia's heal");

        healerSheet.applyDamage(5);
        healerSheet.heal(2, HealingSource.spell(VidaSpell.REVIGORAR, patient));
        assertEquals(1, healerSheet.getTemporaryBonus(ModifierType.DAMAGE_ROLL_BONUS), "+1 dano for 1 Rodada");
    }

    @Test
    void esquecidaAndCultistaHideAndTheCultistStrikesNonUmbrals() {
        Character cultist = devotee(Deity.TECE_MORTES, DevotionTier.FUNDAMENTALISTA, DevotoFeat.CULTISTA_UMBRAL);
        Character forgotten = devotee(Deity.A_ESQUECIDA, DevotionTier.FIEL, DevotoFeat.ABRACADO_PELA_ESQUECIDA);

        assertEquals(2, DevotoFeat.CULTISTA_UMBRAL.resolveSkillRollBonus(SkillType.FURTIVIDADE, null,
                org.aventyrs.core.skill.furtividade.FurtividadeSpecialization.MAESTRIA_DA_OCULTACAO, cultist));
        assertEquals(1, DevotoFeat.ABRACADO_PELA_ESQUECIDA.resolveDefenseBonus(DefenseType.MAGIC, forgotten));
        assertTrue(DevotoFeat.isUmbral(cultist));
        assertTrue(DevotoFeat.isUmbral(forgotten));
        assertFalse(DevotoFeat.isUmbral(CharacterFixture.blank(CharacterFixture.BLANK).deity(Deity.GAEA).build()));
        assertEquals(0, DevotoFeat.CULTISTA_UMBRAL.resolveCriticalMarginIncrease(SkillType.ATAQUE_CORPO_A_CORPO, null,
                cultist), "no named target");
    }
}
