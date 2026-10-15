package org.aventyrs.core.monster.model.brotosdemapinguari;

import org.aventyrs.core.character.Resistance;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.services.ActiveAbilityServiceImpl;
import org.aventyrs.core.character.services.CharacterSizeServiceImpl;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.catalog.MagicTree;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.monster.MonstrousAbilitySelection;
import org.aventyrs.core.monster.model.MonstrousTraits;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.character.services.AttackTargetingServiceImpl;
import org.aventyrs.core.effect.monstrous.AutocontroleLoss;
import org.aventyrs.core.effect.monstrous.RouboDeBonusBase;
import org.aventyrs.core.effect.monstrous.VenenoPrimaveril;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.MonstrousAbilityGrant;
import org.aventyrs.core.scene.AreaOfEffect;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.aventyrs.core.monster.model.MonsterTestKit.finalDamage;
import static org.aventyrs.core.monster.model.MonsterTestKit.held;
import static org.aventyrs.core.monster.model.MonsterTestKit.spawn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrotosDeMapinguariAbilityTest {

    private final ActiveAbilityServiceImpl activeAbilities = new ActiveAbilityServiceImpl();

    private static MonstrousAbilitySelection lagrimas() {
        return MonstrousAbilitySelection.of(BrotosDeMapinguariAbility.NASCIDO_DAS_LAGRIMAS_DE_FLORA, ElementalType.GELO.name());
    }

    private static MonstrousAbilitySelection sangue() {
        return MonstrousAbilitySelection.of(BrotosDeMapinguariAbility.NASCIDO_DO_SANGUE_DE_LACERTO, ElementalType.NATURAL.name());
    }

    @Test
    void anatomiaVegetalIsGrantedOnceThroughEitherNascido() {
        MonsterSheet both = spawn(5, lagrimas(), sangue());
        assertEquals(3, both.getCharacter().getEffectiveAttributeTotal(AttributeDomain.VIGOR), "Vigor +2 once");
        assertEquals(3, both.getCharacter().getEffectiveAttributeTotal(AttributeDomain.INSTINCT));
        assertEquals(3, both.getCharacter().getEffectiveAttributeTotal(AttributeDomain.STRENGTH));
        assertEquals(Resistance.RC.instance(), both.getTotalCriticalResistance(null), "RC once");
        assertTrue(both.getCriticalEffectImmunities().containsAll(
                List.of(CriticalEffectType.ATORDOANTE, CriticalEffectType.FERIDA_PROFUNDA, CriticalEffectType.SANGRAMENTO)));
    }

    @Test
    void anatomiaVegetalReportsItsWeaknessesWithoutSizingThem() {
        MonsterSheet broto = spawn(0, lagrimas());
        DamageDescriptor fire = new DamageDescriptor(DamageType.MAGICO, ElementalType.FOGO);
        DamageDescriptor ice = new DamageDescriptor(DamageType.MAGICO, ElementalType.GELO);
        DamageDescriptor natural = new DamageDescriptor(DamageType.MAGICO, ElementalType.NATURAL);
        assertTrue(broto.isVulnerableToDamage(DamageType.MAGICO, fire));
        assertTrue(broto.isVulnerableToDamage(DamageType.MAGICO, ice));
        assertFalse(broto.isVulnerableToDamage(DamageType.MAGICO, natural));
        assertEquals(10, finalDamage(broto, DamageType.MAGICO, ElementalType.FOGO));
    }

    @Test
    void lagrimasRegeneratesHalfItsInstintoAndRegeneracaoSelvagemAddsTheDice() {
        MonsterSheet broto = spawn(0, lagrimas());
        broto.applyDamage(10);
        broto.tickTemporaryEffects();
        assertEquals(9, broto.getDamageTaken(), "Instinto 3 → 1 PV per Rodada");

        activeAbilities.activate(broto.getCharacter(), broto, held(broto, "Regeneração Selvagem"), 1);
        broto.tickTemporaryEffects();
        assertEquals(8, broto.getDamageTaken());
        var pending = broto.getPendingDiceRolls();
        assertEquals(1, pending.size());
        assertEquals(1, pending.get(0).dice().count());
        broto.resolveDiceRoll(pending.get(0).id(), List.of(3));
        assertEquals(5, broto.getDamageTaken());
    }

    @Test
    void sangueStealsLifeAndCarnivoriaRollsForMore() {
        MonsterSheet broto = spawn(12, sangue());
        assertEquals(2, broto.getTotalLifeSteal());
        activeAbilities.activate(broto.getCharacter(), broto, held(broto, "Carnivoria"), 1, List.of(5));
        assertEquals(7, broto.getTotalLifeSteal());
    }

    @Test
    void primaverilAndOutonalGrowTheBroto() {
        CharacterSizeServiceImpl size = new CharacterSizeServiceImpl();
        assertEquals(SizeCategory.PLUS_ONE, size.getEffectiveSizeCategory(spawn(12, MonstrousAbilitySelection.of(BrotosDeMapinguariAbility.PRIMAVERIL))));
        MonsterSheet outonal = spawn(12, MonstrousAbilitySelection.of(BrotosDeMapinguariAbility.OUTONAL));
        assertEquals(SizeCategory.PLUS_ONE, size.getEffectiveSizeCategory(outonal));
        assertEquals(9, finalDamage(outonal, DamageType.FISICO, null));
    }

    @Test
    void raizesLongasCannotBeGrabbedAndRootsIntoAFortress() {
        MonsterSheet broto = spawn(26, MonstrousAbilitySelection.of(BrotosDeMapinguariAbility.RAIZES_LONGAS));
        broto.applyCondition(new Condition(ConditionType.AGARRADO, 2));
        broto.applyCondition(new Condition(ConditionType.CAIDO, 2));
        assertFalse(broto.hasCondition(ConditionType.AGARRADO, null));
        assertFalse(broto.hasCondition(ConditionType.CAIDO, null));

        activeAbilities.activate(broto.getCharacter(), broto, held(broto, "Raízes Longas"), 1);
        assertEquals(4, finalDamage(broto, DamageType.FISICO, null), "(10-2)/2");
        assertEquals(10, finalDamage(broto, DamageType.MAGICO, null),
                "RD reaches only physical damage, and so does this Meio-Dano");
        broto.tickTemporaryEffects();
        assertEquals(2, broto.getPendingDiceRolls().get(0).dice().count());
    }

    @Test
    void brumasDeFloraResistsMagicAndTeachesNaturalMagias() {
        MonsterSheet broto = spawn(26, MonstrousAbilitySelection.of(BrotosDeMapinguariAbility.BRUMAS_DE_FLORA));
        assertEquals(4, finalDamage(broto, DamageType.MAGICO, null), "(10 - RM 2) halved");
        assertFalse(broto.getCharacter().getSpells().isEmpty());
        assertTrue(broto.getCharacter().getSpells().stream().allMatch(spell -> MonstrousTraits.isNatural((MagicTree) spell.getTree())));
    }

    @Test
    void dadivaDeGaeaMaiorCleansesAndStealsBonusBase() {
        MonsterSheet broto = spawn(46, MonstrousAbilitySelection.of(BrotosDeMapinguariAbility.DADIVA_DE_GAEA));
        // The heal and the Medo/Possessão immunity are the aura's now (MonsterAuraSceneTest).
        assertEquals(1, broto.resolveMonsterAuras().size());

        broto.applyCondition(new Condition(ConditionType.DOENTE, 3));
        broto.applyCondition(new Condition(ConditionType.ENVENENADO, 3));
        activeAbilities.activate(broto.getCharacter(), broto, held(broto, "Dádiva de Gaea Maior"), 1);
        assertFalse(broto.hasCondition(ConditionType.DOENTE, null));
        assertFalse(broto.hasCondition(ConditionType.ENVENENADO, null));
        assertEquals(2, broto.getTotalLifeSteal());
        assertTrue(MonstrousAbilityGrant.attackEffectChains(broto, SkillType.ATAQUE_CORPO_A_CORPO, null).stream()
                .anyMatch(RouboDeBonusBase.class::isInstance));
    }

    @Test
    void carnivoriasWindowCarriesTheAutocontroleLossAndFromDevianteOferendaMaldita() {
        MonsterSheet deviante = spawn(12, MonstrousAbilitySelection.of(BrotosDeMapinguariAbility.NASCIDO_DO_SANGUE_DE_LACERTO,
                ElementalType.GELO.name()));
        assertTrue(MonstrousAbilityGrant.attackEffectChains(deviante, SkillType.ATAQUE_CORPO_A_CORPO, null).isEmpty());
        activeAbilities.activate(deviante.getCharacter(), deviante, held(deviante, "Carnivoria"), 1, List.of(5));
        assertTrue(MonstrousAbilityGrant.attackEffectChains(deviante, SkillType.ATAQUE_CORPO_A_CORPO, null).stream()
                .anyMatch(AutocontroleLoss.class::isInstance));
        assertEquals(List.of(CriticalEffectType.OFERENDA_MALDITA),
                MonstrousAbilityGrant.additionalCriticalEffects(deviante, SkillType.ATAQUE_CORPO_A_CORPO, null));
    }

    @Test
    void lancarEsporosIsACircularAttackThatPoisonsAndThenEveryAttackStealsFromThePoisoned() {
        MonsterSheet broto = spawn(12, MonstrousAbilitySelection.of(BrotosDeMapinguariAbility.PRIMAVERIL));
        AttackTargetingServiceImpl targeting = new AttackTargetingServiceImpl();
        assertTrue(targeting.resolveAttackArea(broto, SkillType.ATAQUE_A_DISTANCIA, null, null).isEmpty());
        activeAbilities.activate(broto.getCharacter(), broto, held(broto, "Lançar Esporos"), 1);
        assertEquals(java.util.Optional.of(AreaOfEffect.circle(Range.DISTANCIA_CURTA)),
                targeting.resolveAttackArea(broto, SkillType.ATAQUE_A_DISTANCIA, null, null));

        MonsterSheet victim = spawn(0, MonstrousAbilitySelection.of(org.aventyrs.core.monster.model.cireneia.CireneiaAbility.ATRIBUTOS_APRIMORADOS));
        int lifeMultiplier = victim.getTemporaryBonus(ModifierType.LIFE_MULTIPLIER);
        new VenenoPrimaveril(2).applyTo(victim);
        assertEquals(lifeMultiplier - 1, victim.getTemporaryBonus(ModifierType.LIFE_MULTIPLIER));
        assertEquals(-1, victim.getTemporaryBonus(ModifierType.DETERMINATION_MULTIPLIER));
        new VenenoPrimaveril(2).applyTo(victim);
        assertEquals(lifeMultiplier - 1, victim.getTemporaryBonus(ModifierType.LIFE_MULTIPLIER), "renews, never stacks");

        // Another Broto is never infected.
        new VenenoPrimaveril(2).applyTo(broto);
        assertEquals(0, broto.getTemporaryBonus(ModifierType.DETERMINATION_MULTIPLIER));
    }
}
