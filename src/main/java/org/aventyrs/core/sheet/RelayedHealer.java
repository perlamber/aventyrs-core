package org.aventyrs.core.sheet;

/**
 * The healer's half of a heal, resolved where the healer's real sheet lives and carried to a target
 * whose real sheet lives somewhere else — another player's client, which holds only a stand-in for
 * the healer and so could never ask the healer's Títulos itself.
 *
 * <p>Everything here is a fact about the <b>healer</b>; everything the limits also need about the
 * <b>target</b> (is it in a Coma begun this Cena, how long has it been dead) is still judged by
 * {@link CombatantSheet#heal(int, HealingSource)} on the target's own sheet. That split is why the
 * Coma bypass is carried as "would bypass, if the Coma began this Cena" rather than as a verdict.
 *
 * @param healingBonus               PV the healer's Títulos add ({@code AventyrTitle#resolveHealingBonus})
 * @param bypassesComaCapInThisScene whether the healer's Títulos lift the Coma cap for a Coma begun in
 *                                   the current Cena ({@code AventyrTitle#grantsComaHealingBypass})
 * @param revivalWindowRounds        the Rodadas-since-death a revival permission the healer already
 *                                   paid for reaches ({@code AventyrTitle#claimRevivalWindow}), or
 *                                   {@code null} when the heal may not reach the dead
 */
public record RelayedHealer(int healingBonus, boolean bypassesComaCapInThisScene, Integer revivalWindowRounds) {

    /** A healer whose Títulos change nothing. */
    public static final RelayedHealer NONE = new RelayedHealer(0, false, null);
}
