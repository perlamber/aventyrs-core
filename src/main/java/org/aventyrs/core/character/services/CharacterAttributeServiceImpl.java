package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.math.BigDecimal;

import static org.aventyrs.core.util.TranslatableMessages.ATTRIBUTE_BASE_AT_MAXIMUM;

public class CharacterAttributeServiceImpl implements CharacterAttributeService {

    @Override
    public BigDecimal getUpgradeCost(final AttributeValue currentValue) {
        int targetBase = currentValue.getBase() + 1;
        return BigDecimal.valueOf(targetBase + 1);
    }

    @Override
    public AttributeValue upgradeBase(final AttributeValue currentValue) throws IllegalOperationException {
        int targetBase = currentValue.getBase() + 1;
        if (targetBase > MAX_ATTRIBUTE_BASE) {
            throw new IllegalOperationException(ATTRIBUTE_BASE_AT_MAXIMUM);
        }
        return currentValue.toBuilder().base(targetBase).build();
    }
}
