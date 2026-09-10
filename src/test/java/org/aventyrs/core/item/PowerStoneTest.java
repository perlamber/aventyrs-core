package org.aventyrs.core.item;

import java.util.List;
import java.util.Map;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageBaseService;
import org.aventyrs.core.character.services.DamageBaseServiceImpl;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.character.services.MovementService;
import org.aventyrs.core.character.services.MovementServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.atletismo.AtletismoInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerStoneTest {

    private final DefenseService defenseService = new DefenseServiceImpl();
    private final DamageService damageService = new DamageServiceImpl();
    private final MovementService movementService = new MovementServiceImpl();
    private final DamageBaseService damageBaseService = new DamageBaseServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    // --- Tri-modal resolution ---------------------------------------------------------------

    @Test
    void efeitoBaseAppliesRegardlessOfHostType() {
        // Rútilo Subterrâneo's Efeito Base grants Defesas +2.
        assertEquals(2, PowerStoneType.RUTILO_SUBTERRANEO.resolveBonus(ModifierType.DEFESAS, ItemType.DEFENSIVE));
        assertEquals(2, PowerStoneType.RUTILO_SUBTERRANEO.resolveBonus(ModifierType.DEFESAS, ItemType.OFFENSIVE));
        assertEquals(2, PowerStoneType.RUTILO_SUBTERRANEO.resolveBonus(ModifierType.DEFESAS, ItemType.UTILITY));
    }

    @Test
    void efeitoDefensivoAppliesOnlyOnADefensiveHost() {
        // Sombra Solidificada: Efeito Defensivo is RD 1, Efeito Base grants nothing.
        assertEquals(1, PowerStoneType.SOMBRA_SOLIDIFICADA.resolveBonus(ModifierType.DAMAGE_REDUCTION, ItemType.DEFENSIVE));
        assertEquals(0, PowerStoneType.SOMBRA_SOLIDIFICADA.resolveBonus(ModifierType.DAMAGE_REDUCTION, ItemType.OFFENSIVE));
    }

    @Test
    void efeitoOfensivoDamageBaseIncreaseAppliesOnlyOnAnOffensiveHost() {
        assertEquals(1, PowerStoneType.SOMBRA_SOLIDIFICADA.resolveDamageBaseIncrease(null, ItemType.OFFENSIVE));
        assertEquals(0, PowerStoneType.SOMBRA_SOLIDIFICADA.resolveDamageBaseIncrease(null, ItemType.DEFENSIVE));
        assertEquals(0, PowerStoneType.SOMBRA_SOLIDIFICADA.resolveBonus(ModifierType.DAMAGE_REDUCTION, ItemType.OFFENSIVE));
    }

    // --- End-to-end wiring through the services --------------------------------------------

    @Test
    void movimentoBaseEfeitoBaseReachesMovementService() {
        Character character = characterWith(socketed(PowerStone.of(
                PowerStoneType.HEMATITA_DO_VENDAVAL, PowerStoneQuality.JOIA)));

        assertEquals(6, movementService.getMovementBase(character)); // BLANK base 4 + 2
    }

    @Test
    void defesasEfeitoBaseReachesDefenseServiceForBothPools() {
        Character character = characterWith(socketed(PowerStone.of(
                PowerStoneType.RUTILO_SUBTERRANEO, PowerStoneQuality.RELIQUIA)));

        assertEquals(2, defenseService.getTotalDefense(character, DefenseType.PHYSICAL));
        assertEquals(2, defenseService.getTotalDefense(character, DefenseType.MAGIC));
    }

    @Test
    void danoReducaoEfeitoDefensivoReachesDamageService() {
        Character character = characterWith(socketed(PowerStone.of(
                PowerStoneType.SOMBRA_SOLIDIFICADA, PowerStoneQuality.JOIA)));

        assertEquals(1, damageService.getTotalDamageReduction(character));
    }

    @Test
    void vantagemAtletismoEfeitoBaseReachesTheRoll() {
        AbstractItem bare = AbstractItem.builder().name("Peitoral").category(ItemCategory.ARMOR).build();
        bare.addImprovement(ItemImprovement.of(DefensiveImprovement.ENCAIXE));
        int baseline = atletismoBonus(characterWith(bare));

        AbstractItem socketed = AbstractItem.builder().name("Peitoral").category(ItemCategory.ARMOR).build();
        socketed.addImprovement(ItemImprovement.of(DefensiveImprovement.ENCAIXE));
        socketed.setPowerStone(PowerStone.of(PowerStoneType.RUTILO_SUBTERRANEO, PowerStoneQuality.RELIQUIA));

        assertEquals(baseline + Skill.ADVANTAGE_BONUS, atletismoBonus(characterWith(socketed)));
    }

    private int atletismoBonus(final Character character) {
        return new AtletismoInteraction().applyTo(CharacterSheet.of(character, new Player())).getSkillRollBonus();
    }

    @Test
    void danoBaseEfeitoOfensivoReachesDamageBaseServiceThroughAWeaponHost() {
        // A weapon cannot take the Encaixe Aprimoramento yet, so the socket is builder-set here
        // (a Builder-bypassable invariant); the Efeito Ofensivo still flows with no rewiring.
        AbstractWeapon blade = AbstractWeapon.builder()
                .name("Lâmina Umbral")
                .category(ItemCategory.LIGHT_BLADE)
                .damageBase(DamageBase.of(1, 1))
                .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
                .powerStone(PowerStone.of(PowerStoneType.SOMBRA_SOLIDIFICADA, PowerStoneQuality.JOIA))
                .build();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).equipment(List.of(blade)).build();

        assertEquals(DamageBase.of(1, 1).scaledUp(1), damageBaseService.getDamageBase(character, blade));
    }

    @Test
    void aDestroyedHostStopsGrantingItsStone() {
        AbstractItem armor = AbstractItem.builder()
                .name("Peitoral Encaixado").category(ItemCategory.ARMOR).hardness(1).build();
        armor.addImprovement(ItemImprovement.of(DefensiveImprovement.ENCAIXE));
        armor.setPowerStone(PowerStone.of(PowerStoneType.RUTILO_SUBTERRANEO, PowerStoneQuality.RELIQUIA));
        Character character = characterWith(armor);

        assertEquals(2, defenseService.getTotalDefense(character, DefenseType.PHYSICAL));

        armor.applyDamage(5);

        assertTrue(armor.isDestroyed());
        assertEquals(0, defenseService.getTotalDefense(character, DefenseType.PHYSICAL));
    }

    // --- Encaixe gating and PowerStone construction ---------------------------------------

    @Test
    void socketingRequiresTheEncaixeAprimoramento() {
        AbstractItem bare = AbstractItem.builder().name("Peitoral").category(ItemCategory.ARMOR).build();
        assertThrows(IllegalArgumentException.class, () -> bare.setPowerStone(
                PowerStone.of(PowerStoneType.SOMBRA_SOLIDIFICADA, PowerStoneQuality.JOIA)));

        bare.addImprovement(ItemImprovement.of(DefensiveImprovement.ENCAIXE));
        bare.setPowerStone(PowerStone.of(PowerStoneType.SOMBRA_SOLIDIFICADA, PowerStoneQuality.JOIA));
        assertEquals(PowerStoneType.SOMBRA_SOLIDIFICADA, bare.getPowerStone().getType());
    }

    @Test
    void aJoldaStoneCannotBeAnObraPrima() {
        assertThrows(IllegalArgumentException.class, () -> PowerStone.withMasterpiece(
                PowerStoneType.SOMBRA_SOLIDIFICADA, PowerStoneQuality.JOLDA, PowerStoneMasterpiece.CARGA_EXTRA));
        assertEquals(PowerStoneMasterpiece.CARGA_EXTRA, PowerStone.withMasterpiece(
                PowerStoneType.SOMBRA_SOLIDIFICADA, PowerStoneQuality.JOIA, PowerStoneMasterpiece.CARGA_EXTRA)
                .getMasterpiece());
    }

    // --- The charge economy, folded with the Obra-Prima deltas ---------------------------

    @Test
    void obraPrimaDeltasFoldIntoTheEffectiveChargeEconomy() {
        PowerStone cargaExtra = PowerStone.withMasterpiece(
                PowerStoneType.ADAMANTE_BRUTO, PowerStoneQuality.RELIQUIA, PowerStoneMasterpiece.CARGA_EXTRA);
        assertEquals(30, cargaExtra.getEffectiveCharges()); // Relíquia 20 + 50%

        PowerStone estendida = PowerStone.withMasterpiece(
                PowerStoneType.ADAMANTE_BRUTO, PowerStoneQuality.JOIA, PowerStoneMasterpiece.DURACAO_ESTENDIDA);
        assertEquals(5, estendida.getEffectiveEffectDurationRounds()); // Joia 3 + 2
        assertEquals(5, estendida.getEffectiveCooldownRounds());       // Joia 4 + 1

        PowerStone rapida = PowerStone.withMasterpiece(
                PowerStoneType.ADAMANTE_BRUTO, PowerStoneQuality.JOIA, PowerStoneMasterpiece.RESFRIAMENTO_RAPIDO);
        assertEquals(3, rapida.getEffectiveCooldownRounds());          // Joia 4 - 1

        PowerStone refinada = PowerStone.withMasterpiece(
                PowerStoneType.ADAMANTE_BRUTO, PowerStoneQuality.RELIQUIA, PowerStoneMasterpiece.REFINADA);
        assertEquals(1, refinada.getEffectiveBindingDamage());         // Relíquia 2 - 1
        assertFalse(refinada.bindsToBearerInsteadOfItem());

        assertTrue(PowerStone.withMasterpiece(PowerStoneType.ADAMANTE_BRUTO, PowerStoneQuality.RELIQUIA,
                PowerStoneMasterpiece.SOLVE_VIDAS).bindsToBearerInsteadOfItem());
        assertTrue(PowerStone.withImprovement(PowerStoneType.ADAMANTE_BRUTO, PowerStoneQuality.JOIA,
                PowerStoneImprovement.CONEXAO_VELOZ).bindsAsFreeAction());
    }

    // --- helpers ------------------------------------------------------------------------------

    private AbstractItem socketed(final PowerStone stone) {
        AbstractItem armor = AbstractItem.builder()
                .name("Peitoral Encaixado").category(ItemCategory.ARMOR).build();
        armor.addImprovement(ItemImprovement.of(DefensiveImprovement.ENCAIXE));
        armor.setPowerStone(stone);
        return armor;
    }

    private Character characterWith(final AbstractItem item) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.of(Map.of(AttributeDomain.DEXTERITY, 3)))
                .equipment(List.of(item))
                .build();
    }
}
