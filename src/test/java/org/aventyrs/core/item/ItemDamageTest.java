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
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.furtividade.FurtividadeInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Damage dealt to a piece of Equipamento itself, and what a destroyed one stops granting.
 *
 * <p>Nothing in main source calls {@link Item#applyDamage(int)} automatically yet: every clause
 * that deals it — {@code StrengthAbility#ESTILHACADOR}, {@code DuelistaFeat
 * #DEFENDER_SE_ATACANDO}, the Estilhaçador/Sabotar/Repelir e Suprimir/Retorno de Danos Efeitos
 * Críticos — is blocked on a further missing system of its own. The mechanism they all cite is
 * what these tests pin, the same way {@code MoralHerdadaAbility#applyStartingFama} is real and
 * tested with no automatic caller.
 */
class ItemDamageTest {

    private final DefenseService defenseService = new DefenseServiceImpl();
    private final DamageService damageService = new DamageServiceImpl();
    private final DamageBaseService damageBaseService = new DamageBaseServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void applyDamageSpendsTheItemPvWithoutTouchingItsAuthoredMaximum() {
        AbstractItem armor = armor(10, null);

        assertEquals(4, armor.applyDamage(4));

        assertEquals(4, armor.getDamageTaken());
        assertEquals(6, armor.getCurrentHardness());
        assertEquals(10, armor.getEffectiveHardness());
        assertFalse(armor.isDestroyed());
    }

    @Test
    void anItemIsDestroyedOnlyOnceDamageHasActuallyReducedItToZeroPv() {
        AbstractItem armor = armor(10, null);
        AbstractItem pristineWithNoHardnessAtAll = armor(0, null);

        armor.applyDamage(9);
        assertFalse(armor.isDestroyed());

        armor.applyDamage(1);
        assertTrue(armor.isDestroyed());
        assertEquals(0, armor.getCurrentHardness());

        assertFalse(pristineWithNoHardnessAtAll.isDestroyed());
        pristineWithNoHardnessAtAll.applyDamage(1);
        assertTrue(pristineWithNoHardnessAtAll.isDestroyed());
    }

    @Test
    void damageAccumulatesPastZeroSoARaisedMaximumCannotUndestroyAWreck() {
        AbstractItem armor = armor(10, null);

        armor.applyDamage(25);
        armor.setImprovement(ItemImprovement.of(DefensiveImprovement.RESISTENTE));

        assertEquals(25, armor.getDamageTaken());
        assertEquals(20, armor.getEffectiveHardness());
        assertTrue(armor.isDestroyed());
    }

    @Test
    void resistenteRaisesTheItemMaximumPvAndBlocksOnePointOfEveryDamageInstanceDealtToIt() {
        AbstractItem resistant = armor(10, ItemImprovement.of(DefensiveImprovement.RESISTENTE));

        assertEquals(1, resistant.getItemDamageReduction());
        assertEquals(20, resistant.getEffectiveHardness());

        assertEquals(4, resistant.applyDamage(5));
        assertEquals(4, resistant.applyDamage(5));

        assertEquals(12, resistant.getCurrentHardness());
    }

    @Test
    void mitigationFloorsAtZeroSoAFullyBlockedHitLeavesTheItemPristine() {
        AbstractItem plain = armor(10, null);
        AbstractItem resistant = armor(10, ItemImprovement.of(DefensiveImprovement.RESISTENTE));

        assertEquals(0, plain.getItemDamageReduction());
        assertEquals(0, plain.applyDamage(0));
        assertEquals(0, plain.getDamageTaken());

        assertEquals(0, resistant.applyDamage(1));
        assertEquals(0, resistant.getDamageTaken());
        assertEquals(20, resistant.getCurrentHardness());
    }

    @Test
    void aCatalogTemplateTakesNoDamageAtAllSinceEveryCopyWouldShareIt() {
        assertEquals(0, ArmorItem.ARMADURA_COMPLETA.applyDamage(50));

        assertEquals(0, ArmorItem.ARMADURA_COMPLETA.getDamageTaken());
        assertFalse(ArmorItem.ARMADURA_COMPLETA.isDestroyed());
        assertEquals(32, ArmorItem.ARMADURA_COMPLETA.getCurrentHardness());
    }

    @Test
    void aDestroyedItemStopsGrantingItsDefesasFavorAndEnhancementBonuses() {
        AbstractItem armor = AbstractItem.fromTemplate(ArmorItem.ARMADURA_COMPLETA);
        armor.setImprovement(ItemImprovement.of(DefensiveImprovement.OCULTA));
        Character character = characterWith(armor);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        assertEquals(5, defenseService.getTotalDefense(character, DefenseType.PHYSICAL));
        assertEquals(2, defenseService.getTotalDefense(character, DefenseType.MAGIC));
        assertEquals(2, damageService.getTotalDamageReduction(character));
        assertTrue(armor.grantsFavorTo(character));
        assertEquals(3, new FurtividadeInteraction().applyTo(sheet).getSkillRollBonus());

        armor.applyDamage(armor.getEffectiveHardness());

        assertEquals(0, defenseService.getTotalDefense(character, DefenseType.PHYSICAL));
        assertEquals(0, defenseService.getTotalDefense(character, DefenseType.MAGIC));
        assertEquals(0, damageService.getTotalDamageReduction(character));
        assertFalse(armor.grantsFavorTo(character));
        assertTrue(armor.resolveFavorBonuses(character).isEmpty());
        assertEquals(1, new FurtividadeInteraction().applyTo(sheet).getSkillRollBonus());
    }

    @Test
    void aDestroyedWeaponSwingsAsAnAtaqueDesarmadoWhileKeepingItsAuthoredColumn() {
        AbstractWeapon sword = AbstractWeapon.builder()
                .name("Espada de teste")
                .category(ItemCategory.HEAVY_BLADE)
                .hardness(8)
                .damageBase(DamageBase.of(2, 1))
                .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
                .build();
        Character character = characterWith(sword);

        assertEquals(DamageBase.of(2, 1), damageBaseService.getDamageBase(character, sword));

        sword.applyDamage(8);

        assertEquals(DamageBase.UNARMED, damageBaseService.getDamageBase(character, sword));
        assertEquals(DamageBase.of(2, 1), sword.getDamageBase());
    }

    @Test
    void aDestroyedItemStaysEquippedAsGarbageUntilItsOwnerRemovesIt() {
        AbstractItem armor = AbstractItem.fromTemplate(ArmorItem.ARMADURA_COMPLETA);
        Character character = characterWith(armor);

        armor.applyDamage(armor.getEffectiveHardness());

        assertEquals(1, character.getEquipment().size());
        assertSame(armor, character.getEquipment().get(0));
        assertEquals("Armadura Completa", armor.getName());
        assertEquals(ItemWeightClass.HEAVY, armor.getEffectiveWeightClass());
    }

    private AbstractItem armor(final int hardness, final ItemImprovement improvement) {
        AbstractItem item = AbstractItem.builder()
                .name("Item de teste")
                .category(ItemCategory.ARMOR)
                .weightClass(ItemWeightClass.MEDIUM)
                .hardness(hardness)
                .build();
        if (improvement != null) {
            item.setImprovement(improvement);
        }
        return item;
    }

    private Character characterWith(final AbstractItem item) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.of(
                        Map.of(AttributeDomain.STRENGTH, 3, AttributeDomain.DEXTERITY, 3)))
                .equipment(List.of(item))
                .build();
    }
}
