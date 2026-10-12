package org.aventyrs.core.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.ItemPurchaseServiceImpl;
import org.aventyrs.core.character.services.MovementServiceImpl;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.PowerStoneBinding;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pedras do Poder, V19 (docs/rules/pedras-do-poder.txt) — table rulings 2026-10-12. */
class PowerStoneTest {

    private final PowerStoneService service = new PowerStoneServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static AbstractItem masterpieceArmor() {
        AbstractItem armor = AbstractItem.builder().name("Peitoral").category(ItemCategory.ARMOR).hardness(20).build();
        armor.setMasterpiece(ItemMasterpiece.of(DefensiveMasterpiece.REFORCADA));
        return armor;
    }

    private static AbstractWeapon masterpieceBlade() {
        AbstractWeapon blade = AbstractWeapon.builder().name("Lâmina").category(ItemCategory.LIGHT_BLADE)
                .hardness(20).damageBase(DamageBase.of(1, 1)).skillType(SkillType.ATAQUE_CORPO_A_CORPO).build();
        blade.setMasterpiece(OffensiveMasterpiece.PRECISA);
        return blade;
    }

    /** A bearer wearing host and carrying stone, in combat. */
    private static CharacterSheet bearer(final Item host, final PowerStone stone) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .equipment(new ArrayList<>(List.of(host)))
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.addToInventory(stone);
        sheet.startCombat();
        return sheet;
    }

    private static void passRounds(final CharacterSheet sheet, final int rounds) {
        for (int i = 0; i < rounds; i++) {
            sheet.tickTemporaryEffects();
            sheet.startNewRound();
        }
    }

    // --- catalog --------------------------------------------------------------------------------

    @Test
    void theV19RefinosCarryTheirTable() {
        assertEquals(List.of(5, 12, 20, 30), java.util.Arrays.stream(PowerStoneQuality.values())
                .map(PowerStoneQuality::getPrice).toList());
        assertEquals(List.of(6, 10, 20, 14), java.util.Arrays.stream(PowerStoneQuality.values())
                .map(PowerStoneQuality::getCharges).toList());
        assertEquals(ItemRarity.UNCOMMON, PowerStoneQuality.JOLDA.getRarity());
        assertTrue(PowerStoneQuality.RELIQUIA.reducesCooldownOnEarlyRemoval());
        assertEquals(17, PowerStoneType.values().length);
        assertEquals(7, PowerStoneImprovement.values().length);
    }

    @Test
    void aJoldaIsNoObraPrimaAndAnAprimoramentoNeedsOne() {
        assertThrows(IllegalArgumentException.class, () -> PowerStone.withMasterpiece(PowerStoneType.MITRAL_PURO,
                PowerStoneQuality.JOLDA, PowerStoneMasterpiece.REFINADA));
        assertThrows(IllegalArgumentException.class, () -> new PowerStoneSpecification(PowerStoneType.MITRAL_PURO,
                PowerStoneQuality.JOIA, null, PowerStoneImprovement.ARCANUM));
    }

    @Test
    void aStoneIsACarriedItemPricedByItsParts() {
        PowerStone stone = PowerStone.withMasterpieceAndImprovement(PowerStoneType.HEMATITA_DO_VENDAVAL,
                PowerStoneQuality.JOIA, PowerStoneMasterpiece.CARGA_EXTRA, PowerStoneImprovement.CONEXAO_VELOZ);
        assertEquals(ItemCategory.POWER_STONE, stone.getCategory());
        assertEquals("Hematita do Vendaval (Joia)", stone.getName());
        assertEquals(12 + 9 + 10, stone.getPrice());
        assertEquals(15, stone.getRemainingCharges());   // 10 +50%
    }

    @Test
    void refinadaNeverTakesTheDanosBelowOne() {
        assertEquals(1, PowerStone.withMasterpiece(PowerStoneType.MITRAL_PURO, PowerStoneQuality.RELIQUIA,
                PowerStoneMasterpiece.REFINADA).getEffectiveBindingDamage());
        assertEquals(2, PowerStone.withMasterpiece(PowerStoneType.MITRAL_PURO, PowerStoneQuality.AETHERNUM,
                PowerStoneMasterpiece.REFINADA).getEffectiveBindingDamage());
    }

    // --- who can host ---------------------------------------------------------------------------

    @Test
    void onlyAnObraPrimaArmaArmaduraOrEscudoHostsAStone() {
        PowerStone stone = PowerStone.of(PowerStoneType.MITRAL_PURO, PowerStoneQuality.JOIA);
        assertTrue(masterpieceArmor().acceptsPowerStone(stone));
        assertTrue(masterpieceBlade().acceptsPowerStone(stone));
        assertFalse(AbstractItem.builder().name("Peitoral").category(ItemCategory.ARMOR).build()
                .acceptsPowerStone(stone));
        AbstractItem boots = AbstractItem.builder().name("Botas").category(ItemCategory.BOOTS).build();
        boots.setMasterpiece(ItemMasterpiece.of(DefensiveMasterpiece.REFORCADA));
        assertFalse(boots.acceptsPowerStone(stone));
    }

    @Test
    void anEsferaDeAetherBindsToNoOrdinaryWeapon() {
        PowerStone esfera = PowerStone.of(PowerStoneType.ESFERA_DE_AETHER, PowerStoneQuality.JOIA);
        assertFalse(masterpieceBlade().acceptsPowerStone(esfera));
        assertTrue(masterpieceArmor().acceptsPowerStone(esfera));
    }

    // --- the Vinculação cycle -------------------------------------------------------------------

    @Test
    void socketingIsCombatOnly() {
        AbstractItem armor = masterpieceArmor();
        PowerStone stone = PowerStone.of(PowerStoneType.HEMATITA_DO_VENDAVAL, PowerStoneQuality.JOIA);
        CharacterSheet sheet = bearer(armor, stone);
        sheet.endCombat();

        assertTrue(service.readyStones(sheet).isEmpty());
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> service.bind(sheet, stone, armor, false));
        assertEquals("POWER_STONE_NOT_IN_COMBAT", refused.getMessage());
    }

    @Test
    void bindingSpendsACargaDamagesTheHostAndTurnsTheStoneOn() {
        AbstractItem armor = masterpieceArmor();
        PowerStone stone = PowerStone.of(PowerStoneType.HEMATITA_DO_VENDAVAL, PowerStoneQuality.JOIA);
        CharacterSheet sheet = bearer(armor, stone);
        int baseMovement = new MovementServiceImpl().getMovementBase(sheet.getCharacter());
        int baseDexterity = sheet.getCharacter().getEffectiveAttributeTotal(AttributeDomain.DEXTERITY);

        assertEquals(List.of(stone), service.readyStones(sheet));
        PowerStoneService.BindResult result = service.bind(sheet, stone, armor, false);

        assertEquals(ActionCost.ofActionPoints(1), result.cost());
        assertEquals(9, stone.getRemainingCharges());
        assertEquals(2, result.hostDamage());
        assertEquals(2, armor.getDamageTaken());
        assertSame(stone, armor.getPowerStone());
        assertEquals(baseMovement + 3, new MovementServiceImpl().getMovementBase(sheet.getCharacter()));
        assertEquals(baseDexterity + 1, sheet.getCharacter().getEffectiveAttributeTotal(AttributeDomain.DEXTERITY));
    }

    @Test
    void conexaoVelozBindsAsAFreeAction() {
        PowerStone stone = PowerStone.withMasterpieceAndImprovement(PowerStoneType.MITRAL_PURO,
                PowerStoneQuality.JOIA, PowerStoneMasterpiece.REFINADA, PowerStoneImprovement.CONEXAO_VELOZ);
        assertEquals(ActionCost.FREE_ACTION, service.getBindCost(stone));
    }

    @Test
    void whenTheDuracaoEndsStoneAndHostCoolDown() {
        AbstractItem armor = masterpieceArmor();
        PowerStone stone = PowerStone.of(PowerStoneType.MITRAL_PURO, PowerStoneQuality.JOIA); // Duração 3, Resfr. 4
        CharacterSheet sheet = bearer(armor, stone);
        service.bind(sheet, stone, armor, false);

        passRounds(sheet, 3);

        assertNull(armor.getPowerStone());
        assertTrue(sheet.getPowerStoneBindings().isEmpty());
        assertTrue(sheet.getRemainingItemCooldown(stone) > 0);
        assertTrue(sheet.getRemainingItemCooldown(armor) > 0);
        assertTrue(service.readyStones(sheet).isEmpty());
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> service.bind(sheet, stone, armor, false));
        assertEquals("POWER_STONE_COOLING_DOWN", refused.getMessage());

        passRounds(sheet, 4);
        assertEquals(List.of(stone), service.readyStones(sheet));
    }

    @Test
    void aReliquiaRemovedEarlyShortensTheHostsCooling() {
        AbstractItem armor = masterpieceArmor();
        PowerStone stone = PowerStone.of(PowerStoneType.MITRAL_PURO, PowerStoneQuality.RELIQUIA); // Dur. 3, Resfr. 3
        CharacterSheet sheet = bearer(armor, stone);
        service.bind(sheet, stone, armor, false);

        service.unbind(sheet, armor);   // all 3 Rodadas left: 3 - 3*3/3 = 0

        assertNull(armor.getPowerStone());
        assertEquals(0, sheet.getRemainingItemCooldown(armor));
        assertEquals(3, sheet.getRemainingItemCooldown(stone));
    }

    @Test
    void combatEndingClearsBindingsAndCoolingButNotSpentCargas() {
        AbstractItem armor = masterpieceArmor();
        PowerStone stone = PowerStone.of(PowerStoneType.MITRAL_PURO, PowerStoneQuality.JOIA);
        CharacterSheet sheet = bearer(armor, stone);
        service.bind(sheet, stone, armor, false);

        sheet.endCombat();

        assertNull(armor.getPowerStone());
        assertTrue(sheet.getPowerStoneBindings().isEmpty());
        assertEquals(0, sheet.getRemainingItemCooldown(armor));
        assertEquals(9, stone.getRemainingCharges());
    }

    @Test
    void mecanismoDeSobrecargaSpendsThreeAndDoublesDuracaoAndCooling() {
        AbstractItem armor = masterpieceArmor();
        PowerStone stone = PowerStone.withMasterpieceAndImprovement(PowerStoneType.MITRAL_PURO,
                PowerStoneQuality.JOIA, PowerStoneMasterpiece.REFINADA, PowerStoneImprovement.MECANISMO_DE_SOBRECARGA);
        CharacterSheet sheet = bearer(armor, stone);

        PowerStoneBinding binding = service.bind(sheet, stone, armor, true).binding();

        assertEquals(7, stone.getRemainingCharges());
        assertEquals(6, binding.getDurationRounds());
        assertEquals(8, binding.getCooldownRounds());
        assertThrows(IllegalOperationException.class, () -> service.bind(bearer(masterpieceArmor(),
                PowerStone.of(PowerStoneType.MITRAL_PURO, PowerStoneQuality.JOIA)), stone, armor, true));
    }

    @Test
    void ultimaCargaGivesOneLastDoubledUse() {
        AbstractItem armor = masterpieceArmor();
        PowerStone stone = PowerStone.restored(PowerStoneType.MITRAL_PURO, PowerStoneQuality.JOIA,
                PowerStoneMasterpiece.CARGA_EXTRA, PowerStoneImprovement.ULTIMA_CARGA, 0, false);
        CharacterSheet sheet = bearer(armor, stone);

        PowerStoneService.BindResult result = service.bind(sheet, stone, armor, false);

        assertEquals(4, result.hostDamage());                       // Danos 2, doubled
        assertEquals(PowerStone.LAST_CHANCE_DURATION_ROUNDS, result.binding().getDurationRounds());
        assertTrue(stone.isLastChanceUsed());
        assertFalse(stone.isUsable());
    }

    @Test
    void anEmptyStoneCannotBind() {
        AbstractItem armor = masterpieceArmor();
        PowerStone stone = PowerStone.restored(PowerStoneType.MITRAL_PURO, PowerStoneQuality.JOIA, null, null, 0, false);
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> service.bind(bearer(armor, stone), stone, armor, false));
        assertEquals("POWER_STONE_NO_CHARGES", refused.getMessage());
    }

    @Test
    void solveVidasSendsTheFirstBindingOfTheCenaToTheBearer() {
        AbstractItem armor = masterpieceArmor();
        PowerStone stone = PowerStone.withMasterpiece(PowerStoneType.MITRAL_PURO, PowerStoneQuality.JOIA,
                PowerStoneMasterpiece.SOLVE_VIDAS);
        CharacterSheet sheet = bearer(armor, stone);

        PowerStoneService.BindResult first = service.bind(sheet, stone, armor, false);
        assertEquals(2, first.bearerDamage());
        assertEquals(0, armor.getDamageTaken());

        sheet.endCombat();
        sheet.startCombat();
        PowerStoneService.BindResult second = service.bind(sheet, stone, armor, false);
        assertEquals(2, second.hostDamage());
    }

    @Test
    void aRegaliaTakesNoDanos() {
        AbstractItem regalia = masterpieceArmor();
        regalia.setRegaliaGrade(RegaliaGrade.MENOR);
        PowerStone stone = PowerStone.of(PowerStoneType.MITRAL_PURO, PowerStoneQuality.JOLDA);
        PowerStoneService.BindResult result = service.bind(bearer(regalia, stone), stone, regalia, false);
        assertEquals(0, result.hostDamage());
        assertEquals(0, regalia.getDamageTaken());
    }

    // --- spending Cargas otherwise --------------------------------------------------------------

    @Test
    void vitalitasImmanisHealsTwoPvPerCarga() {
        AbstractItem armor = masterpieceArmor();
        PowerStone stone = PowerStone.withMasterpieceAndImprovement(PowerStoneType.MITRAL_PURO,
                PowerStoneQuality.JOIA, PowerStoneMasterpiece.REFINADA, PowerStoneImprovement.VITALITAS_IMMANIS);
        CharacterSheet sheet = bearer(armor, stone);
        sheet.applyDamage(5);

        assertEquals(4, service.heal(sheet, stone, 2));
        assertEquals(8, stone.getRemainingCharges());
        assertThrows(IllegalOperationException.class, () -> service.heal(sheet,
                PowerStone.of(PowerStoneType.MITRAL_PURO, PowerStoneQuality.JOIA), 1));
    }

    @Test
    void arcanumDrawsCargasAsPm() {
        AbstractItem armor = masterpieceArmor();
        PowerStone stone = PowerStone.withMasterpieceAndImprovement(PowerStoneType.MITRAL_PURO,
                PowerStoneQuality.JOIA, PowerStoneMasterpiece.REFINADA, PowerStoneImprovement.ARCANUM);
        CharacterSheet sheet = bearer(armor, stone);

        assertEquals(3, service.drawCharges(sheet, stone, PowerStoneImprovement.ARCANUM, 3));
        assertThrows(IllegalOperationException.class,
                () -> service.drawCharges(sheet, stone, PowerStoneImprovement.ANIMA_SELVAGEM, 1));
    }

    // --- effects reaching the services ----------------------------------------------------------

    @Test
    void relampagoDouradoOnAWeaponGrantsAPa() {
        AbstractWeapon blade = masterpieceBlade();
        PowerStone stone = PowerStone.of(PowerStoneType.RELAMPAGO_DOURADO, PowerStoneQuality.JOIA);
        CharacterSheet sheet = bearer(blade, stone);
        int before = new ActionPointsServiceImpl().getMaxActionPoints(sheet, 0);

        service.bind(sheet, stone, blade, false);

        assertEquals(before + 1, new ActionPointsServiceImpl().getMaxActionPoints(sheet, 0));
    }

    @Test
    void calcitaGrantsResistenciaACriticos() {
        AbstractItem armor = masterpieceArmor();
        PowerStone stone = PowerStone.of(PowerStoneType.CALCITA_VULCANICA, PowerStoneQuality.JOIA);
        CharacterSheet sheet = bearer(armor, stone);
        int before = sheet.getTotalCriticalResistance(null);

        service.bind(sheet, stone, armor, false);

        assertEquals(before + 2, sheet.getTotalCriticalResistance(null));
    }

    @Test
    void mitralsViolentMarginWidensOnlyItsOwnWeapon() {
        AbstractWeapon blade = masterpieceBlade();
        blade.setPowerStone(PowerStone.of(PowerStoneType.MITRAL_PURO, PowerStoneQuality.JOIA));
        AbstractWeapon other = masterpieceBlade();

        assertEquals(1, blade.resolveEnhancementCriticalMarginIncrease(blade, null)
                - masterpieceBlade().resolveEnhancementCriticalMarginIncrease(masterpieceBlade(), null));
        assertEquals(0, blade.resolveEnhancementCriticalMarginIncrease(other, null));
        assertEquals(1, blade.resolveEnhancementMajorCriticalMarginIncrease(blade, null));
    }

    @Test
    void sombraSolidificadaRaisesItsWeaponsDanoBase() {
        AbstractWeapon blade = masterpieceBlade();
        blade.setPowerStone(PowerStone.of(PowerStoneType.SOMBRA_SOLIDIFICADA, PowerStoneQuality.JOIA));
        assertEquals(1, PowerStoneType.SOMBRA_SOLIDIFICADA.resolveDamageBaseIncrease(blade, ItemType.OFFENSIVE));
        assertEquals(0, PowerStoneType.SOMBRA_SOLIDIFICADA.resolveDamageBaseIncrease(blade, ItemType.DEFENSIVE));
    }

    // --- the store ------------------------------------------------------------------------------

    @Test
    void aStoreSellsStonesUpToItsCeiling() {
        ItemStore store = new ItemStore(ItemRarity.RARE);
        assertEquals(List.of(PowerStoneQuality.JOLDA, PowerStoneQuality.JOIA), store.getOfferedPowerStoneQualities());

        CharacterSheet buyer = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>()).build(), new Player());
        buyer.grantEquipmentPoints(30);
        PowerStone bought = new ItemPurchaseServiceImpl().purchase(store,
                PowerStoneSpecification.of(PowerStoneType.SODALITA_GELIDA, PowerStoneQuality.JOIA), buyer);

        assertEquals(18, buyer.getEquipmentPoints());
        assertTrue(buyer.getInventory().contains(bought));
        assertEquals(10, bought.getRemainingCharges());
        assertThrows(IllegalOperationException.class, () -> new ItemPurchaseServiceImpl().purchase(store,
                PowerStoneSpecification.of(PowerStoneType.SODALITA_GELIDA, PowerStoneQuality.RELIQUIA), buyer));
    }
}
