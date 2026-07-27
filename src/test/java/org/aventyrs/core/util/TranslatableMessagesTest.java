package org.aventyrs.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TranslatableMessagesTest {

    @Test
    void everyMessageKeyIsANonBlankString() {
        assertNotNull(new TranslatableMessages());
        assertFalse(TranslatableMessages.NOT_ENOUGH_EXPERIENCE.isBlank());
        assertFalse(TranslatableMessages.INVALID_ATTRIBUTE_POINT_ALLOCATION.isBlank());
        assertFalse(TranslatableMessages.INVALID_RACIAL_BONUS_ALLOCATION.isBlank());
        assertFalse(TranslatableMessages.ATTRIBUTE_BASE_AT_MAXIMUM.isBlank());
        assertFalse(TranslatableMessages.ATTRIBUTE_ABILITY_ALREADY_CHOSEN.isBlank());
        assertFalse(TranslatableMessages.NO_ATTRIBUTE_ABILITY_SLOT_AVAILABLE.isBlank());
        assertFalse(TranslatableMessages.INVALID_EGO_POINT_ALLOCATION.isBlank());
    }
}
