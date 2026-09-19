package org.aventyrs.core.skill;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.TestSpell;
import org.aventyrs.core.skill.ataqueadistancia.AtaqueADistanciaSpecialization;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoSpecialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AttackSpecializationsTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private static Weapon weapon(final ItemCategory category, final ItemWeightClass weightClass, final SkillType skill) {
        return AbstractWeapon.builder().name(category.name()).category(category).weightClass(weightClass)
                .damageBase(DamageBase.of(1, 1)).skillType(skill).build();
    }

    private static Character blank() {
        return CharacterFixture.blank(CharacterFixture.BLANK).build();
    }

    private static Optional<SkillSpecialization> required(final Weapon weapon) {
        return AttackSpecializations.requiredFor(weapon, blank());
    }

    @Test
    void aMeleeWeaponIsRolledWithTheInfantariaItsBaseWeightNames() {
        assertEquals(Optional.of(AtaqueCorpoACorpoSpecialization.INFANTARIA_LEVE),
                required(weapon(ItemCategory.LIGHT_BLADE, ItemWeightClass.LIGHT, SkillType.ATAQUE_CORPO_A_CORPO)));
        assertEquals(Optional.of(AtaqueCorpoACorpoSpecialization.INFANTARIA_LEVE),
                required(weapon(ItemCategory.CLUB, ItemWeightClass.MEDIUM, SkillType.ATAQUE_CORPO_A_CORPO)));
        assertEquals(Optional.of(AtaqueCorpoACorpoSpecialization.INFANTARIA_PESADA),
                required(weapon(ItemCategory.HEAVY_BLADE, ItemWeightClass.HEAVY, SkillType.ATAQUE_CORPO_A_CORPO)));
    }

    @Test
    void aRangedWeaponIsRolledWithTheArtilhariaItsBaseWeightNames() {
        assertEquals(Optional.of(AtaqueADistanciaSpecialization.ARTILHARIA_LEVE),
                required(weapon(ItemCategory.BOW, ItemWeightClass.LIGHT, SkillType.ATAQUE_A_DISTANCIA)));
        assertEquals(Optional.of(AtaqueADistanciaSpecialization.ARTILHARIA_PESADA),
                required(weapon(ItemCategory.CROSSBOW, ItemWeightClass.MEDIUM, SkillType.ATAQUE_A_DISTANCIA)));
        assertEquals(Optional.of(AtaqueADistanciaSpecialization.ARTILHARIA_PESADA),
                required(weapon(ItemCategory.BOW, ItemWeightClass.HEAVY, SkillType.ATAQUE_A_DISTANCIA)));
    }

    @Test
    void aThrowableIsTecnicasDeArremessoWhateverItWeighs() {
        assertEquals(Optional.of(AtaqueADistanciaSpecialization.TECNICAS_DE_ARREMESSO),
                required(weapon(ItemCategory.THROWABLE, ItemWeightClass.LIGHT, SkillType.ATAQUE_A_DISTANCIA)));
    }

    @Test
    void anArmaNaturalIsPrimalWhateverItWeighs() {
        assertEquals(Optional.of(AtaqueCorpoACorpoSpecialization.PRIMAL),
                required(weapon(ItemCategory.NATURAL_WEAPON, ItemWeightClass.HEAVY, SkillType.ATAQUE_CORPO_A_CORPO)));
        assertEquals(Optional.of(AtaqueCorpoACorpoSpecialization.PRIMAL), required(NaturalWeapon.ATAQUE_DESARMADO));
    }

    @Test
    void aRangedArmaNaturalHasNoEspecializacao() {
        assertEquals(Optional.empty(), required(NaturalWeapon.ARMA_DE_SOPRO));
    }

    @Test
    void aSpellIsRolledWithTheConjurationEspecializacaoOfItsPericia() {
        assertEquals(Optional.of(AtaqueADistanciaSpecialization.CONJURADOR_DE_LINHA_DE_TRAS),
                AttackSpecializations.requiredFor(new TestSpell(SkillType.ATAQUE_A_DISTANCIA), blank()));
        assertEquals(Optional.of(AtaqueCorpoACorpoSpecialization.ARCANISTA_DE_LINHA_DE_FRENTE),
                AttackSpecializations.requiredFor(new TestSpell(SkillType.ATAQUE_CORPO_A_CORPO), blank()));
    }

    @Test
    void noSourceOrANonAttackPericiaHasNoEspecializacao() {
        assertEquals(Optional.empty(), AttackSpecializations.requiredFor(null, blank()));
        assertEquals(Optional.empty(),
                required(weapon(ItemCategory.LIGHT_BLADE, ItemWeightClass.LIGHT, SkillType.ATLETISMO)));
    }

    @Test
    void heldForKeepsOnlyAnEspecializacaoTheCharacterHolds() {
        Weapon dagger = weapon(ItemCategory.LIGHT_BLADE, ItemWeightClass.LIGHT, SkillType.ATAQUE_CORPO_A_CORPO);
        Weapon greatsword = weapon(ItemCategory.HEAVY_BLADE, ItemWeightClass.HEAVY, SkillType.ATAQUE_CORPO_A_CORPO);
        CharacterSkill melee = CharacterSkillFixture.blank(CharacterSkillFixture.ATAQUE_CORPO_A_CORPO_1)
                .specializations(List.of(AtaqueCorpoACorpoSpecialization.INFANTARIA_LEVE))
                .build();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, melee)
                .build();

        assertEquals(Optional.of(AtaqueCorpoACorpoSpecialization.INFANTARIA_LEVE),
                AttackSpecializations.heldFor(dagger, character));
        assertEquals(Optional.empty(), AttackSpecializations.heldFor(greatsword, character));
    }
}
