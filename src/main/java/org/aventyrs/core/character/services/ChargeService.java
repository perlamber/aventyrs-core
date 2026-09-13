package org.aventyrs.core.character.services;

import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.TemporaryBonus;
import org.aventyrs.core.skill.Skill;

/**
 * The Investida — a movement and an Ataque Corpo-a-Corpo bought as one action.
 *
 * <p>This service prices it, gates it, says how far it carries the charger and applies what a miss
 * costs them. It deliberately does <b>not</b> attack: {@link #begin} returns a {@link ChargeResult}
 * and the caller then runs an ordinary {@code org.aventyrs.core.combat.DeliveredAttack} through
 * {@code AttackDelivery}, with a {@code SkillRoll} carrying {@link Manoeuvre#INVESTIDA} and the
 * {@code ActionCost} this service resolved. That split is the design: an Investida picks up every
 * Talento, Condição, Forma and Efeito Crítico clause the normal attack path already resolves,
 * instead of growing a second pipeline that would have to re-earn all of them.
 *
 * <p>The shape is {@code WeaponDrawService}'s — a pure cost question, a non-throwing {@link
 * #canCharge} predicate, a throwing mutator, and one private refusal helper behind both so the
 * predicate and the mutator can never disagree.
 *
 * <h2>Where the numbers come from</h2>
 *
 * <b>No document under {@code docs/rules/} defines an Investida.</b> The nearest thing to a
 * definition is {@code docs/rules/magias.txt}'s Bote Inesperado — "esta é uma ação de investida
 * regular e recebe todos os seus benefícios e devem seguir os critérios padrões" — which defers to
 * criteria nothing states. So each constant below is labelled with its actual provenance, the same
 * honesty {@code WeaponDrawService#DEFAULT_DRAW_COST} applies to its own inferred 1PA.
 *
 * <h2>What an Investida is worth, and where each half lands</h2>
 *
 * <ul>
 *   <li><b>+2 to the dano roll on a hit</b> — not here. It is a property of the manoeuvre rather
 *   than of anything the charger holds, so it hangs off no {@code resolve*} hook at all and is
 *   summed into {@code DamageBonus} by {@code AbstractSkillInteraction}, beside the melee half-Força
 *   term which is there for exactly the same reason. It needs no "did it hit" test: a {@code
 *   DamageBonus} only ever reaches a dano roll, and {@code AttackDelivery} only builds one on a hit.</li>
 *   <li><b>-2 to Defesas on a miss</b> — {@link #applyOutcome}, called by the caller after it has
 *   resolved the attack. {@code AttackDelivery#resolve} is report-only and stays so; this mirrors
 *   {@code DefeatBlessingService#applyDefeatBlessings}, the other caller-driven aftermath.</li>
 *   <li><b>The Reações it provokes</b> — {@link MovementReactionService}, because that rule is
 *   general to movement and an Investida is merely the first manoeuvre that triggers it.</li>
 * </ul>
 */
public interface ChargeService {

    /**
     * The Pontos de Ação an Investida costs before any Talento shortens it.
     *
     * <p><b>An inference.</b> No rules text prices an Investida; 3PA is the figure this core was
     * given. For scale, an ordinary Perícia roll is {@code
     * org.aventyrs.core.action.ActionPointsService#DEFAULT_SKILL_ROLL_COST} (2) — so an Investida
     * costs one point more than the plain attack it replaces, and buys a movement with it.
     */
    int BASE_ACTION_POINT_COST = 3;

    /**
     * How many times its Movimento Base an Investida carries the charger.
     *
     * <p><b>Read off the catalog</b>, not invented: {@code DexterityAbility#IMPLACAVEL} grants
     * "percorrer até o <i>triplo</i> do seu Movimento Base, <i>ao invés do dobro</i>" — a clause
     * that only means anything if the baseline is double.
     */
    int BASE_MOVEMENT_MULTIPLIER = 2;

    /** The floor on a shortened Investida — {@code ActionCost} refuses a Pontos de Ação cost of 0. */
    int MINIMUM_ACTION_POINT_COST = 1;

    /**
     * What a landed Investida adds to its dano roll. Numerically {@code Skill#ADVANTAGE_BONUS},
     * and referenced through it deliberately: a flat +2 on a dano roll is precisely what Vantagem
     * is worth everywhere else in this ruleset.
     */
    int HIT_DAMAGE_BONUS = Skill.ADVANTAGE_BONUS;

    /**
     * What a missed Investida costs its charger's Defesas. {@code
     * DexterityAbility#IMPLACAVEL}'s "você não recebe o redutor <i>padrão</i> de -2 em suas
     * Defesas" is what identifies this as the manoeuvre's own baseline rather than that
     * Habilidade's invention.
     */
    int MISS_DEFENSE_MALUS = Skill.DISADVANTAGE_MALUS;

