package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.feat.ArtesMarciaisFeat;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.AbstractItem;
import org.aventyrs.core.item.DefensiveImprovement;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemImprovement;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.artes.ArtesAprimorarComArteAbility;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoCompetencyAbility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DamageBaseServiceTest {

    private final DamageBaseService damageBaseService = new DamageBaseServiceImpl();

    /** A 2d6+0 Corpo-a-Corpo weapon — four rows up the scale from bare hands. */
    private static final Weapon ESPADA = AbstractWeapon.builder()
            .name("Espada Longa")
            .category(ItemCategory.HEAVY_BLADE)
            .damageBase(DamageBase.of(2, 0))
            .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
            .build();

    /** The same 2d6+0 starting row, but swung with the other Perícia de Ataque. */
    private static final Weapon ARCO = AbstractWeapon.builder()
            .name("Arco Longo")
            .category(ItemCategory.BOW)
            .damageBase(DamageBase.of(2, 0))
            .skillType(SkillType.ATAQUE_A_DISTANCIA)
            .build();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private Character.CharacterBuilder blankCharacter() {
        return CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>());
    }

    private CharacterSkill meleeSkillAt(int graduation) {
        CharacterSkill skill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1).build();
        skill.increaseGraduation(graduation);
        return skill;
    }

    @Test
    void anUnarmedCharacterWithNoGrantsIsAtTheBottomOfTheScale() {
        Character character = blankCharacter().build();

        assertEquals(DamageBase.UNARMED,
                damageBaseService.getDamageBase(character, SkillType.ATAQUE_CORPO_A_CORPO));
    }

    @Test
    void aWeaponWithNoGrantsReportsItsOwnAuthoredDamageBase() {
        Character character = blankCharacter().build();

        assertEquals(DamageBase.of(2, 0), damageBaseService.getDamageBase(character, ESPADA));
    }

    /** An unarmed attack is the other overload, never a null weapon here. */
    @Test
    void theWeaponOverloadRefusesANullWeapon() {
        Character character = blankCharacter().build();

        assertThrows(NullPointerException.class, () -> damageBaseService.getDamageBase(character, (Weapon) null));
    }

    @Test
    void aFeatsGrantScalesTheWeaponUp() {
        Character character = blankCharacter().build();
        character.grantFeat(ArtesMarciaisFeat.ARTISTA_MARCIAL);

        // +1, and no Título Aventyr held, so exactly one row: 2d6+0 -> 2d6+1.
        assertEquals(DamageBase.of(2, 1), damageBaseService.getDamageBase(character, ESPADA));
    }

    @Test
    void bencaoSelvagemRaisesOnlyTheNaturalWeaponUsedForTheAttack() {
        AbstractItem blessedArmor = AbstractItem.builder().name("Armadura Abençoada")
                .category(ItemCategory.ARMOR).build();
        blessedArmor.setImprovement(ItemImprovement.of(DefensiveImprovement.BENCAO_SELVAGEM));
        Weapon naturalWeapon = AbstractWeapon.builder()
                .name("Garras")
                .category(ItemCategory.NATURAL_WEAPON)
                .damageBase(DamageBase.of(2, 0))
                .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
                .build();
        Character character = blankCharacter().equipment(List.of(blessedArmor, naturalWeapon)).build();

        assertEquals(DamageBase.of(2, 1), damageBaseService.getDamageBase(character, naturalWeapon));
        assertEquals(DamageBase.of(2, 0), damageBaseService.getDamageBase(character, ESPADA));
    }

    /**
     * APRIMORAR_COM_ARTE is an Artes ability that raises the Dano Base of the Perícia de Ataque
     * its holder chose — so it must be reached without the service filtering by the ability's
     * own getSkillType(), and must not leak into the other attack Perícia. Which Perícia a swing
     * is made with is now read off the weapon, so the two weapons are what differ here.
     */
    @Test
    void anAbilityScopedToAChosenPericiaAppliesOnlyToThatOne() {
        Character character = blankCharacter()
                .skillCompetencyAbility(new ArtesAprimorarComArteAbility(SkillType.ATAQUE_CORPO_A_CORPO))
                .build();

        assertEquals(DamageBase.of(2, 1), damageBaseService.getDamageBase(character, ESPADA));
        assertEquals(DamageBase.of(2, 0), damageBaseService.getDamageBase(character, ARCO));
    }

    /**
     * Ataque à Distância's FOCADO Excelência raises Dano Base, and must not raise a
     * Corpo-a-Corpo swing's — which is why only the attacking Perícia's own tiers are scanned.
     */
    @Test
    void anExcellencyGrantIsScopedToItsOwnPericia() {
        CharacterSkill rangedSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_A_DISTANCIA_1).build();
        rangedSkill.increaseGraduation(5);
        Character character = blankCharacter()
                .skill(SkillType.ATAQUE_A_DISTANCIA, rangedSkill)
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, meleeSkillAt(5))
                .build();

        assertEquals(DamageBase.of(2, 1), damageBaseService.getDamageBase(character, ARCO));
        assertEquals(DamageBase.of(2, 0), damageBaseService.getDamageBase(character, ESPADA));
    }

    @Test
    void anUntrainedAttackingPericiaUnlocksNoExcellencyTiers() {
        Character character = blankCharacter().build();

        assertEquals(DamageBase.of(2, 0), damageBaseService.getDamageBase(character, ARCO));
    }

    /**
     * Three sources at once, on a weapon already partway up the scale — and the point of the
     * whole type: the four scale-ups roll 2d6+0 over into 3d6+0, not into "2d6+4".
     */
    @Test
    void everySourceStacksAndRollsOverIntoAnExtraDie() {
        Character character = blankCharacter()
                .skillCompetencyAbility(new ArtesAprimorarComArteAbility(SkillType.ATAQUE_CORPO_A_CORPO))
                .skillCompetencyAbility(AtaqueCorpoACorpoCompetencyAbility.BRUTALIDADE)
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, meleeSkillAt(10))
                .build();
        character.grantFeat(ArtesMarciaisFeat.ARTISTA_MARCIAL);

        // +1 Aprimorar com Arte, +2 Brutalidade at 10 Graduações, +1 Artista Marcial = 4 rows.
        assertEquals(DamageBase.of(3, 0), damageBaseService.getDamageBase(character, ESPADA));
    }
}
