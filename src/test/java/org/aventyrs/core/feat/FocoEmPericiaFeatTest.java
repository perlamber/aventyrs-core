package org.aventyrs.core.feat;

import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FocoEmPericiaFeatTest {

    @Test
    void delegatesIdentityToTheCatalogConstant() {
        FocoEmPericiaFeat feat = FocoEmPericiaFeat.of(SkillType.ATLETISMO);

        assertSame(PeritoFeat.FOCO_EM_PERICIA, feat.catalogEntry());
        assertEquals(FeatCategory.PERITO, feat.getFeatCategory());
        assertEquals(PeritoFeat.FOCO_EM_PERICIA.getDescription(), feat.getDescription());
        assertEquals(PeritoFeat.FOCO_EM_PERICIA.getFeatRequirements(), feat.getFeatRequirements());
    }

    @Test
    void requiresAChosenSkill() {
        assertThrows(NullPointerException.class, () -> FocoEmPericiaFeat.of(null));
    }

    @Test
    void grantsVantagemOnTheChosenSkillAndNothingElse() {
        FocoEmPericiaFeat feat = FocoEmPericiaFeat.of(SkillType.ATLETISMO);

        assertEquals(Skill.ADVANTAGE_BONUS,
                feat.resolveSkillRollBonus(SkillType.ATLETISMO, null, null, null));
        assertEquals(0,
                feat.resolveSkillRollBonus(SkillType.PERSUASAO, null, null, null));
    }

    /** No custom equals — two instances with the same choice are still distinct objects. */
    @Test
    void identityIsReferenceBased() {
        assertNotEquals(FocoEmPericiaFeat.of(SkillType.ATLETISMO), FocoEmPericiaFeat.of(SkillType.ATLETISMO));
    }
}
