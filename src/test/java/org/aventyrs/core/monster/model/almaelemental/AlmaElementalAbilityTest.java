package org.aventyrs.core.monster.model.almaelemental;

import org.aventyrs.core.character.Resistance;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageSanctity;
import org.aventyrs.core.character.services.AttackTargetingServiceImpl;
import org.aventyrs.core.effect.monstrous.MarcaElemental;
import org.aventyrs.core.effect.monstrous.MurcharAbissal;
import org.aventyrs.core.monster.MonstrousAbilityGrant;
import org.aventyrs.core.scene.AreaOfEffect;
import org.aventyrs.core.sheet.RecurringDamage;
import java.util.Optional;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.services.ActiveAbilityServiceImpl;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.catalog.MagicTree;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.MonsterBlueprint;
import org.aventyrs.core.monster.MonsterRules;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.monster.MonsterViolation;
import org.aventyrs.core.monster.MonstrousAbilitySelection;
import org.aventyrs.core.monster.model.MonstrousTraits;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.aventyrs.core.monster.model.MonsterTestKit.blueprint;
import static org.aventyrs.core.monster.model.MonsterTestKit.finalDamage;
import static org.aventyrs.core.monster.model.MonsterTestKit.held;
import static org.aventyrs.core.monster.model.MonsterTestKit.spawn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlmaElementalAbilityTest {

    private static MonstrousAbilitySelection fireBlood() {
        return MonstrousAbilitySelection.of(AlmaElementalAbility.SANGUE_ELEMENTAL, ElementalType.FOGO.name());
    }

    @Test
    void sangueElementalResistsItsElementThenHalvesThenIgnoresIt() {
        // Presa: one RE instance
        assertEquals(10 - Resistance.RE.instance(), finalDamage(spawn(0, fireBlood()), DamageType.MAGICO, ElementalType.FOGO));
        assertEquals(10, finalDamage(spawn(0, fireBlood()), DamageType.MAGICO, ElementalType.GELO));
        // Deviante: then halved
        assertEquals((10 - Resistance.RE.instance()) / 2, finalDamage(spawn(12, fireBlood()), DamageType.MAGICO, ElementalType.FOGO));
        // Apex: immune
        assertEquals(0, finalDamage(spawn(46, fireBlood()), DamageType.MAGICO, ElementalType.FOGO));
        assertEquals(10, finalDamage(spawn(46, fireBlood()), DamageType.FISICO, null));
    }

    @Test
    void sangueElementalGrantsInstintoAndResistenciaACriticos() {
        MonsterSheet presa = spawn(0, fireBlood());
        assertEquals(3, presa.getCharacter().getEffectiveAttributeTotal(AttributeDomain.INSTINCT));
        assertEquals(Resistance.RC.instance(), presa.getTotalCriticalResistance(null));
        assertEquals(5, spawn(26, fireBlood()).getCharacter().getEffectiveAttributeTotal(AttributeDomain.INSTINCT));
    }

    @Test
    void sangueElementalNeedsItsElementPicked() {
        MonsterBlueprint unpicked = blueprint(0, MonstrousAbilitySelection.of(AlmaElementalAbility.SANGUE_ELEMENTAL)).build();
        assertTrue(MonsterRules.validate(unpicked).stream().anyMatch(v -> v.code() == MonsterViolation.Code.INVALID_ABILITY_CHOICE));
    }

    private static MonstrousAbilitySelection claws() {
        return MonstrousAbilitySelection.of(AlmaElementalAbility.ARMAMENTO_ELEMENTAL, Map.of(
                AlmaElementalAbility.WEAPON, List.of(NaturalWeapon.GARRAS_AFIADAS.name()),
                MonstrousTraits.ELEMENT, List.of(ElementalType.FOGO.name())));
    }

    @Test
    void armamentoElementalGrantsTheWeaponAndFromDevianteAnExplosionAndThreeDamage() {
        MonsterSheet presa = spawn(0, claws());
        assertTrue(presa.getNaturalWeapons().contains(NaturalWeapon.GARRAS_AFIADAS));
        assertTrue(presa.getCharacter().getNaturalWeapons().contains(NaturalWeapon.GARRAS_AFIADAS));

        SkillType skill = NaturalWeapon.GARRAS_AFIADAS.getSkillType();
        // 20261015: the old "+2 on attacks and damage" is gone — an Explosão and +3 Danos instead.
        assertEquals(MonsterRules.skillDifficulty(blueprint(0, claws()).build(), skill).bonus(),
                MonsterRules.skillDifficulty(blueprint(12, claws()).build(), skill).bonus());
        AttackTargetingServiceImpl targeting = new AttackTargetingServiceImpl();
        assertTrue(targeting.resolveAttackArea(presa.getCharacter(), skill, NaturalWeapon.GARRAS_AFIADAS, null).isEmpty());
        MonsterSheet deviante = spawn(12, claws());
        assertEquals(Optional.of(AreaOfEffect.ATTACK_EXPLOSION),
                targeting.resolveAttackArea(deviante.getCharacter(), skill, NaturalWeapon.GARRAS_AFIADAS, null));
        assertEquals(0, presa.getTemporaryBonus(ModifierType.DAMAGE_ROLL_BONUS));
        assertEquals(3, deviante.getTemporaryBonus(ModifierType.DAMAGE_ROLL_BONUS));

        assertEquals(3, AlmaElementalAbility.ARMAMENTO_ELEMENTAL.resolveCriticalMarginIncrease(skill,
                blueprint(46, claws()).build().contextFor(blueprint(46, claws()).build().getAbilities().get(0))));
    }

    @Test
    void anatomiaElementalRetypesEveryAttackToItsElement() {
        MonsterSheet sheet = spawn(0, fireBlood());
        assertEquals(new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.FOGO),
                MonstrousAbilityGrant.attackDamageRetype(sheet, SkillType.ATAQUE_CORPO_A_CORPO, NaturalWeapon.GARRAS_AFIADAS));
    }

    @Test
    void furiaElementalRetypesAddsCataclismoAndFromPredadorMarcaElemental() {
        MonsterSheet presa = spawn(0, claws());
        SkillType skill = NaturalWeapon.GARRAS_AFIADAS.getSkillType();
        assertEquals(List.of(), MonstrousAbilityGrant.additionalCriticalEffects(presa, skill, NaturalWeapon.GARRAS_AFIADAS));
        new ActiveAbilityServiceImpl().activate(presa.getCharacter(), presa, held(presa, "Fúria Elemental"), 1);
        assertEquals(new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.FOGO),
                MonstrousAbilityGrant.attackDamageRetype(presa, skill, NaturalWeapon.GARRAS_AFIADAS));
        assertEquals(List.of(CriticalEffectType.CATACLISMO),
                MonstrousAbilityGrant.additionalCriticalEffects(presa, skill, NaturalWeapon.GARRAS_AFIADAS));
        assertEquals(3, presa.getTemporaryBonus(ModifierType.LESSER_CRITICAL_MARGIN));
        assertTrue(MonstrousAbilityGrant.attackEffectChains(presa, skill, NaturalWeapon.GARRAS_AFIADAS).isEmpty());

        MonsterSheet predador = spawn(26, claws());
        new ActiveAbilityServiceImpl().activate(predador.getCharacter(), predador, held(predador, "Fúria Elemental"), 1);
        assertEquals(new DamageDescriptor(DamageType.MAGICO, ElementalType.FOGO),
                MonstrousAbilityGrant.attackDamageRetype(predador, skill, NaturalWeapon.GARRAS_AFIADAS));
        assertTrue(MonstrousAbilityGrant.attackEffectChains(predador, skill, NaturalWeapon.GARRAS_AFIADAS).stream()
                .anyMatch(MarcaElemental.class::isInstance));
    }

    @Test
    void marcaElementalDealsThreeMagicoElementalForTwoPlusVigorRodadas() {
        MonsterSheet target = spawn(0, claws());
        new MarcaElemental(ElementalType.FOGO).applyTo(target);
        RecurringDamage mark = target.getRunningEffects().stream().filter(RecurringDamage.class::isInstance)
                .map(RecurringDamage.class::cast).findFirst().orElseThrow();
        assertEquals(3, mark.getValuePerRound());
        assertEquals(2 + target.getAttributeTotal(AttributeDomain.VIGOR), mark.getRemainingRounds());
        new MarcaElemental(ElementalType.FOGO).applyTo(target);
        assertEquals(1, target.getRunningEffects().stream().filter(RecurringDamage.class::isInstance).count());
    }

    @Test
    void ascencaoAbissalTurnsTheElementalRetypeProfaneAndCarriesMurchar() {
        MonsterSheet sheet = spawn(46, fireBlood(),
                MonstrousAbilitySelection.of(AlmaElementalAbility.ASCENCAO, AlmaElementalAbility.ABISSAL));
        assertEquals(new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.FOGO, DamageSanctity.PROFANO),
                MonstrousAbilityGrant.attackDamageRetype(sheet, SkillType.ATAQUE_CORPO_A_CORPO, NaturalWeapon.GARRAS_AFIADAS));
        assertTrue(MonstrousAbilityGrant.attackEffectChains(sheet, SkillType.ATAQUE_CORPO_A_CORPO,
                NaturalWeapon.GARRAS_AFIADAS).stream().anyMatch(MurcharAbissal.class::isInstance));
    }

    @Test
    void soproCataclismicoAddsCataclismoToTheSoproOnly() {
        MonsterSheet sheet = spawn(26, MonstrousAbilitySelection.of(AlmaElementalAbility.SOPRO_ELEMENTAL));
        new ActiveAbilityServiceImpl().activate(sheet.getCharacter(), sheet, held(sheet, "Sopro Cataclísmico"), 1);
        SkillType skill = NaturalWeapon.ARMA_DE_SOPRO.getSkillType();
        assertEquals(List.of(CriticalEffectType.CATACLISMO),
                MonstrousAbilityGrant.additionalCriticalEffects(sheet, skill, NaturalWeapon.ARMA_DE_SOPRO));
        assertEquals(List.of(), MonstrousAbilityGrant.additionalCriticalEffects(sheet, skill, NaturalWeapon.GARRAS_AFIADAS));
    }

    @Test
    void tormentaCataclismicaIsOncePerCena() {
        MonsterSheet sheet = spawn(46, MonstrousAbilitySelection.of(AlmaElementalAbility.TORMENTA_CATACLISMICA));
        assertTrue(((org.aventyrs.core.monster.model.MonstrousActiveAbility) held(sheet, "Tormenta Cataclísmica"))
                .isOncePerScene());
    }

    @Test
    void auraElementalOnlyActivatesWhenWounded() {
        MonsterSheet sheet = spawn(12, MonstrousAbilitySelection.of(AlmaElementalAbility.SANGUE_ELEMENTAL_DEVIANTE, "FOGO"));
        ActiveAbilityServiceImpl service = new ActiveAbilityServiceImpl();
        assertThrows(RuntimeException.class,
                () -> service.activate(sheet.getCharacter(), sheet, held(sheet, "Aura Elemental"), 1));
        sheet.applyDamage(1);
        service.activate(sheet.getCharacter(), sheet, held(sheet, "Aura Elemental"), 1);
    }

    @Test
    void conjuracaoElementalTeachesItsTreesToTheCategoriasDepth() {
        Map<String, List<String>> deviantePicks = Map.of(AlmaElementalAbility.TREES,
                List.of(MagicTree.IRA_DE_VULCANO.name(), MagicTree.VOO.name()));
        MonsterBlueprint deviante = blueprint(12, MonstrousAbilitySelection.of(AlmaElementalAbility.CONJURACAO_ELEMENTAL, deviantePicks)).build();
        var spells = deviante.buildCharacter().getSpells();
        assertFalse(spells.isEmpty());
        assertTrue(spells.stream().allMatch(spell -> BranchLevel.MUDA.isAtLeast(spell.getBranchLevel())));
        assertTrue(spells.stream().allMatch(spell -> spell.getTree() == MagicTree.IRA_DE_VULCANO || spell.getTree() == MagicTree.VOO));
        assertEquals(2, deviante.buildCharacter().getAttributes().getAttribute(AttributeDomain.FOCUS).getRacialBonus());
        assertEquals(List.of(), MonsterRules.validate(deviante).stream()
                .filter(v -> v.code() == MonsterViolation.Code.INVALID_ABILITY_CHOICE).toList());

        // a Predador must also pick its third tree
        MonsterBlueprint predador = deviante.toBuilder().powerDegree(26).build();
        assertTrue(MonsterRules.validate(predador).stream().anyMatch(v -> v.code() == MonsterViolation.Code.INVALID_ABILITY_CHOICE));
        // and only Elemental trees count at Deviante
        MonsterBlueprint wrongTree = blueprint(12, MonstrousAbilitySelection.of(AlmaElementalAbility.CONJURACAO_ELEMENTAL,
                Map.of(AlmaElementalAbility.TREES, List.of(MagicTree.VIDA.name(), MagicTree.VOO.name())))).build();
        assertTrue(MonsterRules.validate(wrongTree).stream().anyMatch(v -> v.code() == MonsterViolation.Code.INVALID_ABILITY_CHOICE));
    }

    @Test
    void soproElementalGrantsTheBreathAndACataclysmicTurn() {
        MonsterSheet sheet = spawn(26, MonstrousAbilitySelection.of(AlmaElementalAbility.SOPRO_ELEMENTAL));
        assertTrue(sheet.getNaturalWeapons().contains(NaturalWeapon.ARMA_DE_SOPRO));
        new ActiveAbilityServiceImpl().activate(sheet.getCharacter(), sheet, held(sheet, "Sopro Cataclísmico"), 1);
        assertEquals(2, sheet.getTemporaryBonus(ModifierType.EXTRA_DAMAGE_DICE));
        assertEquals(3, sheet.getTemporaryBonus(ModifierType.LESSER_CRITICAL_MARGIN));
    }

    @Test
    void tormentaRaisesPdAndRegeneratesByTheCallersDice() {
        MonsterSheet sheet = spawn(46, MonstrousAbilitySelection.of(AlmaElementalAbility.TORMENTA_CATACLISMICA));
        assertEquals(4, sheet.getCharacter().getDeterminationMultiplier()
                + AlmaElementalAbility.TORMENTA_CATACLISMICA.resolveModifier(ModifierType.DETERMINATION_MULTIPLIER,
                        org.aventyrs.core.monster.model.AbilityContext.of(org.aventyrs.core.monster.MonsterCategory.APEX)));
        new ActiveAbilityServiceImpl().activate(sheet.getCharacter(), sheet, held(sheet, "Tormenta Cataclísmica"), 1);
        assertEquals(2, sheet.getTotalLifeSteal());
        sheet.applyDamage(10);
        sheet.tickTemporaryEffects();
        assertEquals(1, sheet.getPendingDiceRolls().size());
        assertEquals(4, sheet.resolveDiceRoll(sheet.getPendingDiceRolls().get(0).id(), List.of(4)));
        assertEquals(6, sheet.getDamageTaken());
    }

    @Test
    void ascencaoAbissalRefusesDiseaseAndCurseAndSteals() {
        MonsterSheet sheet = spawn(46, MonstrousAbilitySelection.of(AlmaElementalAbility.ASCENCAO, AlmaElementalAbility.ABISSAL));
        sheet.applyCondition(new Condition(ConditionType.DOENTE, 2));
        sheet.applyCondition(new Condition(ConditionType.AMALDICOADO, 2));
        assertFalse(sheet.hasCondition(ConditionType.DOENTE, null));
        assertFalse(sheet.hasCondition(ConditionType.AMALDICOADO, null));
        assertEquals(2, sheet.getTotalLifeSteal());

        MonsterSheet celestial = spawn(46, MonstrousAbilitySelection.of(AlmaElementalAbility.ASCENCAO, AlmaElementalAbility.CELESTIAL));
        celestial.applyCondition(new Condition(ConditionType.AMALDICOADO, 2));
        assertTrue(celestial.hasCondition(ConditionType.AMALDICOADO, null));
        celestial.tickTemporaryEffects();
        assertEquals(1, celestial.getPendingDiceRolls().size());

        MonsterSheet abominacao = spawn(61, MonstrousAbilitySelection.of(AlmaElementalAbility.ASCENCAO, AlmaElementalAbility.ABISSAL));
        assertTrue(abominacao.getCriticalEffectImmunities().contains(CriticalEffectType.SANGRAMENTO));
    }
}
