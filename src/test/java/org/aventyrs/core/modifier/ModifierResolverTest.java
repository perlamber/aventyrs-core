package org.aventyrs.core.modifier;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModifierResolverTest {

    private final ModifierResolver resolver = new ModifierResolverImpl();

    static class SingleBonusSource {
        @Modifier(ModifierType.LIFE_MULTIPLIER)
        public int bonus() {
            return 2;
        }
    }

    static class MultiBonusSource {
        @Modifier(ModifierType.LIFE_MULTIPLIER)
        public int firstBonus() {
            return 1;
        }

        @Modifier(ModifierType.LIFE_MULTIPLIER)
        public int secondBonus() {
            return 1;
        }
    }

    static class NoBonusSource {
        public int unrelatedMethod() {
            return 99;
        }
    }

    static class BrokenBonusSource {
        @Modifier(ModifierType.LIFE_MULTIPLIER)
        public int bonus() {
            throw new IllegalStateException("boom");
        }
    }

    @Test
    void sumsSingleAnnotatedMethod() {
        assertEquals(2, resolver.sumModifiers(new SingleBonusSource(), ModifierType.LIFE_MULTIPLIER));
    }

    @Test
    void sumsMultipleAnnotatedMethodsOnSameSource() {
        assertEquals(2, resolver.sumModifiers(new MultiBonusSource(), ModifierType.LIFE_MULTIPLIER));
    }

    @Test
    void returnsZeroWhenSourceHasNoMatchingAnnotation() {
        assertEquals(0, resolver.sumModifiers(new NoBonusSource(), ModifierType.LIFE_MULTIPLIER));
    }

    @Test
    void sumsAcrossACollectionOfHeterogeneousSources() {
        List<Object> sources = List.of(new SingleBonusSource(), new MultiBonusSource(), new NoBonusSource());
        assertEquals(4, resolver.sumModifiers(sources, ModifierType.LIFE_MULTIPLIER));
    }

    @Test
    void wrapsReflectiveFailuresInModifierResolutionException() {
        assertThrows(ModifierResolutionException.class,
                () -> resolver.sumModifiers(new BrokenBonusSource(), ModifierType.LIFE_MULTIPLIER));
    }
}
