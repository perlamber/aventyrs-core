package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.IllegalOperationException;

/**
 * Sacar e guardar armas — taking a carried weapon in hand, and putting it away again.
 *
 * <p>Drawing is an <b>action</b>, not a bookkeeping detail: it costs Pontos de Ação, a Talento
 * can make it free, and a Condição can forbid it outright. That is why it has a service rather
 * than living only as the plain {@code Character#drawWeapon(Weapon)} mutator — the same
 * check-then-mutate split {@link ActiveAbilityService#activate} draws, and the same reason
 * {@code FeatService#grantFeat} exists beside {@code Character#grantFeat}. The mutators stay
 * unvalidating and builder-bypassable by design; this is the entry point that enforces the rules.
 *
 * <p><b>What "drawn" means</b> is {@code Character#getDrawnWeapons()} — the in-hand subset of
 * {@code getEquipment()}. A weapon must be carried before it can be drawn, and "utilizando uma
 * arma" throughout this core means drawn, never merely carried.
 */
public interface WeaponDrawService {

    /**
     * What drawing a weapon costs by default — <b>1 Ponto de Ação</b>.
     *
     * <p><b>Inferred, not authored.</b> No entry in {@code docs/rules/} states the cost of sacar.
     * It is taken as 1PA because {@code AssassinoFeat#SAQUE_RAPIDO}'s entire benefit is "você
     * pode sacar uma arma como Ação Livre" — a Talento worth acquiring only if the default is
     * <i>not</i> free, and 1PA is the smallest cost that makes it so. Change this one constant if
     * the rules say otherwise.
     */
    ActionCost DEFAULT_DRAW_COST = ActionCost.ofActionPoints(1);

    /**
     * What drawing would cost character right now — {@link #DEFAULT_DRAW_COST}, or {@link
     * ActionCost#FREE_ACTION} if any held Talento says otherwise ({@code
     * Feat#drawsWeaponAsFreeAction}).
     *
     * <p>A pure question: asking never draws anything, so a UI can price the action before
     * offering it.
     */
    ActionCost getDrawCost(Character character);

    /**
     * Whether sheet may draw weapon right now — carried, not already in hand, and not held back
     * by a Condição ({@link ConditionType#preventsArming()} — Devorado). The non-throwing form of
     * {@link #draw}, for a caller deciding what to offer rather than performing it.
     */
    boolean canDraw(CombatantSheet sheet, Weapon weapon);

    /**
     * Takes weapon in hand on sheet's behalf, returning what the action cost.
     *
     * <p>Validates, in order: that the character is carrying weapon ({@code WEAPON_NOT_CARRIED});
     * that it is not already drawn ({@code WEAPON_ALREADY_DRAWN}); and that no held Condição
     * forbids arming at all ({@code WEAPON_DRAW_PREVENTED} — Devorado). Throws {@link
     * IllegalOperationException} on any failure, leaving the sheet untouched.
     *
     * <p>On success the weapon enters {@code Character#getDrawnWeapons()} and the draw is marked
     * on the Turn ({@code CombatantSheet#hasDrawnWeaponThisTurn()}), which is what {@code
     * AssassinoFeat#SAQUE_RAPIDO}'s Desvantagem is measured against.
     *
     * <p><b>The Pontos de Ação are reported, not deducted.</b> This core keeps no "PA already
     * spent this Turn" pool — {@code ActionPointsService} answers a Turn's maximum, not a running
     * balance (see {@code ActiveAbilityService#activate}'s own note on the same limitation) — so
     * affordability is the caller's to enforce against the returned cost.
     *
     * @return the {@link ActionCost} the draw actually cost, per {@link #getDrawCost}
     */
    ActionCost draw(CombatantSheet sheet, Weapon weapon) throws IllegalOperationException;

    /**
     * Puts weapon away, leaving it carried — "guardar sua arma". Returns whether it was in hand
     * to begin with; a weapon that was not drawn is a no-op rather than an error, since nothing
     * about the rules makes putting away an empty hand a failure.
     *
     * <p>Deliberately unpriced and ungated: no authored clause charges Pontos de Ação for
     * sheathing, and {@code AssassinoFeat#TROCA_DE_ARMA_VELOZ}'s "como uma Reação" describes what
     * a Talento <i>lets</i> you do off-Turn rather than what sheathing normally costs.
     */
    boolean sheathe(CombatantSheet sheet, Weapon weapon);
}
