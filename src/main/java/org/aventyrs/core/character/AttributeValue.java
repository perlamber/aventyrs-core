package org.aventyrs.core.character;

import lombok.Builder;
import lombok.Getter;

/**
 * An attribute's value is the sum of three independent components: the natural
 * Base a creature invested in, a Racial Bonus granted by its race (fixed or
 * chosen at creation), and a Variable component from spells, feats or equipment.
 */
@Builder(toBuilder = true)
@Getter
public class AttributeValue {
    @Builder.Default
    private int base = 1;
    @Builder.Default
    private int racialBonus = 0;
    @Builder.Default
    private int variable = 0;

    public int getTotal() {
        return base + racialBonus + variable;
    }
}
