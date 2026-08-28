package org.aventyrs.core.magic;

import org.aventyrs.core.scene.AreaOfEffect;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellTargetingTest {

    @Test
    void pessoalCarriesNeitherARangeNorAnArea() {
        assertEquals(SpellReach.PESSOAL, SpellTargeting.PESSOAL.reach());
        assertNull(SpellTargeting.PESSOAL.range());
        assertNull(SpellTargeting.PESSOAL.area());
        assertFalse(SpellTargeting.PESSOAL.isAreaOfEffect());
        assertFalse(SpellTargeting.PESSOAL.isCenteredOnCaster());
    }

    @Test
    void toqueCarriesNeitherARangeNorAnArea() {
        assertEquals(SpellReach.TOQUE, SpellTargeting.TOQUE.reach());
        assertNull(SpellTargeting.TOQUE.range());
        assertNull(SpellTargeting.TOQUE.area());
        assertFalse(SpellTargeting.TOQUE.isAreaOfEffect());
    }

    @Test
    void pessoalAndToqueRejectARangeOrAnArea() {
        assertThrows(IllegalOperationException.class,
                () -> new SpellTargeting(SpellReach.PESSOAL, Range.DISTANCIA_MEDIA, null));
        assertThrows(IllegalOperationException.class,
                () -> new SpellTargeting(SpellReach.PESSOAL, null, AreaOfEffect.circle(2)));
        assertThrows(IllegalOperationException.class,
                () -> new SpellTargeting(SpellReach.TOQUE, Range.ADJACENTE, null));
        assertThrows(IllegalOperationException.class,
                () -> new SpellTargeting(SpellReach.TOQUE, null, AreaOfEffect.circle(2)));
    }

    @Test
    void distanciaCarriesItsRangeAndNoArea() {
        SpellTargeting targeting = SpellTargeting.distancia(Range.DISTANCIA_MEDIA);

        assertEquals(SpellReach.DISTANCIA, targeting.reach());
        assertEquals(Range.DISTANCIA_MEDIA, targeting.range());
        assertNull(targeting.area());
        assertFalse(targeting.isAreaOfEffect());
    }

    @Test
    void distanciaRequiresARangeAndRefusesAnArea() {
        assertThrows(IllegalOperationException.class, () -> SpellTargeting.distancia(null));
        assertThrows(IllegalOperationException.class,
                () -> new SpellTargeting(SpellReach.DISTANCIA, Range.DISTANCIA_MEDIA, AreaOfEffect.circle(2)));
    }

    @Test
    void anAreaWithNoRangeIsCenteredOnTheCaster() {
        SpellTargeting targeting = SpellTargeting.areaDeEfeito(AreaOfEffect.circle(2));

        assertEquals(SpellReach.AREA_DE_EFEITO, targeting.reach());
        assertNull(targeting.range());
        assertEquals(AreaOfEffect.circle(2), targeting.area());
        assertTrue(targeting.isAreaOfEffect());
        assertTrue(targeting.isCenteredOnCaster());
    }

    @Test
    void anAreaWithARangeHasItsCentrePlacedAtThatRange() {
        SpellTargeting targeting = SpellTargeting.areaDeEfeito(Range.DISTANCIA_MEDIA, AreaOfEffect.circle(2));

        assertEquals(SpellReach.AREA_DE_EFEITO, targeting.reach());
        assertEquals(Range.DISTANCIA_MEDIA, targeting.range());
        assertEquals(AreaOfEffect.circle(2), targeting.area());
        assertTrue(targeting.isAreaOfEffect());
        assertFalse(targeting.isCenteredOnCaster());
    }

    @Test
    void areaDeEfeitoRequiresAnArea() {
        assertThrows(IllegalOperationException.class, () -> SpellTargeting.areaDeEfeito(null));
        assertThrows(IllegalOperationException.class,
                () -> SpellTargeting.areaDeEfeito(Range.DISTANCIA_MEDIA, null));
    }

    @Test
    void anEmanationMayBeCenteredOnTheCaster() {
        assertTrue(SpellTargeting.areaDeEfeito(AreaOfEffect.cone(4)).isCenteredOnCaster());
        assertTrue(SpellTargeting.areaDeEfeito(AreaOfEffect.line(6)).isCenteredOnCaster());
    }

    @Test
    void anEmanationMayNotHaveItsCentrePlacedAtARange() {
        assertThrows(IllegalOperationException.class,
                () -> SpellTargeting.areaDeEfeito(Range.DISTANCIA_MEDIA, AreaOfEffect.cone(4)));
        assertThrows(IllegalOperationException.class,
                () -> SpellTargeting.areaDeEfeito(Range.DISTANCIA_MEDIA, AreaOfEffect.line(6)));
    }

    @Test
    void anAreaAoAlcanceDosOlhosIsLegal() {
        SpellTargeting targeting =
                SpellTargeting.areaDeEfeito(Range.AO_ALCANCE_DOS_OLHOS, AreaOfEffect.circle(2));

        assertEquals(Range.AO_ALCANCE_DOS_OLHOS, targeting.range());
    }

    @Test
    void aTargetingWithNoReachIsRejected() {
        assertThrows(IllegalOperationException.class, () -> new SpellTargeting(null, null, null));
    }

    @Test
    void theBareClassificationIsStillReachable() {
        assertEquals(SpellReach.DISTANCIA, SpellTargeting.distancia(Range.DISTANCIA_CURTA).reach());
        assertEquals(SpellReach.AREA_DE_EFEITO, SpellTargeting.areaDeEfeito(AreaOfEffect.cone(4)).reach());
    }
}
