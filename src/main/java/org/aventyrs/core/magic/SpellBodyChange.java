package org.aventyrs.core.magic;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_SPELL_BODY_CHANGE;

import java.util.Set;

import lombok.Builder;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.sheet.IllegalOperationException;

/**
 * What a Polimorfismo Magia does to its target's body, as data (core 0.0.99) — {@link Spell#getBodyChange()}: a
 * Categoria de Tamanho shift, a change to Força and Destreza, and a Multiplicador de PV increase, all lasting the
 * Magia's own Duração. Applied by {@code effect.BodyChangeEffect}, which grants each part as a round-scoped bonus.
 *
 * <p>Every attribute clause in the tree names Força and Destreza and nothing else, so those are the only two this
 * moves — "Força <b>e</b> Destreza" both, or "Força <b>ou</b> Destreza" ({@link #attributeChoice}) the one the caster
 * picks at the cast.
 *
 * @param sizeCategoryShift      steps of Categoria de Tamanho, signed — Titânecer's +2, Toque de Nanicolina's −2
 * @param attributeChange        added to Força and Destreza (or the chosen one), signed
 * @param attributeChoice        "Força ou Destreza": only the Atributo the caster picks moves
 * @param attributesSetTo        Enfadecer's "Força e Destreza reduzidas à 1" — a target value rather than a change,
 *                               resolved against the target's total when the effect lands; {@code null} otherwise
 * @param lifeMultiplierIncrease added to the Multiplicador de PV — Titânecer's and Dracônecer's +2
 */
@Builder
public record SpellBodyChange(int sizeCategoryShift, int attributeChange, boolean attributeChoice,
                              Integer attributesSetTo, int lifeMultiplierIncrease) {

    /**
     * The Atributo floor a harmful change respects — "este efeito não reduz Atributos à um total de zero ou menos"
     * (Murcha-Corpo, Serra-Pernas). ⚠️ Applied to every harmful change in the tree, including the two that do not
     * repeat the clause (Toque de Nanicolina's −3, and Enfadecer, whose "reduzidas à 1" lands on it anyway).
     */
    public static final int MINIMUM_ATTRIBUTE = 1;

    /** The two Atributos every Polimorfismo clause moves. */
    public static final Set<AttributeDomain> PHYSICAL_ATTRIBUTES = Set.of(AttributeDomain.STRENGTH,
            AttributeDomain.DEXTERITY);

    public SpellBodyChange {
        boolean anything = sizeCategoryShift != 0 || attributeChange != 0 || attributesSetTo != null
                || lifeMultiplierIncrease != 0;
        boolean setAndChange = attributesSetTo != null && (attributeChange != 0 || attributeChoice);
        boolean choiceOfNothing = attributeChoice && attributeChange == 0;
        if (!anything || setAndChange || choiceOfNothing || !sameDirection(sizeCategoryShift, attributeChange,
                attributesSetTo == null ? 0 : -1, lifeMultiplierIncrease)) {
            throw new IllegalOperationException(INVALID_SPELL_BODY_CHANGE);
        }
    }

    /**
     * Whether this shrinks or weakens its target rather than strengthening them — what files its effect as an {@code
     * OffensiveEffect} instead of a {@code DefensiveEffect}. Every part points the same way (the constructor refuses a
     * mix), so any one of them answers.
     */
    public boolean isHarmful() {
        return sizeCategoryShift < 0 || attributeChange < 0 || attributesSetTo != null || lifeMultiplierIncrease < 0;
    }

    /** The Atributos this moves, the caster having picked chosen (ignored unless {@link #attributeChoice}). */
    public Set<AttributeDomain> affectedAttributes(final AttributeDomain chosen) {
        if (attributeChange == 0 && attributesSetTo == null) {
            return Set.of();
        }
        if (!attributeChoice) {
            return PHYSICAL_ATTRIBUTES;
        }
        return chosen != null && PHYSICAL_ATTRIBUTES.contains(chosen) ? Set.of(chosen) : Set.of();
    }

    private static boolean sameDirection(final int... parts) {
        boolean up = false;
        boolean down = false;
        for (int part : parts) {
            up |= part > 0;
            down |= part < 0;
        }
        return !(up && down);
    }
}
