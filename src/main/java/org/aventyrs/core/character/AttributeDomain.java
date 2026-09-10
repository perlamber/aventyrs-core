package org.aventyrs.core.character;

import java.lang.reflect.Method;

import lombok.Getter;
import org.aventyrs.core.modifier.ModifierType;

@Getter
public enum AttributeDomain {

    VIGOR(KeyAttributeMethods.getVigor, 1, ModifierType.VIGOR_BONUS),
    STRENGTH(KeyAttributeMethods.getStrength, 2, ModifierType.STRENGTH_BONUS),
    DEXTERITY(KeyAttributeMethods.getDexterity, 3, ModifierType.DEXTERITY_BONUS),
    FOCUS(KeyAttributeMethods.getFocus, 4, ModifierType.FOCUS_BONUS),
    INSTINCT(KeyAttributeMethods.getInstinct, 5, ModifierType.INSTINCT_BONUS),
    GNOSE(KeyAttributeMethods.getGnose, 6, ModifierType.GNOSE_BONUS),
    CHARISMA(KeyAttributeMethods.getCharisma, 7, ModifierType.CHARISMA_BONUS);

    private Method keyAttributeMethod;
    private int id;

    /**
     * The {@link ModifierType} a round-scoped bonus to this Atributo is carried as — see that
     * constant's own javadoc for which consumers read it (only the skill-roll path, so far).
     */
    private ModifierType bonusModifierType;

    AttributeDomain(Method keyAttributeMethod, int id, ModifierType bonusModifierType) {
        this.keyAttributeMethod = keyAttributeMethod;
        this.id = id;
        this.bonusModifierType = bonusModifierType;
    }


}
