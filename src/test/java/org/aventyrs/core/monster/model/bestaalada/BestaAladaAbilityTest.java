package org.aventyrs.core.monster.model.bestaalada;

import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.services.ActiveAbilityServiceImpl;
import org.aventyrs.core.character.services.MovementServiceImpl;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.monster.MonstrousAbilitySelection;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.aventyrs.core.monster.model.MonsterTestKit.finalDamage;
import static org.aventyrs.core.monster.model.MonsterTestKit.held;
import static org.aventyrs.core.monster.model.MonsterTestKit.spawn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BestaAladaAbilityTest {

    private final ActiveAbilityServiceImpl activeAbilities = new ActiveAbilityServiceImpl();

    @Test
    void mergulhoAtrozNeedsFlightAndLandsTheMonster() {
        MonsterSheet sheet = spawn(46, MonstrousAbilitySelection.of(BestaAladaAbility.PLANAR_E_PAIRAR));
        var mergulho = held(sheet, "Mergulho Atroz");
        assertThrows(IllegalOperationException.class, () -> activeAbilities.activate(sheet.getCharacter(), sheet, mergulho, 1));

        sheet.setFlying(true);
        activeAbilities.activate(sheet.getCharacter(), sheet, mergulho, 1);
        assertFalse(sheet.isFlying());
        assertEquals(2, sheet.getTemporaryBonus(ModifierType.EXTRA_DAMAGE_DICE));
        assertEquals(4, sheet.getTemporaryBonus(ModifierType.LESSER_CRITICAL_MARGIN));
    }

    @Test
    void desfileDeSylphMovesFasterOnlyInFlight() {
        MonsterSheet sheet = spawn(0, MonstrousAbilitySelection.of(BestaAladaAbility.DESFILE_DE_SYLPH));
        MovementServiceImpl movement = new MovementServiceImpl();
        int ground = movement.getMovementBase(sheet);
        sheet.setFlying(true);
        assertEquals(ground + 2, movement.getMovementBase(sheet));
        sheet.setFlying(false);
        assertEquals(ground, movement.getMovementBase(sheet));
    }

    @Test
    void investidaAereaRaisesTheAttackGdAndDice() {
        MonsterSheet sheet = spawn(26, MonstrousAbilitySelection.of(BestaAladaAbility.DESFILE_DE_SYLPH));
        var before = sheet.getSkillDifficulty(SkillType.ATAQUE_CORPO_A_CORPO).level();
        activeAbilities.activate(sheet.getCharacter(), sheet, held(sheet, "Investida Aérea"), 1);
        assertEquals(before.harder(1), sheet.getSkillDifficulty(SkillType.ATAQUE_CORPO_A_CORPO).level());
        assertEquals(1, sheet.getTemporaryBonus(ModifierType.EXTRA_DAMAGE_DICE));
        assertEquals(4, sheet.getTemporaryBonus(ModifierType.LESSER_CRITICAL_MARGIN));
    }

    @Test
    void dominioDosCeusBoostsPhysicalPericiasInFlightAndPaAtApex() {
        MonsterSheet sheet = spawn(12, MonstrousAbilitySelection.of(BestaAladaAbility.DOMINIO_DOS_CEUS));
        int atletismo = sheet.getSkillDifficulty(SkillType.ATLETISMO).bonus();
        sheet.setFlying(true);
        assertEquals(atletismo + 2, sheet.getSkillDifficulty(SkillType.ATLETISMO).bonus());
        assertEquals(sheet.getSkillDifficulty(SkillType.PERSUASAO).bonus(), 0);

        MonsterSheet apex = spawn(46, MonstrousAbilitySelection.of(BestaAladaAbility.DOMINIO_DOS_CEUS));
        assertEquals(new ActionPointsServiceImpl().getMaxActionPoints(sheet, 1) + 2 + 2,
                new ActionPointsServiceImpl().getMaxActionPoints(apex, 1), "Apex +2PA, and the Apex Categoria's own +2");
        assertTrue(apex.getCharacter().getActiveAbilities().isEmpty());
    }

    @Test
    void defesaAereaHardensTheMonsterInFlight() {
        MonsterSheet sheet = spawn(12, MonstrousAbilitySelection.of(BestaAladaAbility.DEFESA_AREA));
        int grounded = sheet.getDefense(DefenseType.PHYSICAL);
        sheet.setFlying(true);
        assertEquals(grounded + 5, sheet.getDefense(DefenseType.PHYSICAL));
        assertEquals(9, finalDamage(sheet, DamageType.FISICO, null));

        MonsterSheet apex = spawn(46, MonstrousAbilitySelection.of(BestaAladaAbility.DEFESA_AREA));
        apex.setFlying(true);
        assertEquals(4, finalDamage(apex, DamageType.FISICO, null), "(10-1)/2 in flight at Apex");
        apex.setFlying(false);
        assertEquals(10, finalDamage(apex, DamageType.FISICO, null));
    }

    @Test
    void vooPermanenteNeverLands() {
        MonsterSheet sheet = spawn(26, MonstrousAbilitySelection.of(BestaAladaAbility.VOO_PERMANENTE),
                MonstrousAbilitySelection.of(BestaAladaAbility.SUPERSONICO));
        assertTrue(sheet.isFlying());
        sheet.setFlying(false);
        assertTrue(sheet.isFlying());
        // Supersônico's +2PA in flight is on from the start
        assertEquals(2, sheet.getTemporaryBonus(ModifierType.ACTION_POINTS));
    }

    @Test
    void formaDeVentoIsImmuneToAllButElementalAndPrimordial() {
        MonsterSheet sheet = spawn(46, MonstrousAbilitySelection.of(BestaAladaAbility.PREDILETO_DE_SYLPH));
        activeAbilities.activate(sheet.getCharacter(), sheet, held(sheet, "Forma de Vento"), 1);
        assertEquals(0, finalDamage(sheet, DamageType.FISICO, null));
        assertEquals(0, finalDamage(sheet, DamageType.MAGICO, null));
        assertEquals(10, finalDamage(sheet, DamageType.ELEMENTAL, ElementalType.VENTO));
        assertEquals(10, finalDamage(sheet, DamageType.PRIMORDIAL, null));
    }
}
