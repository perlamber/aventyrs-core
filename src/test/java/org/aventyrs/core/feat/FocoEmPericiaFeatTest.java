package org.aventyrs.core.feat;

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

    /**
     * The choice is what the instance carries; the Vantagem it buys is asserted off the
     * Interaction in {@code GeneralFeatEffectIntegrationTest}.
     */
    @Test
    void carriesTheChosenSkill() {
        assertEquals(SkillType.ATLETISMO, FocoEmPericiaFeat.of(SkillType.ATLETISMO).getChosenSkill());
    }

    /** No custom equals — two instances with the same choice are still distinct objects. */
    @Test
    void identityIsReferenceBased() {
        assertNotEquals(FocoEmPericiaFeat.of(SkillType.ATLETISMO), FocoEmPericiaFeat.of(SkillType.ATLETISMO));
    }
}
