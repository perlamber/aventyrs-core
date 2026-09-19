package org.aventyrs.core.item;

import java.util.List;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageBaseService;
import org.aventyrs.core.character.services.DamageBaseServiceImpl;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.furtividade.FurtividadeInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OffensiveImprovementTest {

    private final DefenseService defenseService = new DefenseServiceImpl();
    private final DamageBaseService damageBaseService = new DamageBaseServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void catalogsEveryOffensiveImprovementAndItsAuthoredColumns() {
        assertEquals(18, OffensiveImprovement.values().length);
        assertEquals(1, OffensiveImprovement.GUARDA_MAOS.getPhysicalDefenseBonus());
        assertEquals(1, OffensiveImprovement.BENCAO_DE_PROTECAO.getMagicDefenseBonus());
        assertEquals(1, OffensiveImprovement.MANOPLA_DE_SEGURANCA.getAttackBonus());
        assertEquals(1, OffensiveImprovement.BENCAO_YMIRIANA.getDamageBonus());
        assertEquals(1, OffensiveImprovement.ARCANISTA.getCastingBonus());
    }

    @Test
    void isPricedOffTheArmasColumn() {
        assertEquals(EnhancementPriceCategory.WEAPON, OffensiveImprovement.GUARDA_MAOS.getPriceCategory());

        assertEquals(4, OffensiveImprovement.GUARDA_MAOS.getPriceModifier());       // Comum
        assertEquals(8, OffensiveImprovement.OCULTA.getPriceModifier());            // Incomum
        assertEquals(13, OffensiveImprovement.ARCANISTA.getPriceModifier());        // Raro
        assertEquals(19, OffensiveImprovement.ENCAIXE.getPriceModifier());          // Épico
        assertEquals(26, OffensiveImprovement.SOLVE_VIDAS.getPriceModifier());      // Mítico
    }

    @Test
    void guardaMaosAndBencaoDeProtecaoAddTheirDefensesFromTheWeaponTheyAreFittedTo() {
        assertEquals(1, defenseService.getTotalDefense(
                characterWith(weaponWith(OffensiveImprovement.GUARDA_MAOS)), DefenseType.PHYSICAL));
        assertEquals(0, defenseService.getTotalDefense(
                characterWith(weaponWith(OffensiveImprovement.GUARDA_MAOS)), DefenseType.MAGIC));

        Character blessed = characterWith(weaponWith(OffensiveImprovement.BENCAO_DE_PROTECAO));
        assertEquals(1, defenseService.getTotalDefense(blessed, DefenseType.PHYSICAL));
        assertEquals(1, defenseService.getTotalDefense(blessed, DefenseType.MAGIC));
    }

    @Test
    void ocultaGrantsFurtividadeAdvantageAndLeaksIntoNoOtherSkill() {
        int baseline = furtividadeBonus(characterWith(weaponWith(null)));

        assertEquals(baseline + Skill.ADVANTAGE_BONUS,
                furtividadeBonus(characterWith(weaponWith(OffensiveImprovement.OCULTA))));
        assertEquals(0, OffensiveImprovement.OCULTA.resolveBonus(
                SkillType.ATLETISMO.getRollBonusType(), SkillType.ATLETISMO,
                characterWith(weaponWith(null))));
    }

    private int furtividadeBonus(final Character character) {
        return new FurtividadeInteraction()
                .applyTo(CharacterSheet.of(character, new Player())).getSkillRollBonus();
    }

    @Test
    void bencaoYmirianaRaisesOnlyItsOwnHostDanoBase() {
        AbstractWeapon blessed = weaponWith(OffensiveImprovement.BENCAO_YMIRIANA);
        AbstractWeapon plain = weaponWith(null);
        Character character = characterWith(blessed, plain);

        assertEquals(DamageBase.of(2, 1), damageBaseService.getDamageBase(character, blessed));
        assertEquals(DamageBase.of(2, 0), damageBaseService.getDamageBase(character, plain));
    }

    /** The clause {@code Weapon#isDisarmable()} has been waiting on since it was written. */
    @Test
    void manoplaDeSegurancaMakesItsWeaponUndisarmable() {
        assertTrue(weaponWith(null).isDisarmable());
        assertFalse(weaponWith(OffensiveImprovement.MANOPLA_DE_SEGURANCA).isDisarmable());
    }

    /**
     * The offensive Encaixe is what finally lets a Pedra do Poder be socketed into a weapon — and
     * therefore the first host that selects a stone's <em>Efeito Ofensivo</em> mode without the
     * builder bypassing the socket.
     */
    @Test
    void encaixeLetsAWeaponHostAPowerStoneAndReachItsOffensiveMode() {
        AbstractWeapon bare = weaponWith(null);
        assertThrows(IllegalArgumentException.class, () -> bare.setPowerStone(
                PowerStone.of(PowerStoneType.SOMBRA_SOLIDIFICADA, PowerStoneQuality.JOIA)));

        AbstractWeapon socketable = weaponWith(OffensiveImprovement.ENCAIXE);
        socketable.setPowerStone(PowerStone.of(PowerStoneType.SOMBRA_SOLIDIFICADA, PowerStoneQuality.JOIA));

        assertNotNull(socketable.getPowerStone());
        assertEquals(DamageBase.of(2, 1), damageBaseService.getDamageBase(
                characterWith(socketable), socketable));
    }

    @Test
    void anOffensiveImprovementCannotBeFittedToADefensiveItem() {
        AbstractItem armadura = AbstractItem.builder()
                .name("Peitoral").category(ItemCategory.ARMOR).build();

        assertThrows(IllegalArgumentException.class,
                () -> armadura.addImprovement(OffensiveImprovement.GUARDA_MAOS));
    }

    /** The two constants whose Características Adicionais say "Apenas …" outright. */
    @Test
    void theTwoRestrictedImprovementsRefuseAnUnsuitableWeapon() {
        AbstractWeapon heavyMelee = AbstractWeapon.builder()
                .name("Espada Pesada")
                .category(ItemCategory.HEAVY_BLADE)
                .weightClass(ItemWeightClass.HEAVY)
                .damageBase(DamageBase.of(2, 0))
                .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> heavyMelee.addImprovement(OffensiveImprovement.CORRENTE_COM_PESO));
        assertThrows(IllegalArgumentException.class,
                () -> heavyMelee.addImprovement(OffensiveImprovement.PENTE_ALONGADO));

        weaponWith(OffensiveImprovement.CORRENTE_COM_PESO); // Leve — accepted
        AbstractWeapon besta = AbstractWeapon.builder()
                .name("Besta")
                .category(ItemCategory.CROSSBOW)
                .weightClass(ItemWeightClass.MEDIUM)
                .damageBase(DamageBase.of(2, 0))
                .skillType(SkillType.ATAQUE_A_DISTANCIA)
                .build();
        besta.addImprovement(OffensiveImprovement.PENTE_ALONGADO);
        assertEquals(1, besta.getImprovements().size());
    }

    /** Nine names recur across the two Aprimoramento lists and agree on nothing but the name. */
    @Test
    void sharesNamesWithTheDefensiveCatalogAndNothingElse() {
        assertEquals(DefensiveImprovement.ENCAIXE.getName(), OffensiveImprovement.ENCAIXE.getName());
        assertEquals(1, OffensiveImprovement.BENCAO_DE_PROTECAO.getPhysicalDefenseBonus());
        assertEquals(ItemRarity.UNCOMMON, OffensiveImprovement.BENCAO_YMIRIANA.getRarity());
        assertEquals(ItemRarity.RARE, DefensiveImprovement.BENCAO_YMIRIANA.getRarity());
    }

    private AbstractWeapon weaponWith(final OffensiveImprovement improvement) {
        AbstractWeapon weapon = AbstractWeapon.builder()
                .name("Arma de teste")
                .category(ItemCategory.LIGHT_BLADE)
                .weightClass(ItemWeightClass.LIGHT)
                .damageBase(DamageBase.of(2, 0))
                .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
                .build();
        if (improvement != null) {
            weapon.addImprovement(improvement);
        }
        return weapon;
    }

    private Character characterWith(final Item... equipment) {
        return CharacterFixture.blank(CharacterFixture.BLANK).equipment(List.of(equipment)).build();
    }
}
