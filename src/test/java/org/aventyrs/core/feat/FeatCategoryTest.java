package org.aventyrs.core.feat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeatCategoryTest {

    @Test
    void generalCategoriesAreTypedGeral() {
        assertEquals(FeatCategory.Type.GERAL, FeatCategory.ARTILHARIA.getType());
        assertEquals(FeatCategory.Type.GERAL, FeatCategory.SOBREVIVENCIA.getType());
    }

    @Test
    void racialCategoriesAreTypedRacial() {
        assertEquals(FeatCategory.Type.RACIAL, FeatCategory.MONSTRUOSO.getType());
        assertEquals(FeatCategory.Type.RACIAL, FeatCategory.BESTIAL.getType());
    }
}
