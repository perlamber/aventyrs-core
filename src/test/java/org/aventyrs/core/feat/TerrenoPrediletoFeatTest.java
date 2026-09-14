package org.aventyrs.core.feat;

import org.aventyrs.core.scene.TerrainType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The choice-carrying instance itself. What the chosen terrain is worth — Vantagem on Furtividade
 * and on Conhecimentos: Natureza, +2 to both Defesas — is asserted off the Interaction and {@code
 * DefenseService} in {@code GeneralFeatEffectIntegrationTest}.
 */
class TerrenoPrediletoFeatTest {

    @Test
    void delegatesIdentityToTheCatalogConstant() {
        TerrenoPrediletoFeat feat = TerrenoPrediletoFeat.of(TerrainType.FOREST);

        assertSame(SobrevivenciaFeat.TERRENO_PREDILETO, feat.catalogEntry());
        assertEquals(SobrevivenciaFeat.TERRENO_PREDILETO.getDescription(), feat.getDescription());
    }

    @Test
    void requiresAChosenTerrain() {
        assertThrows(NullPointerException.class, () -> TerrenoPrediletoFeat.of(null));
    }

    @Test
    void carriesTheChosenTerrain() {
        assertEquals(TerrainType.FOREST, TerrenoPrediletoFeat.of(TerrainType.FOREST).getChosenTerrain());
    }
}
