package org.aventyrs.core.feat;

import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.AttackMethod;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EspecialistaEmArmaFeatTest {

    private static Weapon weapon(final ItemCategory category) {
        return AbstractWeapon.builder().name(category.name()).category(category)
                .damageBase(DamageBase.of(1, 1)).skillType(SkillType.ATAQUE_CORPO_A_CORPO).build();
    }

    @Test
    void delegatesIdentityToTheCatalogConstant() {
        EspecialistaEmArmaFeat feat = EspecialistaEmArmaFeat.of(AttackMethod.BOW);

        assertSame(DuelistaFeat.ESPECIALISTA_EM_ARMA, feat.catalogEntry());
        assertEquals(DuelistaFeat.ESPECIALISTA_EM_ARMA.getDescription(), feat.getDescription());
    }

    @Test
    void requiresAChosenMethod() {
        assertThrows(NullPointerException.class, () -> EspecialistaEmArmaFeat.of(null));
    }

    @Test
    void grantsVantagemOnlyOnAnAttackDeliveredWithTheChosenMethod() {
        EspecialistaEmArmaFeat feat = EspecialistaEmArmaFeat.of(AttackMethod.LIGHT_BLADE);
        Weapon lightBlade = weapon(ItemCategory.LIGHT_BLADE);
        Weapon heavyBlade = weapon(ItemCategory.HEAVY_BLADE);

        assertEquals(Skill.ADVANTAGE_BONUS,
                feat.resolveSkillRollBonus(SkillType.ATAQUE_CORPO_A_CORPO, null, null, null, lightBlade));
        assertEquals(0,
                feat.resolveSkillRollBonus(SkillType.ATAQUE_CORPO_A_CORPO, null, null, null, heavyBlade));
        assertEquals(0,
                feat.resolveSkillRollBonus(SkillType.ATAQUE_CORPO_A_CORPO, null, null, null, null));
    }

    @Test
    void doesNotGrantOnANonAttackSkillEvenWithTheChosenMethod() {
        EspecialistaEmArmaFeat feat = EspecialistaEmArmaFeat.of(AttackMethod.LIGHT_BLADE);
        Weapon lightBlade = weapon(ItemCategory.LIGHT_BLADE);

        assertEquals(0, feat.resolveSkillRollBonus(SkillType.ATLETISMO, null, null, null, lightBlade));
    }
}
