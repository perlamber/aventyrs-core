package org.aventyrs.core.magic;

import org.aventyrs.core.character.Character;

import java.util.OptionalInt;

/**
 * Resolves a Magia's authored Duração into Rodadas for a particular cast.
 *
 * <p>The catalog's {@link SpellDuration} remains immutable authored data: this service resolves
 * target-Attribute and referenced durations, then applies the caster's equipped item Improvements.
 * {@link DurationKind#UNTIL_END_OF_TURN} has no numeric duration and therefore remains unresolved.
 */
public interface SpellDurationService {

    /**
     * Resolves a duration that does not require a target Attribute. A target-Attribute duration
     * returns {@link OptionalInt#empty()}.
     */
    default OptionalInt resolveDurationInRounds(final Spell spell, final Character caster) {
        return resolveDurationInRounds(spell, caster, null);
    }

    /**
     * Resolves the duration for this cast. Target is required only when the authored duration
     * scales from one of the target's Attributes.
     */
    OptionalInt resolveDurationInRounds(Spell spell, Character caster, Character target);
}
