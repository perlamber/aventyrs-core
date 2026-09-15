package org.aventyrs.core.effect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the registry, the way {@code ItemCatalogTest} guards {@code ItemCatalog.CATALOG_ENUMS} —
 * wiring a builder to its constant is the one step a new category could silently forget.
 */
class SpellEffectKindTest {

    /** The categories that can currently produce an effect. Update deliberately, never casually. */
    private static final Set<SpellEffectKind> IMPLEMENTED =
            Set.of(SpellEffectKind.HEALING, SpellEffectKind.DEFENSIVE);

    @ParameterizedTest
    @EnumSource(SpellEffectKind.class)
    void aKindReportsABuilderExactlyWhenItIsImplemented(final SpellEffectKind kind) {
        assertEquals(IMPLEMENTED.contains(kind), kind.isImplemented());
        assertEquals(kind.isImplemented(), kind.newBuilder().isPresent());
    }

    @Test
    void theImplementedKindsBuildTheirOwnContributor() {
        assertInstanceOf(HealingEffectBuilder.class,
                SpellEffectKind.HEALING.newBuilder().orElseThrow());
        assertInstanceOf(ConditionCleansingEffectBuilder.class,
                SpellEffectKind.DEFENSIVE.newBuilder().orElseThrow());
    }

    @Test
    void theUnimplementedKindsAreDeclaredButEmpty() {
        // Deliberate, not an omission — see OffensiveEffect/InvocationEffect's own javadoc.
        assertFalse(SpellEffectKind.OFFENSIVE.isImplemented());
        assertFalse(SpellEffectKind.INVOCATION.isImplemented());
    }

    @Test
    void aBuilderIsFreshPerCall() {
        // The SkillType#newInteraction contract: a supplier, not a shared instance.
        assertNotSame(SpellEffectKind.HEALING.newBuilder().orElseThrow(),
                SpellEffectKind.HEALING.newBuilder().orElseThrow());
    }

    @Test
    void everyCategoryInterfaceHasAKind() {
        // The enum is the complete list of SpellEffect subcategories; if one is added without a
        // constant here, the factory would never build it.
        assertEquals(4, SpellEffectKind.values().length);
        assertTrue(Arrays.stream(SpellEffectKind.values())
                .anyMatch(kind -> kind == SpellEffectKind.OFFENSIVE));
    }
}
