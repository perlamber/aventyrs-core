package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.ArtesMarciaisFeat;
import org.aventyrs.core.item.AbstractItem;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpo;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * How many targets one attack may name — {@code ARTE_FLUIDA}'s "seus ataques afetam um alvo
 * adicional" judged through the service a caller actually asks, never through the hook itself.
 */
class AttackTargetingServiceImplTest {

    private final AttackTargetingService attackTargetingService = new AttackTargetingServiceImpl();

    private static final Weapon SWORD = AbstractWeapon.builder()
            .name("Espada Longa")
            .category(ItemCategory.HEAVY_BLADE)
            .damageBase(DamageBase.of(2, 0))
            .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
            .build();

    private static final Weapon CLAWS = AbstractWeapon.builder()
            .name("Garras Afiadas")
            .category(ItemCategory.NATURAL_WEAPON)
            .damageBase(DamageBase.of(1, 2))
            .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
            .build();

    private static final Item SHIELD = AbstractItem.builder()
            .name("Escudo Redondo")
            .category(ItemCategory.SHIELD)
            .build();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /** Ataque Corpo-a-Corpo 5 plus one Título Aventyr Desperto — exactly ARTE_FLUIDA's ladder. */
    private static Character fluidStylist() {
        CharacterSkill melee = CharacterSkill.builder()
                .skill(new AtaqueCorpoACorpo())
                .graduation(SkillGraduation.builder().graduationValue(5).build())
                .build();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .equipment(new ArrayList<>())
                .drawnWeapons(new ArrayList<>())
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(2).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, melee)
                .primaryTitle(new Santo(List.of(), List.of()))
                .build();
        character.grantFeat(ArtesMarciaisFeat.DOMINAR_ARTE_MARCIAL_ARTE_FLUIDA);
        return character;
    }

    @Test
    void anOrdinaryCharacterAttacksExactlyOneTarget() {
        Character plain = CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>()).build();

        assertEquals(AttackTargetingService.BASE_TARGETS,
                attackTargetingService.getMaximumTargets(plain, SkillType.ATAQUE_CORPO_A_CORPO));
    }

    @Test
    void arteFluidaAddsOneTargetWhileTheStylistFightsEmptyHanded() {
        assertEquals(2, attackTargetingService.getMaximumTargets(fluidStylist(), SkillType.ATAQUE_CORPO_A_CORPO));
    }

    /** "seus ataques" is unqualified — the extra target is not scoped to one Perícia de Ataque. */
    @Test
    void arteFluidaAddsItsTargetToARangedAttackToo() {
        assertEquals(2, attackTargetingService.getMaximumTargets(fluidStylist(), SkillType.ATAQUE_A_DISTANCIA));
    }

    @Test
    void aDrawnNonNaturalWeaponTakesTheExtraTargetAway() {
        Character stylist = fluidStylist();
        stylist.equip(SWORD);
        stylist.drawWeapon(SWORD);

        assertEquals(1, attackTargetingService.getMaximumTargets(stylist, SkillType.ATAQUE_CORPO_A_CORPO));
    }

    /** "utilizando" is in-hand: a sheathed blade costs the stylist nothing. */
    @Test
    void aCarriedButSheathedWeaponKeepsTheExtraTarget() {
        Character stylist = fluidStylist();
        stylist.equip(SWORD);

        assertEquals(2, attackTargetingService.getMaximumTargets(stylist, SkillType.ATAQUE_CORPO_A_CORPO));
    }

    @Test
    void drawnArmasNaturaisAreTheStatedExceptionAndKeepTheExtraTarget() {
        Character stylist = fluidStylist();
        stylist.equip(CLAWS);
        stylist.drawWeapon(CLAWS);

        assertEquals(2, attackTargetingService.getMaximumTargets(stylist, SkillType.ATAQUE_CORPO_A_CORPO));
    }

    @Test
    void anEquippedEscudoTakesTheExtraTargetAway() {
        Character stylist = fluidStylist();
        stylist.equip(SHIELD);

        assertEquals(1, attackTargetingService.getMaximumTargets(stylist, SkillType.ATAQUE_CORPO_A_CORPO));
    }
}
