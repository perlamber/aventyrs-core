package org.aventyrs.core.ego;

import org.aventyrs.core.character.EgoDomain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ResourcesAdvantageTest {

    @Test
    void everyAdvantageBelongsToRecursos() {
        for (ResourcesAdvantage advantage : ResourcesAdvantage.values()) {
            assertEquals(EgoDomain.RECURSOS, advantage.getEgoDomain());
        }
    }

    @Test
    void everyAdvantageHasADescription() {
        for (ResourcesAdvantage advantage : ResourcesAdvantage.values()) {
            assertFalse(advantage.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheThreeDescribedAdvantages() {
        assertEquals(3, ResourcesAdvantage.values().length);
    }
}
