package org.aventyrs.core.monster;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.skill.SkillType;

import java.util.Map;

/**
 * The Mestre's hand on a rules-built monster — a signed delta on each number {@link MonsterRules}
 * derives, applied last.
 *
 * <p>Everything about a {@link MonsterBlueprint} is derived from its Grau de Poder, Atributos and
 * Habilidades; this is the one place a number can be moved by fiat, for the creature the rules
 * don't quite reach (a one-off boss, a set piece, a house rule). It is <b>not validated</b> —
 * {@link MonsterRules#validate} checks the choices the rules govern, never these — and every field
 * defaults to 0, so {@link #NONE} is a monster exactly as the rules build it.
 */
@Getter
@Builder(toBuilder = true)
public class MonsterAdjustments {

    public static final MonsterAdjustments NONE = MonsterAdjustments.builder().build();

    /** Steps up (positive) or down the GD ladder, per Perícia — after every other rule. */
    @NonNull
    @Singular("skillLevelShift")
    private final Map<SkillType, Integer> skillLevelShifts;

    /** Flat bonus on the GD a Perícia presents, per Perícia. */
    @NonNull
    @Singular("skillBonus")
    private final Map<SkillType, Integer> skillBonuses;

    private final int physicalDefense;

    private final int magicDefense;

    /** Added to the fixed Pontos de Ação counter. */
    private final int actionPoints;

    private final int lifeMultiplier;

    private final int manaMultiplier;

    private final int determinationMultiplier;

    public int getSkillLevelShift(@NonNull final SkillType skill) {
        return skillLevelShifts.getOrDefault(skill, 0);
    }

    public int getSkillBonus(@NonNull final SkillType skill) {
        return skillBonuses.getOrDefault(skill, 0);
    }

    public int getDefense(@NonNull final DefenseType defenseType) {
        return defenseType == DefenseType.PHYSICAL ? physicalDefense : magicDefense;
    }
}
