package org.aventyrs.core.feat;

import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The choice-carrying instance itself. The Vantagem each chosen Perícia buys is asserted off the
 * Interaction in {@code RacialFeatEffectIntegrationTest}.
 */
class AdotadoPorSylphFeatTest {

    @Test
    void delegatesIdentityToTheCatalogConstant() {
        AdotadoPorSylphFeat feat = AdotadoPorSylphFeat.of(SkillType.ARTES);

        assertSame(FeericoFeat.ADOTADO_POR_SYLPH, feat.catalogEntry());
        assertEquals(FeericoFeat.ADOTADO_POR_SYLPH.getDescription(), feat.getDescription());
    }

    /** The choice is a <em>set</em> — one Perícia per Título Desperto, so it is more than one pick. */
    @Test
    void carriesEveryChosenPericia() {
        AdotadoPorSylphFeat feat = AdotadoPorSylphFeat.of(SkillType.ARTES, SkillType.ATTENTION);

        assertEquals(Set.of(SkillType.ARTES, SkillType.ATTENTION), feat.getChosenSkills());
    }
}
