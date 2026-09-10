package org.aventyrs.core.feat;

import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.magic.TestSpell;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponOrSpellChoiceTest {

    private static final AttackSource WEAPON = AbstractWeapon.builder()
            .name("Adaga").category(ItemCategory.LIGHT_BLADE)
            .damageBase(DamageBase.of(1, 1)).skillType(SkillType.ATAQUE_CORPO_A_CORPO).build();
    private static final AttackSource SPELL = new TestSpell();

    @Test
    void weaponsMatchesAWeaponOnly() {
        assertTrue(WeaponOrSpellChoice.WEAPONS.matches(WEAPON));
        assertFalse(WeaponOrSpellChoice.WEAPONS.matches(SPELL));
        assertFalse(WeaponOrSpellChoice.WEAPONS.matches(null));
    }

    @Test
    void spellsMatchesASpellOnly() {
        assertTrue(WeaponOrSpellChoice.SPELLS.matches(SPELL));
        assertFalse(WeaponOrSpellChoice.SPELLS.matches(WEAPON));
        assertFalse(WeaponOrSpellChoice.SPELLS.matches(null));
    }
}
