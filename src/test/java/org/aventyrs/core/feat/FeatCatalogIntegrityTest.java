package org.aventyrs.core.feat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Invariants every authored Talento must hold, checked across the whole {@link FeatCatalog}
 * rather than per tree — so a newly added tree is covered the moment it joins {@code Feat}'s
 * permits clause, with no test to remember to write.
 *
 * <p>The first of these is not a formality. An enum constant may name an <b>earlier</b> sibling
 * as its {@code requiredFeat} directly, but naming a <b>later</b> one is a forward reference the
 * compiler rejects — which is why {@code MetamagicoFeat} holds its requirements behind a {@code
 * Supplier}. A tree that gets its declaration order wrong, or that works around the compiler by
 * deferring resolution, can silently end up with a {@code null} prerequisite that lets any
 * character acquire a Talento meant to sit at the top of a ladder.
 */
class FeatCatalogIntegrityTest {

    private static final List<Feat> ALL = FeatCatalog.all();

    @Test
    void theCatalogIsNotEmpty() {
        assertFalse(ALL.isEmpty());
    }

    @Test
    void everyTalentoResolvesItsOwnRequirementsWithoutNulls() {
        for (Feat feat : ALL) {
            assertNotNull(feat.getFeatRequirements(), feat + " has null FeatRequirements");
            assertNotNull(feat.getFeatCategory(), feat + " has null FeatCategory");
        }
    }

    /** A Talento naming a prerequisite Talento must actually resolve it — see the class javadoc. */
    @Test
    void aTalentoNamingAPrerequisiteTalentoResolvesItToARealFeat() {
        for (Feat feat : ALL) {
            Feat required = feat.getFeatRequirements().requiredFeat();
            if (required == null) {
                continue;
            }
            assertNotEquals(feat, required, feat + " names itself as its own prerequisite");
            assertTrue(ALL.contains(required),
                    feat + " requires " + required + ", which is not in the catalog");
        }
    }

    @Test
    void everyTalentoHasANonBlankDescription() {
        for (Feat feat : ALL) {
            assertNotNull(feat.getDescription(), feat + " has a null description");
            assertFalse(feat.getDescription().isBlank(), feat + " has a blank description");
        }
    }
}
