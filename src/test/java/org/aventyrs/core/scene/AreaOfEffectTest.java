package org.aventyrs.core.scene;

import org.aventyrs.core.sheet.IllegalOperationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaOfEffectTest {

    @Test
    void circleCarriesItsRadiusAsUnidadesDeDistancia() {
        AreaOfEffect area = AreaOfEffect.circle(2);

        assertEquals(AreaShape.CIRCULO, area.shape());
        assertEquals(2, area.unidadesDeDistancia());
    }

    @Test
    void lineCarriesItsLengthAsUnidadesDeDistancia() {
        AreaOfEffect area = AreaOfEffect.line(6);

        assertEquals(AreaShape.LINHA, area.shape());
        assertEquals(6, area.unidadesDeDistancia());
    }

    @Test
    void coneCarriesItsLengthAsUnidadesDeDistancia() {
        AreaOfEffect area = AreaOfEffect.cone(4);

        assertEquals(AreaShape.CONE, area.shape());
        assertEquals(4, area.unidadesDeDistancia());
    }

    @Test
    void theSmallestLegalAreaIsOneUnidadeDeDistancia() {
        assertEquals(AreaOfEffect.MIN_UNIDADES_DE_DISTANCIA, AreaOfEffect.circle(1).unidadesDeDistancia());
    }

    @Test
    void anAreaOfZeroOrFewerUnidadesDeDistanciaIsRejected() {
        assertThrows(IllegalOperationException.class, () -> AreaOfEffect.circle(0));
        assertThrows(IllegalOperationException.class, () -> AreaOfEffect.line(0));
        assertThrows(IllegalOperationException.class, () -> AreaOfEffect.cone(-1));
    }

    @Test
    void anAreaWithNoShapeIsRejected() {
        assertThrows(IllegalOperationException.class, () -> new AreaOfEffect(null, 2));
    }

    @Test
    void linesAndConesAreEmanationsAndCirclesAreNot() {
        assertTrue(AreaOfEffect.line(6).isEmanation());
        assertTrue(AreaOfEffect.cone(4).isEmanation());
        assertFalse(AreaOfEffect.circle(2).isEmanation());
    }

    @Test
    void everyAreaShapeAgreesWithItsOwnEmanationFlag() {
        for (AreaShape shape : AreaShape.values()) {
            assertEquals(shape.isEmanation(), new AreaOfEffect(shape, 1).isEmanation());
        }
    }

    @Test
    void twoIdenticallyBuiltAreasAreEqual() {
        assertEquals(AreaOfEffect.circle(2), AreaOfEffect.circle(2));
        assertEquals(AreaOfEffect.circle(2).hashCode(), AreaOfEffect.circle(2).hashCode());
    }

    @Test
    void areasDifferingInShapeOrSizeAreNotEqual() {
        assertFalse(AreaOfEffect.circle(2).equals(AreaOfEffect.circle(3)));
        assertFalse(AreaOfEffect.line(2).equals(AreaOfEffect.cone(2)));
    }

    @Test
    void toStringReadsAsShapeAndSize() {
        assertEquals("CIRCULO 2UD", AreaOfEffect.circle(2).toString());
    }
}
