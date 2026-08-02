package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeValue;
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
     * Raises an attribute's base by one point.
     *
     * @throws IllegalOperationException if the base is already at {@value #MAX_ATTRIBUTE_BASE}
     */
    AttributeValue upgradeBase(AttributeValue currentValue) throws IllegalOperationException;
}
