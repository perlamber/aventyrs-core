package org.aventyrs.core.character.services;

import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;

import java.util.List;

/**
 * Everything {@link ChargeService#begin} resolved about one declared Investida — its price, how far
 * it may carry the charger, what it exposes them to on the way, and what mitigation they carry
 * while it runs.
 *
 * <p><b>It contains no attack.</b> {@code begin} claims the movement and answers these four
 * questions; the attack is the caller's next step, made by running an ordinary {@code
 * org.aventyrs.core.combat.DeliveredAttack} through {@code AttackDelivery} with a {@code SkillRoll}
 * carrying {@link org.aventyrs.core.action.Manoeuvre#INVESTIDA} and {@link #actionCost}. Keeping
 * the two apart is what lets an Investida pick up every Talento, Condição and Forma clause the
 * normal attack path already resolves, instead of growing a parallel one.
 */
public record ChargeResult(

        /**
         * What this Investida cost its charger, already reduced by every Talento that shortens it.
         * <b>Reported, not deducted</b> — this core runs no Pontos de Ação economy (see {@code
         * ActionCost}), so affordability is the caller's to enforce. Stamp it on the attack's
         * {@code SkillRoll} so the per-Rodada action log records what the exchange actually cost.
         */
        ActionCost actionCost,

        /**
         * How far the charger may travel, in UD — <b>a total distance for the whole manoeuvre</b>,
         * and the one deliberate exception to {@link MovementService}'s rule that every movement
         * figure in this ruleset is per Ponto de Ação. An Investida is a fixed-cost bundled action:
         * its allowance is Movimento Base times a multiplier, and there is no per-point choice left
         * for a caller to multiply by. Do not add it to, or compare it against, a Movimento Base.
         */
        int movementAllowanceInUnidadesDeDistancia,

        /**
         * Redução de Dano the charger carries "durante o movimento da investida" ({@code
         * MobilidadeFeat#INVESTIDA_SELVAGEM}), summed across their Talentos.
         *
         * <p><b>Unapplied, and named so.</b> The window is shorter than a Rodada, which is the
         * shortest a {@code TemporaryBonus} can last, so granting one would go on protecting the
         * charger after the charge had landed. Exact data whose application is blocked, exactly as
         * {@code AttackDelivery} reports an attacker's {@code unappliedDifficultyReduction}. 0 when
         * no Talento grants any.
         */
        int unappliedMovementDamageReduction,

        /**
         * The enemies entitled to a Reação against this movement — see {@link
         * MovementReactionService}, which resolves it. Empty when the charger is exempt ({@code
         * DexterityAbility#IMPLACAVEL}), when no enemy has a drawn melee weapon in reach, or when
         * no {@code SceneContext} was supplied.
         *
         * <p><b>Opportunities, not Reações.</b> Nothing in this core fires one or counts one as
         * spent; the caller adjudicates.
         */
        List<CombatantSheet> provokedReactors) {
}
