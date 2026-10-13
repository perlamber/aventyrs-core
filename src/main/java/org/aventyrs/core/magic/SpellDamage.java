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
 * <p>{@code "Dano Mágico Elemental: Fogo"} maps to {@link DamageType#MAGICO} + the element and a bare {@code "Dano
 * Elemental: Fogo"} to {@link DamageType#ELEMENTAL} + the element (2026-10-12 taxonomy). The pairing is validated
 * here the way {@code org.aventyrs.core.character.DamageDescriptor} validates its own: an Elemental names its element,
 * and a Mágico hit — which must always be qualified — names one too, since a {@code SpellDamage} carries no
 * sanctity; nothing else may.
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
        if (damageType.admitsElement() == (elementalType == null)) {
            throw new IllegalOperationException(INVALID_DAMAGE_TYPE_ELEMENT_PAIRING);
        }
    }

    /**
     * {@code "<dice>d6 + Metade do Foco pontos de Dano Mágico Elemental: <element>"} — the shape most Magias the
     * catalog authors damage for take ({@code diceCount} of 0 is the bare {@code "Metade do Foco pontos de Dano"} case,
     * Sopro de Magma Menor).
     */
    public static SpellDamage halfFocusMagicalElemental(final int diceCount, final ElementalType element) {
        return new SpellDamage(diceCount, 0, FocusScaling.HALF, DamageType.MAGICO, element);
    }

    /** {@code "<dice>d6 + Metade do Foco pontos de Dano Elemental: <element>"} — no "Mágico" (Olhar de Eldur). */
    public static SpellDamage halfFocusElemental(final int diceCount, final ElementalType element) {
        return new SpellDamage(diceCount, 0, FocusScaling.HALF, DamageType.ELEMENTAL, element);
    }

    /**
     * {@code "<dice>d6 + Metade do Foco pontos de Dano"} with no type named — Primordial (table ruling, 2026-10-12:
     * Cura Mortis' Necrofagia).
     */
    public static SpellDamage halfFocusPrimordial(final int diceCount) {
        return new SpellDamage(diceCount, 0, FocusScaling.HALF, DamageType.PRIMORDIAL, null);
    }
}
