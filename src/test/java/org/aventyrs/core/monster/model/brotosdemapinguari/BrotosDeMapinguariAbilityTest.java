package org.aventyrs.core.monster.model.brotosdemapinguari;

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
        assertEquals(MonstrousTraits.CRITICAL_RESISTANCE_INSTANCE, both.getTotalCriticalResistance(null), "RC once");
        assertTrue(both.getCriticalEffectImmunities().containsAll(
                List.of(CriticalEffectType.ATORDOANTE, CriticalEffectType.FERIDA_PROFUNDA, CriticalEffectType.SANGRAMENTO)));
    }

    @Test
    void anatomiaVegetalReportsItsWeaknessesWithoutSizingThem() {
        MonsterSheet broto = spawn(0, lagrimas());
        DamageDescriptor fire = new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.FOGO);
        DamageDescriptor ice = new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.GELO);
        DamageDescriptor natural = new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.NATURAL);
        assertTrue(broto.isVulnerableToDamage(DamageType.ELEMENTAL, fire));
        assertTrue(broto.isVulnerableToDamage(DamageType.ELEMENTAL, ice));
        assertFalse(broto.isVulnerableToDamage(DamageType.ELEMENTAL, natural));
        assertEquals(10, finalDamage(broto, DamageType.ELEMENTAL, ElementalType.FOGO));
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
        assertEquals(8, finalDamage(broto, DamageType.MAGICO, null), "RD only; the Meio-Dano is physical");
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
    void dadivaDeGaeaHealsEveryRodadaAndCleanses() {
        MonsterSheet broto = spawn(46, MonstrousAbilitySelection.of(BrotosDeMapinguariAbility.DADIVA_DE_GAEA));
        broto.applyCondition(new Condition(ConditionType.ABALADO, 2));
        assertFalse(broto.hasCondition(ConditionType.ABALADO, null));
        broto.applyDamage(30);
        broto.tickTemporaryEffects();
        assertTrue(broto.getDamageTaken() < 30);

        broto.applyCondition(new Condition(ConditionType.DOENTE, 3));
        activeAbilities.activate(broto.getCharacter(), broto, held(broto, "Dádiva de Gaea"), 1);
        assertFalse(broto.hasCondition(ConditionType.DOENTE, null));
        assertEquals(1, broto.getTotalLifeSteal());
    }
}
