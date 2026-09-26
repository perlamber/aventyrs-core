package org.aventyrs.core.monster.model.mutantemonstruoso;

import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.services.ActiveAbilityServiceImpl;
import org.aventyrs.core.character.services.CharacterSizeServiceImpl;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.monster.MonsterRules;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.monster.MonstrousAbilitySelection;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.aventyrs.core.monster.model.MonsterTestKit.blueprint;
import static org.aventyrs.core.monster.model.MonsterTestKit.finalDamage;
import static org.aventyrs.core.monster.model.MonsterTestKit.held;
import static org.aventyrs.core.monster.model.MonsterTestKit.spawn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MutanteMonstruosoAbilityTest {

    private final ActiveAbilityServiceImpl activeAbilities = new ActiveAbilityServiceImpl();

    @Test
    void fisiologiaEstranhaHalvesDamageAndAddsDefesasFromDeviante() {
        MonsterSheet sheet = spawn(12, MonstrousAbilitySelection.of(MutanteMonstruosoAbility.FISIOLOGIA_ESTRANHA));
        assertEquals(9, finalDamage(sheet, DamageType.FISICO, null), "RDS -1");
        assertEquals(2, sheet.getTotalCriticalResistance(null));
        int defense = new DefenseServiceImpl().getTotalDefense(sheet, DefenseType.PHYSICAL);
        activeAbilities.activate(sheet.getCharacter(), sheet, held(sheet, "Fisiologia Estranha"), 1);
        assertEquals(4, finalDamage(sheet, DamageType.FISICO, null), "(10-1)/2");
        assertEquals(defense + 3, new DefenseServiceImpl().getTotalDefense(sheet, DefenseType.PHYSICAL));
    }

    @Test
    void membrosMultiplosSurtoDeAcaoCountsItsSets() {
        MonstrousAbilitySelection membros = MonstrousAbilitySelection.of(MutanteMonstruosoAbility.MEMBROS_MULTIPLOS, "BRACOS");
        MonsterSheet apex = spawn(46, membros);
        int before = new ActionPointsServiceImpl().getMaxActionPoints(apex, 1);
        activeAbilities.activate(apex.getCharacter(), apex, held(apex, "Surto de Ação"), 1);
        assertEquals(before + 6, new ActionPointsServiceImpl().getMaxActionPoints(apex, 1));
    }

    @Test
    void selecaoNaturalGrantsItsAtributoAdaptabilidadeAndApexRe() {
        MonstrousAbilitySelection selecao = MonstrousAbilitySelection.of(MutanteMonstruosoAbility.SELECAO_NATURAL_SUPERIOR, "VIGOR");
        assertEquals(3, spawn(12, selecao).getCharacter().getEffectiveAttributeTotal(AttributeDomain.VIGOR));

        MonsterSheet sheet = spawn(12, selecao);
        HitPointsServiceImpl hitPoints = new HitPointsServiceImpl();
        int before = hitPoints.getMaxHitPoints(sheet.getCharacter(), sheet);
        activeAbilities.activate(sheet.getCharacter(), sheet, held(sheet, "Adaptabilidade"), 1);
        assertEquals(before + 3 * 3, hitPoints.getMaxHitPoints(sheet.getCharacter(), sheet), "Vigor 3 × 3 more");

        MonsterSheet apex = spawn(46, selecao);
        assertEquals(8, finalDamage(apex, DamageType.ELEMENTAL, ElementalType.VENTO));
        assertEquals(8, finalDamage(apex, DamageType.ELEMENTAL, ElementalType.GELO));
    }

    @Test
    void tamanhoVariavelShiftsSizeForGoodAndForAWhile() {
        MonstrousAbilitySelection grow = MonstrousAbilitySelection.of(MutanteMonstruosoAbility.TAMANHO_VARIAVEL, MutanteMonstruosoAbility.GROW);
        MonsterSheet sheet = spawn(26, grow);
        CharacterSizeServiceImpl size = new CharacterSizeServiceImpl();
        assertEquals(SizeCategory.PLUS_ONE, size.getEffectiveSizeCategory(sheet));
        activeAbilities.activate(sheet.getCharacter(), sheet, held(sheet, "Tamanho Variável (reduzir)"), 1);
        assertEquals(SizeCategory.MINUS_ONE, size.getEffectiveSizeCategory(sheet));

        MonsterSheet apex = spawn(46, grow);
        assertEquals(5, apex.getCharacter().getEffectiveAttributeTotal(AttributeDomain.STRENGTH));

        MonstrousAbilitySelection abominacao = MonstrousAbilitySelection.of(MutanteMonstruosoAbility.TAMANHO_VARIAVEL, Map.of(
                MutanteMonstruosoAbility.DIRECTION, List.of(MutanteMonstruosoAbility.SHRINK),
                MutanteMonstruosoAbility.ATTRIBUTES, List.of("VIGOR", "DEXTERITY")));
        MonsterSheet huge = spawn(61, abominacao);
        assertEquals(3, huge.getCharacter().getEffectiveAttributeTotal(AttributeDomain.VIGOR));
        assertEquals(SizeCategory.MINUS_ONE, size.getEffectiveSizeCategory(huge));
    }

    @Test
    void camuflagemPerfeitaStepsFurtividade() {
        MonstrousAbilitySelection camuflagem = MonstrousAbilitySelection.of(MutanteMonstruosoAbility.CAMUFLAGEM_PERFEITA);
        assertEquals(DifficultyLevel.EASY, MonsterRules.skillDifficulty(blueprint(26, camuflagem).build(), SkillType.FURTIVIDADE).level());
        assertEquals(4, MonsterRules.skillDifficulty(blueprint(46, camuflagem).build(), SkillType.FURTIVIDADE).bonus());
        assertEquals(DifficultyLevel.MEDIUM, MonsterRules.skillDifficulty(blueprint(61, camuflagem).build(), SkillType.FURTIVIDADE).level());
    }

    @Test
    void adaptacaoMilagrosaHealsByTheCallersDiceAndRegeneratesAfterBeingHurt() {
        MonsterSheet sheet = spawn(46, MonstrousAbilitySelection.of(MutanteMonstruosoAbility.ADAPTACAO_MILAGROSA));
        sheet.applyDamage(20);
        var mor = held(sheet, "Adaptação Milagrosa");
        assertThrows(IllegalOperationException.class, () -> activeAbilities.activate(sheet.getCharacter(), sheet, mor, 1));
        assertThrows(IllegalOperationException.class, () -> activeAbilities.activate(sheet.getCharacter(), sheet, mor, 1, List.of(6, 6)));
        activeAbilities.activate(sheet.getCharacter(), sheet, mor, 1, List.of(6, 5, 4));
        assertEquals(5, sheet.getDamageTaken());

        new DamageServiceImpl().applyDamage(sheet, null, 5, false);
        sheet.tickTemporaryEffects();
        assertEquals(1, sheet.getPendingDiceRolls().size(), "one 1d6 owed this Rodada, not cumulative");
    }

    @Test
    void regeneracaoMultiplicativaRecoversVigorEveryRodada() {
        MonsterSheet sheet = spawn(46, MonstrousAbilitySelection.of(MutanteMonstruosoAbility.REGENERACAO_MULTIPLICATIVA));
        sheet.applyDamage(5);
        sheet.tickTemporaryEffects();
        assertEquals(4, sheet.getDamageTaken(), "Vigor 1 per Rodada");
        assertTrue(sheet.getPendingDiceRolls().isEmpty());

        MonsterSheet abominacao = spawn(61, MonstrousAbilitySelection.of(MutanteMonstruosoAbility.REGENERACAO_MULTIPLICATIVA));
        abominacao.tickTemporaryEffects();
        assertEquals(1, abominacao.getPendingDiceRolls().size());
    }
}
