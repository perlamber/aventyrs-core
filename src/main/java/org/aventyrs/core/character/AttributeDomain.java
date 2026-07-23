package org.aventyrs.core.character;

import java.lang.reflect.Method;

import lombok.Getter;

@Getter
public enum AttributeDomain {

    VIGOR(KeyAttributeMethods.getVigor,1),
    STRENGTH(KeyAttributeMethods.getStrength,2),
    DEXTERITY(KeyAttributeMethods.getDexterity,3),
    FOCUS(KeyAttributeMethods.getFocus,4),
    INSTINCT(KeyAttributeMethods.getInstinct,5),
    GNOSE(KeyAttributeMethods.getGnose,6),
    CHARISMA(KeyAttributeMethods.getCharisma,7);

    private Method keyAttributeMethod;
    private int id;

    AttributeDomain(Method keyAttributeMethod, int id) {
        this.keyAttributeMethod = keyAttributeMethod;
        this.id = id;
    }


}
