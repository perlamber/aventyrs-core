package org.aventyrs.core.sheet;

import lombok.Getter;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.modifier.ModifierType;

/**
 * A bonus/malus a {@link CharacterSheet} is temporarily holding — granted by some other
 * Character's action (e.g. {@code ArtesCompetencyAbility#DOM_BARDICO} motivating an ally for
 * a few Rodadas), as opposed to a permanent ability of the CharacterSheet's own
 * {@link Character}. Its {@code type} reuses the existing {@link ModifierType} taxonomy —
 * this is deliberately generic, not scoped to Perícia-roll bonuses specifically, since
 * nothing about "a temporary effect from another Character" is inherently about rolls.
 *
 * <p>Counts down in Rodadas rather than storing an absolute expiry Round number, matching how
 * abilities like DOM_BARDICO describe their own duration ("por 1 Rodada", "por 2 Rodadas") —
 * see {@link TemporaryEffect}, its shared superclass with {@link Bleeding}, and {@link
 * CharacterSheet#tickTemporaryEffects()} for how the countdown advances.
 */
@Getter
public class TemporaryBonus extends TemporaryEffect {
    private final ModifierType type;
    private final int value;

    public TemporaryBonus(final ModifierType type, final int value, final int remainingRounds) {
        super(remainingRounds);
        this.type = type;
        this.value = value;
    }
}
