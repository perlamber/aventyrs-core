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

    /**
     * Combines every dano-roll contribution into the one figure {@code InteractionResult} carries:
     * the summed typed bonuses plus flatModifier, an untyped amount from the {@link
     * org.aventyrs.core.modifier.ModifierType#DAMAGE_ROLL_BONUS} sources (a {@code TemporaryBonus},
     * a Condição). Empty when there is nothing at all to report — never a zero-valued bonus, so a
     * caller can keep treating "no bonus" and "a bonus of 0" alike.
     *
     * <p><b>Bonuses sum; they do not compete.</b> Every other multi-source total in this core is
     * additive (see {@code AbstractSkillInteraction}'s roll-bonus and critical-margin scans), and
     * a character holding two dano-granting traits should get both.
     *
     * <p><b>Mixed types are flattened, deliberately.</b> The result takes the {@link DamageType}
     * (and {@link ElementalType}) of the first typed contributor, and {@link DamageType#FISICO}
     * when only an untyped flatModifier is present — the established reading of an unqualified
     * "+N em rolagens de Danos", the same one {@code
     * AtaqueCorpoACorpoCompetencyAbility#BRUTALIDADE} already takes. Summing a FISICO bonus with
     * an ELEMENTAL one into a single type is a real simplification, and it is the honest one
     * available: nothing downstream models damage carrying two types at once, so the alternative
     * is dropping a bonus a character genuinely has.
     */
    public static java.util.Optional<DamageBonus> total(final java.util.List<DamageBonus> bonuses, final int flatModifier) {
        if (bonuses.isEmpty() && flatModifier == 0) {
            return java.util.Optional.empty();
        }
        int value = bonuses.stream().mapToInt(DamageBonus::getValue).sum() + flatModifier;
        DamageBonus first = bonuses.isEmpty() ? null : bonuses.get(0);
        return java.util.Optional.of(first == null
                ? new DamageBonus(value, DamageType.FISICO)
                : new DamageBonus(value, first.getType(), first.getElementalType()));
    }
}
