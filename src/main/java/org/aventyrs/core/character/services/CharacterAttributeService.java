package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.math.BigDecimal;

public interface CharacterAttributeService {
    int MAX_ATTRIBUTE_BASE = 5;

    /**
     * Cost in experience to raise this attribute's base by one point: the intended
     * (target) base value plus 1.
     */
    BigDecimal getUpgradeCost(AttributeValue currentValue);

    /**
     * Raises an attribute's base by one point, spending {@link #getUpgradeCost} from
     * characterSheet's unused experience — this is a Character-progression action, so it can
     * only happen in the context of a {@link CharacterSheet}, never on a bare
     * {@link AttributeValue}.
     *
     * @throws IllegalOperationException if the base is already at {@value #MAX_ATTRIBUTE_BASE},
     *                                    or if characterSheet doesn't have enough unused experience
     */
    AttributeValue upgradeBase(AttributeValue currentValue, CharacterSheet characterSheet) throws IllegalOperationException;
}
