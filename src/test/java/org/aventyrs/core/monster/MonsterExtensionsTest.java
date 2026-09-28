package org.aventyrs.core.monster;

import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageScope;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.Dice;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.monster.model.almaelemental.AlmaElementalAbility;
import org.aventyrs.core.monster.model.cireneia.CireneiaAbility;
import org.aventyrs.core.sheet.DamageScopeEffect;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.RecurringDice;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.aventyrs.core.monster.model.MonsterTestKit.blueprint;
import static org.aventyrs.core.monster.model.MonsterTestKit.finalDamage;
import static org.aventyrs.core.monster.model.MonsterTestKit.spawn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The shared pieces the Modelos stand on: caller-rolled dice, scoped mitigation, multi-choice picks. */
class MonsterExtensionsTest {

    @Test
    void aDiceExpressionRefusesTheWrongRoll() {
        assertEquals(9, Dice.of(2, 1).total(List.of(3, 5)));
        assertThrows(IllegalOperationException.class, () -> Dice.of(2).total(List.of(3)));
        assertThrows(IllegalOperationException.class, () -> Dice.of(1).total(List.of(7)));
    }

    @Test
    void pendingDamageDiceGoThroughMitigation() {
        MonsterSheet salamander = spawn(46, MonstrousAbilitySelection.of(AlmaElementalAbility.SANGUE_ELEMENTAL, "FOGO"));
        salamander.applyEffect(RecurringDice.damage(Dice.of(2), new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.FOGO),
                2, "Marca Elemental"));
        salamander.tickTemporaryEffects();
        UUID pending = salamander.getPendingDiceRolls().get(0).id();
        assertEquals(0, salamander.resolveDiceRoll(pending, List.of(6, 6)), "immune to its own element at Apex");
        assertTrue(salamander.getPendingDiceRolls().isEmpty());
        assertThrows(IllegalOperationException.class, () -> salamander.resolveDiceRoll(pending, List.of(1, 1)));
    }

    @Test
    void aScopedEffectIsLiftedByItsSource() {
        MonsterSheet sheet = spawn(0);
        sheet.applyEffect(new DamageScopeEffect(DamageScopeEffect.Kind.HALVES, DamageScope.PHYSICAL, null, "Teste"));
        assertEquals(5, finalDamage(sheet, DamageType.FISICO, null));
        assertEquals(10, finalDamage(sheet, DamageType.MAGICO, null));
        sheet.removeEffectsFrom("Teste");
        assertEquals(10, finalDamage(sheet, DamageType.FISICO, null));
    }

    @Test
    void aPickUnderASpecNotAskedOrRepeatedIsRefused() {
        MonsterBlueprint extra = blueprint(12, MonstrousAbilitySelection.of(CireneiaAbility.RELAMPEJANTE,
                Map.of(CireneiaAbility.BONUS_CHOICE, List.of("REACTIONS"), "unasked", List.of("X")))).build();
        assertTrue(MonsterRules.validate(extra).contains(
                MonsterViolation.of(MonsterViolation.Code.INVALID_ABILITY_CHOICE, "RELAMPEJANTE", 1, 0)));

        MonsterBlueprint repeated = blueprint(12, MonstrousAbilitySelection.of(AlmaElementalAbility.CONJURACAO_ELEMENTAL,
                Map.of(AlmaElementalAbility.TREES, List.of("VOO", "VOO")))).build();
        assertTrue(MonsterRules.validate(repeated).stream().anyMatch(v -> v.code() == MonsterViolation.Code.INVALID_ABILITY_CHOICE));
    }

    @Test
    void theSingleChoiceShortcutFillsTheOnlySpec() {
        MonstrousAbilitySelection selection = MonstrousAbilitySelection.of(CireneiaAbility.RELAMPEJANTE, "FREE_ACTIONS");
        assertEquals(Map.of(CireneiaAbility.BONUS_CHOICE, List.of("FREE_ACTIONS")), selection.choices());
    }
}
