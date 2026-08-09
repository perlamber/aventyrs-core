package org.aventyrs.core.character;

import lombok.Getter;

/**
 * A bonus to a dano roll, e.g. Vantagem (see {@code org.aventyrs.core.skill.Skill
 * #ADVANTAGE_BONUS}) granted by {@code
 * org.aventyrs.core.skill.ataqueadistancia.AtaqueADistanciaCompetencyAbility#FRIEZA}. Pairs a
 * flat {@code value} with the {@link DamageType} it applies to — this core never rolls the
 * dano itself (same "this core doesn't roll dice" boundary as everywhere else), so a caller
 * adds {@code value} to its own already-rolled dano total.
 */
@Getter
public class DamageBonus {
    private final int value;
    private final DamageType type;

    public DamageBonus(final int value, final DamageType type) {
        this.value = value;
        this.type = type;
    }
}
