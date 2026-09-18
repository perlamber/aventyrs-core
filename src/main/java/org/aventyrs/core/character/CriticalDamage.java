package org.aventyrs.core.character;

/**
 * What an Acerto Crítico adds to the dano roll it is the attack half of — {@code extraDice} further
 * d6 to roll, plus a flat {@code flatBonus} on top.
 *
 * <h2>A third stage, not a bigger {@link DamageBase} and not a {@link DamageBonus}</h2>
 *
 * A {@link DamageBase} scale-up is a <em>qualitative</em> step along one authored table, and a
 * {@link DamageBonus} is a flat number added to an already-rolled total. A crit is neither: it is
 * conditional on how the <em>attack</em> roll came out, so it cannot be folded into a Dano Base
 * resolved before any dice were thrown, and it can grant a die, which a {@code DamageBonus} has no
 * room for. It is therefore reported separately — {@code InteractionResult#getCriticalDamage()} is
 * {@code null} on every roll that wasn't a critical success — and the flat half is additionally
 * named in the {@link DamageBonusBreakdown} under {@link DamageContributionSource#CRITICAL}, so a
 * reader of the dano line sees where the number came from.
 *
 * <p><b>The baseline is a flat +2 and no die</b> — a crit grants Vantagem em Danos, worth the same
 * {@code Skill#ADVANTAGE_BONUS} it is worth everywhere else. The "+1d6"s belong to specific traits:
 * {@code AssassinoFeat#VIOLENCIA_DESCOMUNAL} <em>replaces</em> the Vantagem with one ("Você não
 * recebe Vantagem em Danos em seus Acertos Críticos, ao invés disso recebe Bônus de +1d6" — the
 * clause that proves what the baseline is), while {@code ArtilhariaFeat#MIRA_MORTAL} and the
 * weapon's Aprimoramentos <em>add</em> to it.
 *
 * <p>This core never rolls dice (same boundary as {@code org.aventyrs.core.skill.SkillRoll}), so
 * {@link #extraDice()} is how many further d6 the caller is expected to roll — each a {@link
 * DamageBase#DICE_SIDES}-sided die, the only die this ruleset deals damage with — and they are
 * <b>not</b> subject to {@link DamageBase#MAX_DICE}: that cap is a property of the Dano Base scale,
 * which this is not part of.
 */
public record CriticalDamage(int extraDice, int flatBonus) {

    /** No critical contribution at all — what every hook that grants none returns. */
    public static final CriticalDamage NONE = new CriticalDamage(0, 0);

    /** Both halves of two contributions added together, for folding a scan into one answer. */
    public CriticalDamage plus(final CriticalDamage other) {
        return other == null ? this
                : new CriticalDamage(extraDice + other.extraDice(), flatBonus + other.flatBonus());
    }

    /** Only {@link #flatBonus}, for a contribution that grants no die. */
    public static CriticalDamage ofFlat(final int flatBonus) {
        return new CriticalDamage(0, flatBonus);
    }

    /** Only {@link #extraDice}, for a contribution that grants dice and no flat bonus. */
    public static CriticalDamage ofDice(final int extraDice) {
        return new CriticalDamage(extraDice, 0);
    }

    /** Whether this grants nothing whatsoever — what {@code AbstractSkillInteraction} reports as a
     * {@code null} {@code criticalDamage} rather than as a pair of zeroes. */
    public boolean isNone() {
        return extraDice == 0 && flatBonus == 0;
    }
}
