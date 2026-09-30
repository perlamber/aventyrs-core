package org.aventyrs.core.ego;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.sheet.EgoPointType;

/**
 * Iniciativa's roll effects (2.5 Ego › Iniciativa): one point buys {@link #USES_PER_POINT} of them, "em até duas
 * rolagens de Perícia (efetuadas na mesma Cena)". Banked as sheet charges by {@code
 * InitiativeEgoService#bankRollCharges} (dropped with the Cena, like every charge), spent one per roll by {@code
 * InitiativeEgoService#useRollCharge}, which marks the roll ({@code SkillRoll#hasInitiativeCharge}).
 */
@Getter
@AllArgsConstructor
public enum InitiativeRollCharge {
    /** "Adquirir Vantagem em até duas rolagens de Perícia" — {@code Skill#ADVANTAGE_BONUS} on the roll. */
    ADVANTAGE(EgoPointType.TEMPORARY),
    /**
     * "Reduzir o GD de até duas rolagens de Perícia." ⚠️ Read as one nível each ({@link
     * #DIFFICULTY_REDUCTION_LEVELS}), as Sorte's; the text gives no amount.
     */
    DIFFICULTY_REDUCTION(EgoPointType.PERMANENT);

    /** How many rolls one point buys. */
    public static final int USES_PER_POINT = 2;

    /** What {@link #DIFFICULTY_REDUCTION} eases a roll by — see its javadoc. */
    public static final int DIFFICULTY_REDUCTION_LEVELS = 1;

    private final EgoPointType pointType;
}
