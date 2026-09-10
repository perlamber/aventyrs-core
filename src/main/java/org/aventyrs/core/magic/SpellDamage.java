package org.aventyrs.core.magic;

import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.sheet.IllegalOperationException;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_DAMAGE_TYPE_ELEMENT_PAIRING;
import static org.aventyrs.core.util.TranslatableMessages.INVALID_SPELL_DAMAGE;

/**
 * The numeric damage a Magia's {@code Efeito:} line deals — the structured form of a phrase like
 * {@code "Causa 2d6+Metade do Foco pontos de Dano Mágico Elemental: Magma"}.
 *
 * <p>Split into the two halves this core treats differently:
 *
 * <ul>
 *   <li>{@code diceCount} d6 the <b>caller</b> rolls and adds — this core never rolls dice, the
 *       same boundary {@code org.aventyrs.core.character.DamageBase} and {@code SkillRoll} keep;</li>
 *   <li>{@code flatBonus} + a Foco term ({@link #focusScaling}) — the <b>deterministic</b> part,
 *       which {@code SpellCastingService#resolvePrimaryDamage} computes from the caster.</li>
 * </ul>
 *
 * <p>{@code "Dano Mágico Elemental: Fogo"} and a bare {@code "Dano Elemental: Fogo"} both map to
 * {@link DamageType#ELEMENTAL} + the element — this core's {@code DamageType} has no {@code
 * MAGICO_ELEMENTAL} constant, and {@code ELEMENTAL} already reads as the magical-elemental case
 * ({@code FISICO_ELEMENTAL} being the physical one). The {@code elementalType} is required exactly
 * when {@code damageType} is {@link DamageType#ELEMENTAL}/{@link DamageType#FISICO_ELEMENTAL},
 * validated here the same way {@code org.aventyrs.core.character.DamageBonus} validates its own.
 *
 * <p>What this deliberately does <b>not</b> hold: distance falloff ("-2 para cada UD percorrido"),
 * a recurring "a cada Rodada" cadence, a delayed second wave, or damage dealt to someone other
 * than the cast target — all still prose on the constant, each blocked on positioning / effect
 * scheduling this core does not have.
 */
public record SpellDamage(int diceCount, int flatBonus, FocusScaling focusScaling,
                          DamageType damageType, ElementalType elementalType) {

    public SpellDamage {
        if (diceCount < 0 || focusScaling == null || damageType == null) {
            throw new IllegalOperationException(INVALID_SPELL_DAMAGE);
        }
        boolean isElemental = damageType == DamageType.ELEMENTAL || damageType == DamageType.FISICO_ELEMENTAL;
        if (isElemental == (elementalType == null)) {
            throw new IllegalOperationException(INVALID_DAMAGE_TYPE_ELEMENT_PAIRING);
        }
    }

    /**
     * {@code "<dice>d6 + Metade do Foco pontos de Dano Mágico Elemental: <element>"} — the shape
     * every Magia the catalog currently authors damage for takes ({@code diceCount} of 0 is the
     * bare {@code "Metade do Foco pontos de Dano"} case, Sopro de Magma Menor).
     */
    public static SpellDamage halfFocusElemental(final int diceCount, final ElementalType element) {
        return new SpellDamage(diceCount, 0, FocusScaling.HALF, DamageType.ELEMENTAL, element);
    }

    /** {@code "<dice>d6 + Metade do Foco pontos de Dano Mágico"} — no element. */
    public static SpellDamage halfFocusMagical(final int diceCount) {
        return new SpellDamage(diceCount, 0, FocusScaling.HALF, DamageType.MAGICO, null);
    }
}
