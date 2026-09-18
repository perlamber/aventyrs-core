package org.aventyrs.core.sheet;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_ACTION_COST;
import static org.aventyrs.core.util.TranslatableMessages.UNRESOLVED_ACTION_COST;

/**
 * A <b>Tempo de Ativação</b> — what one action costs its taker. The single type for that question
 * across this core: the price a trait <em>declares</em> ({@code ActiveAbility#getActionPointCost()},
 * {@code AventyrTitleAbility#getActionPointCost()}, {@code ChargeService#getActionPointCost},
 * {@code WeaponDrawService#DEFAULT_DRAW_COST}) and the payment an action <em>records</em> (carried
 * on {@link org.aventyrs.core.skill.SkillRoll} as optional roll-metadata and on a {@link
 * CombatantAction} in the per-Rodada action log), so a Talento gated on an attack's cost
 * ({@code AssassinoFeat#SAQUE_RELAMPAGO}: "utilizando apenas 1PA ou Ação Livre") can see it.
 *
 * <p>The five {@link Kind}s and what {@link #actionPoints} means for each:
 *
 * <table border="1">
 *   <caption>Kinds</caption>
 *   <tr><th>Kind</th><th>{@code actionPoints}</th><th>Meaning</th></tr>
 *   <tr><td>{@link Kind#NONE}</td><td>0</td><td>No action at all — a passive.</td></tr>
 *   <tr><td>{@link Kind#FIXED}</td><td>{@code >= 1}</td><td>"Custo de Ativação: NPA".</td></tr>
 *   <tr><td>{@link Kind#FREE_ACTION}</td><td>0</td><td>An Ação Livre.</td></tr>
 *   <tr><td>{@link Kind#REACTION}</td><td>0</td><td>A Reação, taken on another combatant's Turn.</td></tr>
 *   <tr><td>{@link Kind#DYNAMIC}</td><td>{@code >= 1}</td><td>"Variável", from that <em>minimum</em> upward.</td></tr>
 * </table>
 *
 * <p><b>{@link Kind#DYNAMIC} is declaration-only.</b> It states a price whose amount the activating
 * player still chooses; it is not a record of anything spent. {@link #spentActionPoints()} therefore
 * throws on it and {@link CombatantAction} refuses to be built with one — call {@link #resolve(int)}
 * with the chosen amount first. Same shape, and the same reason, as {@code
 * org.aventyrs.core.title.PDCost.Variable} on the PD side, which {@code
 * AbstractTitleAbilityInteraction#resolveDeterminationPoints} resolves the same way.
 *
 * <p>This core does not run a Pontos de Ação economy — nothing spends PA or checks affordability
 * mid-exchange (see {@code ActionPointsService}, which only computes maximums), and nothing counts
 * a Reação or an Ação Livre as spent either. The caller decides what an action cost and states it
 * here; {@link #kind} plus {@link #actionPoints} is exactly that statement, validated at
 * construction because it is a system boundary.
 */
public record ActionCost(Kind kind, int actionPoints) {

    /** Which of the five shapes a cost takes — see this record's own table. */
    public enum Kind { NONE, FIXED, FREE_ACTION, REACTION, DYNAMIC }

    /**
     * No cost at all — a passive, always-on trait with nothing to activate. Distinct from {@link
     * #FREE_ACTION}, which <em>is</em> a player-triggered action that happens to be free.
     */
    public static final ActionCost NONE = new ActionCost(Kind.NONE, 0);

    /** An Ação Livre — spends no Pontos de Ação. */
    public static final ActionCost FREE_ACTION = new ActionCost(Kind.FREE_ACTION, 0);

    /** A Reação — taken on another combatant's Turn, spends no Pontos de Ação. */
    public static final ActionCost REACTION = new ActionCost(Kind.REACTION, 0);

    public ActionCost {
        boolean pointsMatchKind = carriesPoints(kind) == (actionPoints > 0);
        if (kind == null || actionPoints < 0 || !pointsMatchKind) {
            throw new IllegalOperationException(INVALID_ACTION_COST);
        }
    }

    /** An action paid for with actionPoints Pontos de Ação — {@code actionPoints} must be {@code >= 1}. */
    public static ActionCost ofActionPoints(final int actionPoints) {
        return new ActionCost(Kind.FIXED, actionPoints);
    }

    /**
     * A cost of any amount of Pontos de Ação from minimum upward, chosen at activation —
     * {@code minimum} must be {@code >= 1}. Resolve it with {@link #resolve(int)} once the player
     * has picked.
     */
    public static ActionCost dynamic(final int minimum) {
        return new ActionCost(Kind.DYNAMIC, minimum);
    }

    /**
     * The least Pontos de Ação an activation can spend — the whole cost for a {@link Kind#FIXED}
     * one, the floor for a {@link Kind#DYNAMIC} one, and 0 for the three that spend no PA.
     */
    public int minimum() {
        return actionPoints;
    }

    /** Whether spending spent Pontos de Ação is a legal payment of this cost. */
    public boolean accepts(final int spent) {
        return kind == Kind.DYNAMIC ? spent >= actionPoints : spent == actionPoints;
    }

    /** Whether the activating player chooses the amount. */
    public boolean isDynamic() {
        return kind == Kind.DYNAMIC;
    }

    /**
     * This cost as the payment of spent Pontos de Ação — a {@link Kind#DYNAMIC} price becomes the
     * {@link Kind#FIXED} amount actually chosen, and every other kind returns itself. This is what
     * turns a declared price into something {@link CombatantAction} will accept.
     *
     * @throws IllegalOperationException {@code INVALID_ACTION_COST} if spent isn't an amount this
     *         cost {@link #accepts(int)}
     */
    public ActionCost resolve(final int spent) {
        if (!accepts(spent)) {
            throw new IllegalOperationException(INVALID_ACTION_COST);
        }
        return kind == Kind.DYNAMIC ? ofActionPoints(spent) : this;
    }

    /**
     * The Pontos de Ação this action actually spent — {@link #actionPoints} for a {@link
     * Kind#FIXED} action, 0 for a passive, a Reação or an Ação Livre, so all three satisfy a
     * "1PA ou menos" gate.
     *
     * @throws IllegalOperationException {@code UNRESOLVED_ACTION_COST} on a {@link Kind#DYNAMIC}
     *         cost, which is a price and not a payment — {@link #resolve(int)} it first
     */
    public int spentActionPoints() {
        if (kind == Kind.DYNAMIC) {
            throw new IllegalOperationException(UNRESOLVED_ACTION_COST);
        }
        return kind == Kind.FIXED ? actionPoints : 0;
    }

    private static boolean carriesPoints(final Kind kind) {
        return kind == Kind.FIXED || kind == Kind.DYNAMIC;
    }
}
