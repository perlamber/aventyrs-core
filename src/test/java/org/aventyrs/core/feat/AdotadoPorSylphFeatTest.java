package org.aventyrs.core.feat;

import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AdotadoPorSylphFeatTest {

    @Test
    void delegatesIdentityToTheCatalogConstant() {
        AdotadoPorSylphFeat feat = AdotadoPorSylphFeat.of(SkillType.ARTES);

        assertSame(FeericoFeat.ADOTADO_POR_SYLPH, feat.catalogEntry());
        assertEquals(FeericoFeat.ADOTADO_POR_SYLPH.getDescription(), feat.getDescription());
    }

    @Test
    void grantsVantagemOnEveryChosenSkillOnly() {
        AdotadoPorSylphFeat feat = AdotadoPorSylphFeat.of(SkillType.ARTES, SkillType.ATTENTION);

        assertEquals(Skill.ADVANTAGE_BONUS, feat.resolveSkillRollBonus(SkillType.ARTES, null, null, null));
        assertEquals(Skill.ADVANTAGE_BONUS, feat.resolveSkillRollBonus(SkillType.ATTENTION, null, null, null));
        assertEquals(0, feat.resolveSkillRollBonus(SkillType.PERSUASAO, null, null, null));
    }
}
