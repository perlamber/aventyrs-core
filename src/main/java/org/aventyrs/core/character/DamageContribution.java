package org.aventyrs.core.character;

/**
 * One named addend of a dano roll — see {@link DamageBonusBreakdown}, which holds the list of
 * them, and {@link DamageContributionSource} for what each one is.
 *
 * <p>Never zero-valued: a source that contributes nothing is left out entirely, the same
 * "nothing to report is not a report of nothing" contract {@link DamageBonus#total} keeps.
 *
 * <p><b>Untyped.</b> The {@link DamageType} belongs to the summed {@link DamageBonus} (which takes
 * its first typed contributor's type), not to a part: a caller showing the parts is explaining one
 * number, not delivering several kinds of dano.
 */
public record DamageContribution(DamageContributionSource source, int value) {
}
