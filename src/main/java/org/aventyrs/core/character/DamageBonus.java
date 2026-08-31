package org.aventyrs.core.character;

import lombok.Getter;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.sheet.IllegalOperationException;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_DAMAGE_TYPE_ELEMENT_PAIRING;

/**
 * A bonus to a dano roll, e.g. Vantagem (see {@code org.aventyrs.core.skill.Skill
 * #ADVANTAGE_BONUS}) granted by {@code
 * org.aventyrs.core.skill.ataqueadistancia.AtaqueADistanciaCompetencyAbility#FRIEZA}. Pairs a
 * flat {@code value} with the {@link DamageType} it applies to — this core never rolls the
 * dano itself (same "this core doesn't roll dice" boundary as everywhere else), so a caller
 * adds {@code value} to its own already-rolled dano total.
 *
 * <p>{@code elementalType} is required exactly when {@code type} is {@link DamageType#ELEMENTAL} or
 * {@link DamageType#FISICO_ELEMENTAL} — {@code null} for {@link DamageType#FISICO}/{@link
 * DamageType#MAGICO}/{@link DamageType#PRIMORDIAL}, a real {@link ElementalType} for the two
 * elemental constants — and validated at construction, a genuine system boundary (a caller
 * supplying this bonus's own classification), the same restraint {@code
 * org.aventyrs.core.skill.SkillRoll}'s own dice validation already applies for the identical
 * reason.
 */
@Getter
public class DamageBonus {
    private final int value;
    private final DamageType type;
    private final ElementalType elementalType;

    public DamageBonus(final int value, final DamageType type) {
        this(value, type, null);
    }

    public DamageBonus(final int value, final DamageType type, final ElementalType elementalType) {
        boolean isElemental = type == DamageType.ELEMENTAL || type == DamageType.FISICO_ELEMENTAL;
        if (isElemental == (elementalType == null)) {
            throw new IllegalOperationException(INVALID_DAMAGE_TYPE_ELEMENT_PAIRING);
        }
        this.value = value;
        this.type = type;
        this.elementalType = elementalType;
    }
}
