package org.aventyrs.core.character;

import java.util.List;

/**
 * The named parts of the one {@link DamageBonus} a Perícia de Ataque roll grants — what it is
 * made of, for a caller that has to explain the number rather than merely add it.
 *
 * <p><b>The invariant:</b> {@link #total()} equals the accompanying {@code DamageBonus#getValue()}.
 * {@code AbstractSkillInteraction} builds the two together and {@code InteractionResult} carries
 * them together, so a reader can always check one against the other; a part that reached the
 * bonus but not this list would be a bug in that method, not a shape this type permits.
 *
 * <p>Contributions are listed in the order they are summed (the typed sources first, then the flat
 * ones), zero-valued ones omitted — so an empty breakdown means nothing contributed at all, and
 * accompanies no {@code DamageBonus}.
 */
public record DamageBonusBreakdown(List<DamageContribution> contributions) {

    public DamageBonusBreakdown {
        contributions = contributions == null ? List.of() : List.copyOf(contributions);
    }

    /** The empty breakdown — what a roll with no dano bonus at all reports. */
    public static DamageBonusBreakdown empty() {
        return new DamageBonusBreakdown(List.of());
    }

    /** The summed value of every contribution — equal to the {@code DamageBonus} this explains. */
    public int total() {
        return contributions.stream().mapToInt(DamageContribution::value).sum();
    }

    /** What source contributed, summed — {@code 0} when it contributed nothing. */
    public int valueOf(final DamageContributionSource source) {
        return contributions.stream()
                .filter(contribution -> contribution.source() == source)
                .mapToInt(DamageContribution::value)
                .sum();
    }
}
