package org.aventyrs.core.sheet;

import lombok.Getter;
import org.aventyrs.core.character.EgoDomain;

/**
 * A temporary reduction of how many temporary Ego points a {@link CombatantSheet} may hold in
 * one {@link EgoDomain} — registered via {@link CombatantSheet#applyEffect(TemporaryEffect)}
 * and counted down in Rodadas by its shared superclass {@link TemporaryEffect}, exactly like
 * {@link Bleeding}/{@link ManaDrain}/{@link Withering}. It has no per-Rodada side effect of its
 * own: its whole behaviour is being present, so it inherits {@link
 * TemporaryEffect#applyRoundEffect}'s no-op the same way {@link TemporaryBonus} does.
 *
 * <p><strong>It lowers the temporary ceiling; it is never a spend.</strong> That distinction is
 * the entire reason this is its own type rather than a drain through {@link
 * CombatantSheet#spendEgoPoints}. A spend is consumption — irreversible except through a
 * specific promise ({@link PendingEgoRecovery}) or a per-session recovery — so modelling a
 * penalty as one would silently outlive the penalty's own Duração. A ceiling term instead
 * reverses the moment this effect expires, because {@link EgoPointPool} only ever <em>reads</em>
 * the ceiling and recomputes what remains under it (see that class's own javadoc). It never
 * touches permanent points: nothing in this ruleset reduces those automatically — they are only
 * spent by their holder's own choice.
 *
 * <p><strong>Why not a negative {@link TemporaryBonus} with an {@code EgoDomain}-keyed {@link
 * org.aventyrs.core.modifier.ModifierType}</strong> — which would otherwise be the obvious
 * reuse, since {@link TemporaryBonus} already documents itself as carrying a malus. Two reasons:
 * adding {@code AUTOCONTROLE}/{@code RECURSOS}/{@code SORTE} constants would immediately expose
 * them to {@code ModifierResolver}'s three-source {@code @Modifier} scan, implying an ability
 * could {@code @Modifier}-grant spendable Ego points — a different mechanism nothing asked for;
 * and {@link org.aventyrs.core.modifier.ModifierType#INITIATIVE} already exists meaning
 * <em>turn order</em>, so an {@code EgoDomain}-to-{@code ModifierType} mapping would collide
 * semantically on one of the four domains with no way out. Keeping the two taxonomies apart is
 * the same restraint that keeps {@link LifeSteal} off {@code ModifierType}.
 *
 * <p>Consequently, if a future clause reduces the <em>Iniciativa stat</em> (the turn-order value
 * {@code InitiativeService}/{@code InitiativeEntry} compute), that is a {@code
 * TemporaryBonus(INITIATIVE, -n, rounds)} and <em>not</em> this. Both exist; they are not
 * alternatives to each other.
 *
 * <p>{@link #isCumulative()} is left at its inherited {@code true}: no rules text says an Ego
 * penalty is "não cumulativo", so two landing at once stack, the same default {@link Bleeding}
 * and {@link ManaDrain} keep.
 */
@Getter
public class TemporaryEgoPenalty extends TemporaryEffect {
    private final EgoDomain domain;
    private final int value;

    public TemporaryEgoPenalty(final EgoDomain domain, final int value, final Integer remainingRounds) {
        super(remainingRounds);
        this.domain = domain;
        this.value = value;
    }
}
