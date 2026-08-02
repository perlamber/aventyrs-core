package org.aventyrs.core.feat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeatTest {

    @Test
    void builderAssignsTheFeatCategory() {
        Feat feat = Feat.builder().featCategory(FeatCategory.DUELISTA).build();
        assertEquals(FeatCategory.DUELISTA, feat.getFeatCategory());
    }
}
