package org.aventyrs.core.character;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * An attribute's value is the sum of three independent components: the natural
 * Base a creature invested in, a Racial Bonus granted by its race (fixed or
 * chosen at creation), and a Variable component from spells, feats or equipment.
 */
@Builder(toBuilder = true)
@Getter
public class AttributeValue{
    @Builder.Default
    private int base = 1;
    @Builder.Default
    private int racialBonus = 0;
    @Builder.Default
    private int variable = 0;
    @NonNull
    private AttributeDomain domain;
    
    public int getTotal() {
        return base + racialBonus + variable;
    }

    public AttributeDomain domain(){
        return domain;
    }
}
