package org.aventyrs.core.magic;

import org.aventyrs.core.character.Character;

import lombok.NonNull;

import java.util.OptionalInt;

/** Default {@link SpellDurationService} implementation. */
public class SpellDurationServiceImpl implements SpellDurationService {

    @Override
    public OptionalInt resolveDurationInRounds(@NonNull final Spell spell, @NonNull final Character caster,
                                               final Character target) {
        OptionalInt durationInRounds = resolveAuthoredDurationInRounds(spell.getDuration(), target);
        if (durationInRounds.isEmpty()) {
            return OptionalInt.empty();
        }
        int improvementIncrease = isExtendable(spell.getDuration())
                ? sumImprovementDurationIncreases(spell, caster)
                : 0;
        return OptionalInt.of(durationInRounds.getAsInt() + improvementIncrease);
    }

    private OptionalInt resolveAuthoredDurationInRounds(final SpellDuration duration, final Character target) {
        return switch (duration.kind()) {
            case INSTANTANEA -> OptionalInt.of(0);
            case FIXED -> OptionalInt.of(duration.count() * duration.unit().getRodadas());
            case TARGET_ATTRIBUTE -> target == null
                    ? OptionalInt.empty()
                    : OptionalInt.of(target.getAttributes().getAttribute(duration.scalingAttribute()).getTotal()
                            * duration.unit().getRodadas());
            case UNTIL_END_OF_TURN -> OptionalInt.empty();
            case SAME_AS_REFERENCED -> resolveAuthoredDurationInRounds(duration.reference().get(), target);
        };
    }

    private int sumImprovementDurationIncreases(final Spell spell, final Character caster) {
        return caster.getEquipment().stream()
                .mapToInt(item -> item.resolveEnhancementDurationIncreaseInRounds(spell, caster))
                .sum();
    }

    private boolean isExtendable(final SpellDuration duration) {
        return switch (duration.kind()) {
            case FIXED, TARGET_ATTRIBUTE -> true;
            case SAME_AS_REFERENCED -> isExtendable(duration.reference().get());
            case INSTANTANEA, UNTIL_END_OF_TURN -> false;
        };
    }
}
