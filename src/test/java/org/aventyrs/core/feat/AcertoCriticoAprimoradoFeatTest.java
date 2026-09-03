package org.aventyrs.core.feat;

import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.AttackMethod;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.TestSpell;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AcertoCriticoAprimoradoFeatTest {

    private static Weapon weapon(final ItemCategory category) {
        return AbstractWeapon.builder().name(category.name()).category(category)
                .damageBase(DamageBase.of(1, 1)).skillType(SkillType.ATAQUE_CORPO_A_CORPO).build();
    }

    @Test
    void delegatesIdentityToTheCatalogConstant() {
        AcertoCriticoAprimoradoFeat feat = AcertoCriticoAprimoradoFeat.of(AttackMethod.OFFENSIVE_MAGIC);

        assertSame(AssassinoFeat.ACERTO_CRITICO_APRIMORADO, feat.catalogEntry());
    }

    @Test
    void requiresAChosenMethod() {
        assertThrows(NullPointerException.class, () -> AcertoCriticoAprimoradoFeat.of(null));
    }

    @Test
    void widensMargemCriticaOnlyForAttacksDeliveredWithTheChosenMethod() {
        AcertoCriticoAprimoradoFeat blades = AcertoCriticoAprimoradoFeat.of(AttackMethod.LIGHT_BLADE);
        AcertoCriticoAprimoradoFeat magic = AcertoCriticoAprimoradoFeat.of(AttackMethod.OFFENSIVE_MAGIC);

        assertEquals(1, blades.resolveCriticalMarginIncrease(
                SkillType.ATAQUE_CORPO_A_CORPO, null, null, weapon(ItemCategory.LIGHT_BLADE)));
        assertEquals(0, blades.resolveCriticalMarginIncrease(
                SkillType.ATAQUE_CORPO_A_CORPO, null, null, weapon(ItemCategory.HEAVY_BLADE)));
        assertEquals(1, magic.resolveCriticalMarginIncrease(
                SkillType.ATAQUE_A_DISTANCIA, null, null, new TestSpell()));
        assertEquals(0, magic.resolveCriticalMarginIncrease(
                SkillType.ATAQUE_A_DISTANCIA, null, null, weapon(ItemCategory.BOW)));
    }
}