    /** How long that Redutor lasts. */
    int MISS_DEFENSE_MALUS_ROUNDS = 1;

    /**
     * The {@code Blessing} source the miss penalty is granted under. Naming a source is what makes
     * it <b>replace rather than stack</b>: {@code TemporaryBonus#stackingKey()} is (source, type),
     * so a second failed Investida in the same window renews the Redutor instead of deepening it
     * to -4. The rules say nothing about two failed charges, and renewal is the conservative read.
     */
    String MISS_PENALTY_SOURCE = "INVESTIDA";

    /**
     * What an Investida costs this character, already reduced by every held Talento ({@code
     * Feat#resolveChargeActionPointReduction} — {@code MobilidadeFeat#INVESTIDA_AQUATICA}) and
     * floored at {@link #MINIMUM_ACTION_POINT_COST}.
     *
     * <p><b>Reported, not deducted.</b> This core keeps no spent-this-Turn pool — the same
     * standing caveat {@code WeaponDrawService#draw} carries.
     */
    org.aventyrs.core.sheet.ActionCost getActionPointCost(Character character);

    /**
     * How far this combatant's Investida may carry them, in UD: {@link #BASE_MOVEMENT_MULTIPLIER}
     * plus every held {@code AttributeAbility#resolveChargeMovementMultiplierIncrease}, times their
     * {@code MovementService#getMovementBase(CombatantSheet)}.
     *
     * <p><b>A total distance, not a per-Ponto-de-Ação figure</b> — the single exception to {@link
     * MovementService}'s governing rule, and the reason it is computed here rather than there. An
     * Investida is one fixed-cost action, so there is no "how many points do I spend moving" choice
     * left for the caller to multiply by. 0 while a Condição forbids moving, since the Movimento
     * Base it scales is 0 then.
     *
     * <p>Reads the sheet's movement counter without claiming a position; {@link #begin} is what
     * calls {@code CombatantSheet#consumeMovementThisRound()}.
     */
    int getMovementAllowance(CombatantSheet sheet);

    /**
     * Redução de Dano this character's Talentos grant them for the charge's movement — see {@code
     * Feat#resolveChargeMovementDamageReduction}. Reported on {@link ChargeResult}, never granted;
     * that method's javadoc has the reason.
     */
    int getMovementDamageReduction(Character character);

    /**
     * Whether this combatant may declare an Investida with weapon right now. A {@code null} weapon
     * is an Ataque Desarmado and is a legitimate charge.
     *
     * <p>Never throws — {@link #begin} is the form that does, and both read the same refusals.
     */
    boolean canCharge(CombatantSheet sheet, Weapon weapon);

    /**
     * Declares the Investida: claims the movement on sheet and reports everything resolved about
     * it. <b>Does not attack</b> — see this interface's own javadoc for what the caller does next.
     *
     * <p>sceneContext is the charger's own snapshot, and only {@link ChargeResult#provokedReactors}
     * depends on it; {@code null} is legitimate and yields an empty reactor list.
     *
     * @throws IllegalOperationException if the charger cannot presently charge with this weapon —
     *         it is carried but sheathed ({@code WEAPON_NOT_DRAWN}), not theirs at all
     *         ({@code WEAPON_NOT_CARRIED}), not swung as an Ataque Corpo-a-Corpo
     *         ({@code CHARGE_REQUIRES_MELEE_WEAPON}), unusable in their current Forma
     *         ({@code CANNOT_ATTACK_WITH_WEAPON}), or they are forbidden to move
     *         ({@code CHARGE_MOVEMENT_PREVENTED})
     */
    ChargeResult begin(CombatantSheet sheet, Weapon weapon, SceneContext sceneContext);

    /**
     * Settles what the Investida's outcome costs its charger, after the caller has resolved the
     * attack. A hit costs nothing — its +2 already rode the dano roll. A miss grants the charger
     * {@link #MISS_DEFENSE_MALUS} to their Defesas for {@link #MISS_DEFENSE_MALUS_ROUNDS} Rodada,
     * unless a held {@code AttributeAbility#waivesChargeMissDefensePenalty} spares them.
     *
     * <p>Caller-driven rather than fired from the attack path, because {@code
     * AttackDelivery#resolve} applies nothing at all and is not going to start — the same shape
     * {@code DefeatBlessingService} takes.
     *
     * @return the granted {@code TemporaryBonus}, or {@code null} on a hit or a waived penalty
     */
    TemporaryBonus applyOutcome(CombatantSheet sheet, boolean hit);
}
