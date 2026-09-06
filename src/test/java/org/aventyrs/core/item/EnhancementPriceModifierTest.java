package org.aventyrs.core.item;

import org.aventyrs.core.character.DefenseType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * That the authored catalogs are priced by {@link EnhancementPricing} off the Raridade they
 * already carry — one constant per tier is enough, since the lookup itself is covered by {@link
 * EnhancementPricingTest}. What is worth pinning here is that each catalog reads the right
 * <em>column</em>, and that a per-copy wrapper does not re-derive a price of its own.
 */
class EnhancementPriceModifierTest {

    @Test
    void aDefensiveMasterpieceIsPricedOffTheArmadurasColumn() {
        assertEquals(EnhancementPriceCategory.ARMOR, DefensiveMasterpiece.REFORCADA.getPriceCategory());

        assertEquals(8, DefensiveMasterpiece.REFORCADA.getPriceModifier());
        assertEquals(12, DefensiveMasterpiece.CONJURADORA.getPriceModifier());
        assertEquals(20, DefensiveMasterpiece.BANHADA_EM_OURO.getPriceModifier());
        assertEquals(32, DefensiveMasterpiece.MITRAL.getPriceModifier());
        assertEquals(48, DefensiveMasterpiece.COURO_DE_DRAGAO.getPriceModifier());
    }

    @Test
    void aDefensiveImprovementIsPricedOffTheArmadurasColumn() {
        assertEquals(EnhancementPriceCategory.ARMOR, DefensiveImprovement.RESISTENTE.getPriceCategory());

        assertEquals(5, DefensiveImprovement.RESISTENTE.getPriceModifier());
        assertEquals(8, DefensiveImprovement.OCULTA.getPriceModifier());
        assertEquals(12, DefensiveImprovement.ENCANTADORA.getPriceModifier());
    }

    /**
     * A creation-time choice changes what the enhancement does, never what it costs — the fitted
     * copy is worth exactly its catalog entry.
     */
    @Test
    void aFittedCopyIsPricedAsItsCatalogEntry() {
        assertEquals(DefensiveMasterpiece.MAGISTRAL.getPriceModifier(),
                ItemMasterpiece.magistral(DefenseType.PHYSICAL).getPriceModifier());
        assertEquals(DefensiveMasterpiece.REFORCADA.getPriceModifier(),
                ItemMasterpiece.of(DefensiveMasterpiece.REFORCADA).getPriceModifier());

        assertEquals(DefensiveImprovement.CAMADA_DE_REFORCO.getPriceModifier(),
                ItemImprovement.camadaDeReforco(DefenseType.MAGIC).getPriceModifier());
        assertEquals(DefensiveImprovement.RESISTENTE.getPriceModifier(),
                ItemImprovement.of(DefensiveImprovement.RESISTENTE).getPriceModifier());
    }

    /**
     * A Pedra do Poder's refinements read the third column directly — they are not {@link
     * Masterpiece}/{@link Improvement} implementations, so they carry their own accessor.
     */
    @Test
    void aPowerStoneRefinementIsPricedOffThePedrasDoPoderColumn() {
        assertEquals(6, PowerStoneMasterpiece.RESFRIAMENTO_RAPIDO.getPriceModifier());
        assertEquals(9, PowerStoneMasterpiece.CARGA_EXTRA.getPriceModifier());
        assertEquals(13, PowerStoneMasterpiece.SOLVE_VIDAS.getPriceModifier());

        assertEquals(7, PowerStoneImprovement.CONEXAO_VELOZ.getPriceModifier());
    }
}
